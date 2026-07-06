"""
【案例】LangChain + Ollama 多轮对话（动态构建消息历史）

知识点速览：
- 模型本身无记忆，每次 invoke() 都是全新的无状态请求。
- 要实现多轮对话，需要在代码里维护一个 history 列表，
  每轮把完整历史（System + 所有过去的 Human/AI 消息 + 当前问题）一起发给模型。
- 每轮结束后把模型回复作为 AIMessage 追加到 history，
  下一轮发送时模型就能"记住"前面说了什么。

消息角色对照：
  SystemMessage  → role: "system"    开发者设定的规则/人格，只需写一次放在列表最前面
  HumanMessage   → role: "user"      用户说的话
  AIMessage      → role: "assistant" 模型之前的回复（由代码追加，不是模型自己维护的）

history 随轮次增长示意：
  第1轮发送: [System, Human("问题1")]
  第1轮回复: AI("回答1")  → 追加到 history

  第2轮发送: [System, Human("问题1"), AI("回答1"), Human("问题2")]
  第2轮回复: AI("回答2")  → 追加到 history

  第3轮发送: [System, Human("问题1"), AI("回答1"), Human("问题2"), AI("回答2"), Human("问题3")]
  ...以此类推

【问题：一直 append 不是浪费资源吗？】

是的。Token 统计实验证明输入 token 随轮次线性增长：
  轮次1 输入: ~34 tokens
  轮次2 输入: ~598 tokens  （涨了 17 倍，因为历史 + 思考过程都被包含）
  轮次3 输入: ~1003 tokens

而且用户的问题不一定有关联性——当前问题可能只和第6轮有关，
却要把所有历史都发给模型，造成浪费。

【三种主流解决方案】

  方案1：固定窗口（最简单）
    只保留最近 N 轮，更早的丢掉。
    缺点：模型忘记早期内容。
    实现：history = history[-N*2:]（每轮 2 条消息）

  方案2：摘要压缩
    把旧历史让模型总结成一段话，替换掉原始消息。
    缺点：细节丢失，多一次 API 调用。
    LangChain 内置：ConversationSummaryMemory

  方案3：向量检索记忆（最完整，也是 RAG 的核心应用）
    所有历史永久存入向量数据库，每轮只检索"最相关的 N 条"放进 message。
    流程：
      用户输入 → 转成向量 → 去历史库检索相似片段
                                  ↓
                    只把相关片段 + 当前问题发给模型
                                  ↓
                    把本轮 Human + AI 存回历史库（永久保存但不全发）

    难点：用户说"它有什么优点？"，"它"没有语义，向量检索会失准。
    解决：Query Rewriting —— 先让模型把问题改写成独立完整的句子再检索：
      原始：  "它有什么优点？"
      改写后：  "RAG（检索增强生成）有什么优点？"  ← 再拿这句话去向量库检索

    LangChain 后续章节会实现这套完整架构。

How to run?
$ python3 案例与源码-2-LangChain框架/03-ollama/LangChain_Ollama_MultiTurn.py
"""

from langchain_ollama import ChatOllama
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage

# ---------- 第一步：创建模型客户端 ----------
model = ChatOllama(
    base_url="http://localhost:11434",
    model="qwen3:4b",
    reasoning=False,  # 关闭思考模式，回复更快；改成 True 可看到推理过程
)

# ---------- 第二步：初始化对话历史 ----------
# history 列表贯穿整个对话，每轮都会往里追加消息。
# SystemMessage 只需要一条，放在最前面，对所有轮次生效。
history = [
    SystemMessage(content="你是一个简洁友好的助手，每次回答不超过100字。"),
]

print("多轮对话已启动（输入 quit 或 exit 退出）")
print("模型会记住你之前说的话，试试问一个后续问题！")
print("-" * 50)

# ---------- 第三步：对话循环 ----------
while True:
    user_input = input("你：").strip()
    if user_input.lower() in ("quit", "exit"):
        print("再见！")
        break
    if not user_input:
        continue

    # 把用户这句话加入历史
    history.append(HumanMessage(content=user_input))

    # 把完整历史发给模型（System + 所有过去消息 + 当前问题）
    response = model.invoke(history)

    print(f"模型：{response.content}")

    # 把模型的回复也加入历史，下一轮模型才能"记住"它说过什么
    history.append(AIMessage(content=response.content))

    # Token 统计：来自 response_metadata（Ollama 每次都会返回这些数据）
    # prompt_eval_count  = 本轮发送给模型的 token 总数（包含完整历史）
    # eval_count         = 模型本轮生成的 token 数（回复长度）
    # 随着轮次增加，prompt_eval_count 会持续增长，印证了"历史越长越贵"
    meta = response.response_metadata
    input_tokens  = meta.get("prompt_eval_count", 0)
    output_tokens = meta.get("eval_count", 0)
    turn = (len(history) - 1) // 2
    print(f"[轮次 {turn} | 输入 tokens: {input_tokens} | 输出 tokens: {output_tokens} | 合计: {input_tokens + output_tokens}]")
    print("-" * 50)
