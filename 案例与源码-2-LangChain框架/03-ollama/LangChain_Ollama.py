"""
【案例】LangChain + Ollama 本地大模型对话

对应教程章节：第 12 章 - Ollama 本地部署与调用 → 5、LangChain 整合 Ollama 调用本地大模型

知识点速览：
- 本案例对应第 12 章里“把本地 Ollama 服务接进 LangChain”的最小落地示例。
- 使用 `ChatOllama` 连接本机模型，无需云端 API Key；前提是本机已安装并启动 Ollama，且已经拉取过目标模型。
- `base_url` 指向本机 Ollama 服务根地址（默认 `http://localhost:11434`），`model` 必须与 `ollama list` 里已存在的标签一致。
- `invoke()` 返回的仍然是 LangChain 语义下的 `AIMessage`，因此和第 11 章云端模型调用一样，正文通常用 `response.content` 读取。
"""

from langchain.messages import AIMessage, HumanMessage
from langchain_ollama import ChatOllama

# ---------- 第一步：创建“聊天客户端” ----------
# ChatOllama 是 LangChain 中连接本地 Ollama 服务的聊天模型类。
# 你可以把它理解成“本地模型版本的 Chat Model 客户端”：
# - base_url：Ollama 服务根地址（本机默认 http://localhost:11434）
# - model：已通过 ollama pull / ollama run 拉取到本机的模型标签
# - reasoning：是否开启推理/思考模式（是否支持取决于具体模型）- 可看到推理过程
model = ChatOllama(
    base_url="http://localhost:11434",
    model="qwen3:4b",
    reasoning=True,
)

# ---------- 第二步：发一条消息并打印回复 ----------

# ── 写法 A：直接传字符串（最简写法）──────────────────────────────────
# invoke() 接受一个纯字符串，LangChain 内部会自动把它包装成 HumanMessage。
# 适合：单轮问答、快速测试
#
# response = model.invoke("什么是LangChain，100字以内回答")

# ── 写法 B：传 HumanMessage 列表（推荐写法）──────────────────────────
# HumanMessage 是 LangChain 的消息对象，明确表示"这是用户说的话"。
# invoke() 也接受一个消息列表，列表里每条消息都有明确的角色（role）：
#   HumanMessage  → role: "user"     用户说的话
#   AIMessage     → role: "assistant" 模型之前的回复（用于多轮对话历史）
#   SystemMessage → role: "system"   系统提示，设定模型行为/人格
#
# 写法 B 的优势：
#   1. 可以传入多条消息，构造多轮对话历史（写法 A 只能传当前一句）
#   2. 可以加 SystemMessage 设置系统提示
#   3. 代码意图更清晰，在复杂应用中更易维护
#
# 示例：带系统提示的多轮对话
# from langchain_core.messages import SystemMessage, AIMessage
# messages = [
#     SystemMessage(content="你是一个简洁的助手，回答不超过50字"),
#     HumanMessage(content="什么是RAG？"),
#     AIMessage(content="RAG是检索增强生成，先从知识库检索相关内容再生成回答。"),
#     HumanMessage(content="它有什么优点？"),   # 当前问题
# ]
from langchain_core.messages import SystemMessage, AIMessage
messages = [
    SystemMessage(content="你是一个简洁的助手，回答不超过50字"),
    HumanMessage(content="什么是RAG？"),
    AIMessage(content="RAG是检索增强生成，先从知识库检索相关内容再生成回答。"),
    HumanMessage(content="它有什么优点？"),
   ]
response = model.invoke(messages)

# 打印思考过程
# reasoning=True 时 langchain-ollama 把思考内容放在 additional_kwargs["reasoning_content"]
# （注意：不是 "thinking"，这是 langchain-ollama 的具体实现）
thinking = response.additional_kwargs.get("reasoning_content", "")
if thinking:
    print("【模型思考过程】")
    print(thinking)
    print("-" * 50)

# 打印最终回答
print("【最终回答】")
print(response.content)
