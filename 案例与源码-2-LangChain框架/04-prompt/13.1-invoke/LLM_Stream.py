"""
【案例】模型调用：同步 stream（流式输出）

对应教程章节：第 13 章 - 提示词与消息模板 → 4、调用大模型的调用方式

知识点速览：
- `stream` 适合做“边生成边展示”的交互体验，尤其常见于聊天界面和长文本生成。
- 与 `invoke` 不同，`stream` 返回的是可迭代对象；循环中的每一块通常是 `AIMessageChunk`。
- 项目里如果你只想先跑通最基础调用，优先学 `invoke`；需要打字机效果时再切到 `stream`。

How to run?
$ python3 案例与源码-2-LangChain框架/04-prompt/13.1-invoke/LLM_Stream.py
"""

import time
from langchain_ollama import ChatOllama
from langchain_core.messages import HumanMessage, SystemMessage

# ---------- 1. 实例化模型 ----------
# 使用本地 Ollama，不依赖外部 API Key，且没有 HuggingFace tokenizers Rust 库
# 在 Python 退出时触发 segmentation fault 的问题。
# 如需改用远程 API，取消下面任意一段的注释：
#
# from dotenv import load_dotenv; import os; load_dotenv()
# model = init_chat_model(          # Qwen 兼容接口
#     model="qwen-plus", model_provider="openai",
#     api_key=os.getenv("aliQwen-api"),
#     base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
# )
# model = init_chat_model(          # HuggingFace 远程 API（注意：该库退出时会 segfault）
#     model="Qwen/Qwen2.5-7B-Instruct", model_provider="huggingface",
#     backend="endpoint", huggingfacehub_api_token=os.getenv("HF_TOKEN"),
# )

model = ChatOllama(
    base_url="http://localhost:11434",
    model="qwen3:4b",
    reasoning=False,
)

# ---------- 2. 构建多角色消息（同 invoke）----------
messages = [
    SystemMessage(content="你叫小问，是一个乐于助人的AI人工助手"),
    HumanMessage(content="你是谁"),
]

# ---------- 3. 流式调用：model.stream(messages) ----------
# stream 返回的是一个「生成器」（generator），不会等模型全部生成完才返回；
# 边生成边 yield，每次 for 取到的一小块通常是 AIMessageChunk。所以这里 type(response) 是 generator。
# 对比：invoke/ainvoke 等全部生成完一次性返回 → 类型是 AIMessage（一条完整消息）。
response = model.stream(messages)
print(f"响应类型：{type(response)}")

# ── 模式 1：打字机效果（正常使用方式）────────────────────────
# chunk.content 是当前这一小段文本；end="" 不换行，flush=True 立即输出到屏幕
# 这样用户会看到文字像打字机一样一点点出现，而不是等全部生成完才显示
print("── 模式 1：打字机效果 ──")
for chunk in response:
    print(chunk.content, end="", flush=True)
    time.sleep(0.1)  # 每块之间暂停 1s，让打字机效果更明显（实际使用可去掉）
print("\n")


response2 = model.stream(messages)
print(f"响应类型：{type(response)}")
# ── 模式 2：逐块检查（用于理解 chunk 是什么）────────────────
# 重新发一次请求，这次把每个 chunk 的完整信息打印出来
# chunk 是 AIMessageChunk，不是完整的 AIMessage：
#   - chunk.content      → 这一块的文字片段（可能只有 1-3 个字）
#   - chunk.response_metadata → 只有最后一块才有（包含 stop reason 等信息）
print("── 模式 2：逐块检查（看清 chunk 结构）──")
for i, chunk in enumerate(response2):
    has_meta = bool(chunk.response_metadata)
    print(f"  chunk[{i:02d}] content={repr(chunk.content):<25} has_metadata={has_meta}")
    time.sleep(0.1)
print()


"""
【输出示例】
响应类型：<class 'generator'>
你好呀！我是小问，一个乐于助人的AI人工助手～😊
我擅长解答问题、帮你理清思路、写文案、做学习规划、整理资料，甚至陪你聊聊天、出出主意。不管是学习上的难题、工作中的困惑，还是生活里的小烦恼，我都很乐意倾听和帮忙！
"""

# 你今天有什么想了解的，或者需要我帮什么忙吗？✨
