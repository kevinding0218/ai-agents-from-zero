"""
【案例】LangChain + Ollama 向量记忆多轮对话（真实世界工作流）

【问题回顾】
  普通 MultiTurn 方案（见 LangChain_Ollama_MultiTurn.py）把所有历史 append 进 message 列表，
  每轮 token 线性增长，且不相关的历史也被发给模型，浪费资源。

【本文件的解决方案：向量记忆（Vector Memory）】

  真实工作流（每轮对话执行以下步骤）：

  ┌─────────────────────────────────────────────────────────────┐
  │  Step 1: Query Rewriting                                    │
  │    用户说 "它有什么优点？"                                    │
  │    → 让模型改写成独立问题："RAG 有什么优点？"                  │
  │    → 目的：代词/指代不清 → 向量检索失准，改写后才准           │
  ├─────────────────────────────────────────────────────────────┤
  │  Step 2: Vector Retrieval（向量检索）                        │
  │    → 把改写后的问题转成向量（Embedding）                      │
  │    → 在历史向量库中找最相似的 k 条过去对话                    │
  │    → 只取相关的，不是全量历史                                 │
  ├─────────────────────────────────────────────────────────────┤
  │  Step 3: Build Messages                                     │
  │    → System + 检索到的相关历史（作为上下文） + 当前问题        │
  │    → 发给模型                                                │
  ├─────────────────────────────────────────────────────────────┤
  │  Step 4: Store Exchange                                     │
  │    → 把本轮 Human + AI 对话存入向量库（永久保存）             │
  │    → 下次有相关问题时才会被检索出来                           │
  └─────────────────────────────────────────────────────────────┘

  对比 MultiTurn（全量历史）：
  ┌──────────────┬─────────────────────┬────────────────────────┐
  │              │ MultiTurn（全量）     │ Memorial（向量记忆）    │
  ├──────────────┼─────────────────────┼────────────────────────┤
  │ 历史存储     │ 内存列表，程序退出丢失 │ 向量库，可持久化        │
  │ 每轮输入量   │ 线性增长             │ 固定（只取 top-k）      │
  │ 相关性       │ 全部历史都发          │ 只发相关的             │
  │ 实现复杂度   │ 简单                 │ 较复杂                 │
  └──────────────┴─────────────────────┴────────────────────────┘

【依赖安装】
  pip3 install chromadb langchain-chroma
  ollama pull nomic-embed-text   ← 专用 embedding 模型，约 274MB

How to run?
$ python3 案例与源码-2-LangChain框架/03-ollama/LangChain_Ollama_Memorial.py
"""

import uuid
from langchain_ollama import ChatOllama, OllamaEmbeddings
from langchain_chroma import Chroma
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage

# ══════════════════════════════════════════════════════════════
# 第一步：初始化三个核心组件
# ══════════════════════════════════════════════════════════════

# 1a. 对话模型 —— 用于 Query Rewriting 和主对话
chat_model = ChatOllama(
    base_url="http://localhost:11434",
    model="qwen3:4b",
    reasoning=False,
)

# 1b. Embedding 模型 —— 专门用于把文本转成向量
# nomic-embed-text 是轻量级专用 embedding 模型，不能用于对话，只能生成向量。
# 和 chat_model 是两个不同的工具，各司其职。
embeddings = OllamaEmbeddings(
    base_url="http://localhost:11434",
    model="nomic-embed-text",
)

# 1c. 向量数据库 —— 存储历史对话的向量表示
# collection_name: 数据库里的"表名"，可以有多个 collection 区分不同场景
# 这里用内存模式（不传 persist_directory），程序退出后清空。
# 生产环境改为：Chroma(..., persist_directory="./memory_db") 实现磁盘持久化
vector_store = Chroma(
    collection_name="conversation_memory",
    embedding_function=embeddings,
)

# ══════════════════════════════════════════════════════════════
# 第二步：定义工具函数
# ══════════════════════════════════════════════════════════════
# Why store both Q and A together as one entry?

# Because the answer often contains richer semantic content than the question alone. When a future question like "检索增强生成有什么用？" comes in, the embedding similarity is more likely to match against the full Q+A pair (which contains words like "检索"、"知识库"、"生成回答") than just the original question "什么是RAG？".

# If you only stored the question, you'd lose all the vocabulary from the answer during retrieval.
def store_exchange(human_msg: str, ai_msg: str):
    """
    把一轮对话（Human + AI）存入向量库。

    存储格式："用户: xxx\n助手: xxx"
    向量库会自动对这段文字做 embedding，后续按语义检索。
    """
    doc_text = f"用户: {human_msg}\n助手: {ai_msg}"
    # add_texts is a batch API — it's designed to insert multiple texts in one call, so both texts and ids are always arrays of the same length, matched by index:
    # ChromaDB 在插入前会做校验，texts 会被转成 embeddings（所以 embeddings 长度 = texts 长度 = 3），然后发现 ids 长度是 2，立刻报错，没有任何数据被写入——操作是原子性的，要么全成功，要么全失败
    vector_store.add_texts(
        texts=[doc_text],
        ids=[str(uuid.uuid4())],  # 每条记录需要唯一 ID
    )


def retrieve_relevant(query: str, k: int = 3) -> list:
    """
    把 query 转成向量，在历史库中找语义最相似的 k 条对话。

    similarity_search 内部做的事：
      1. 把 query 用 embedding 模型转成向量
      2. 用余弦相似度（cosine similarity）在向量库中比对
      3. 返回最相似的 k 条 Document

    返回：字符串列表，每条是"用户: xxx\n助手: xxx"格式
    """
    if vector_store._collection.count() == 0:
        return []  # 库里还没有历史，直接返回空
    results = vector_store.similarity_search(query, k=k)
    return [doc.page_content for doc in results]


def rewrite_query(user_input: str, recent_history: list) -> str:
    """
    Query Rewriting：把模糊问题改写成独立完整的问题。

    为什么需要这一步？
      用户说 "它有什么优点？" → 向量里没有"它"的语义，检索会失准。
      改写成 "RAG 有什么优点？" → 向量能正确匹配之前讨论 RAG 的历史。

    只有当历史不为空时才改写，首轮直接返回原始问题。
    只给最近 2 轮做参考（recent_history[-4:]），不需要全量历史。
    """
    if not recent_history:
        return user_input

    prompt = [
        SystemMessage(content=(
            "你是一个问题改写助手。根据对话历史，把用户的问题改写成一个独立、完整的句子，"
            "不依赖上下文也能看懂。只输出改写后的问题，不要任何解释或标点之外的内容。"
        )),
        *recent_history[-4:],  # 最近 2 轮 = 4 条消息（Human + AI × 2）
        HumanMessage(content=f"请改写这个问题：{user_input}"),
    ]
    result = chat_model.invoke(prompt)
    return result.content.strip()


# ══════════════════════════════════════════════════════════════
# 第三步：主对话循环
# ══════════════════════════════════════════════════════════════

SYSTEM_PROMPT = SystemMessage(content="你是一个简洁友好的助手，每次回答不超过100字。")

# recent_history：只保留最近 2 轮，仅用于 Query Rewriting。
# 不用于向模型发送全量历史（那是 MultiTurn 的做法）。
recent_history = []
exchange_count = 0  # 记录已存入向量库的对话轮数

print("向量记忆多轮对话已启动（输入 quit 退出）")
print("提示：试着问一个指代不清的问题（如'它有什么优点？'），观察 Query Rewriting 效果")
print("-" * 60)

while True:
    user_input = input("你：").strip()
    if user_input.lower() in ("quit", "exit"):
        print("再见！")
        break
    if not user_input:
        continue

    # ── Step 1: Query Rewriting ──────────────────────────────
    rewritten = rewrite_query(user_input, recent_history)
    if rewritten != user_input:
        print(f"  → [Query Rewriting] 原始: 「{user_input}」")
        print(f"                      改写: 「{rewritten}」")

    # ── Step 2: 向量检索相关历史 ─────────────────────────────
    relevant_docs = retrieve_relevant(rewritten, k=3)

    # ── Step 3: 构建本轮 message 列表 ────────────────────────
    messages = [SYSTEM_PROMPT]

    if relevant_docs:
        # 把检索到的历史拼成一段上下文，告诉模型"这是参考资料"
        context = "\n\n---\n".join(relevant_docs)
        messages.append(SystemMessage(
            content=f"以下是可能相关的历史对话，供参考（不一定全部相关）：\n\n{context}"
        ))
        print(f"  → [检索到 {len(relevant_docs)} 条相关历史]")
    else:
        print(f"  → [向量库为空，无历史参考]")

    messages.append(HumanMessage(content=user_input))

    # ── Step 4: 调用模型 ─────────────────────────────────────
    response = chat_model.invoke(messages)
    print(f"模型：{response.content}")

    # ── Step 5: 存入向量库（永久记忆）───────────────────────
    store_exchange(user_input, response.content)
    exchange_count += 1

    # ── Step 6: 更新 recent_history（仅用于下轮 Query Rewriting）
    recent_history.append(HumanMessage(content=user_input))
    recent_history.append(AIMessage(content=response.content))
    recent_history = recent_history[-4:]  # 只保留最近 2 轮

    # Token 统计
    meta = response.response_metadata
    input_tokens = meta.get("prompt_eval_count", 0)
    output_tokens = meta.get("eval_count", 0)
    print(f"[输入 tokens: {input_tokens} | 输出: {output_tokens} | 向量库已存: {exchange_count} 轮]")
    print("-" * 60)
