"""
【案例】LangChain 1.0 写法：通过 HuggingFace 远程 API 调用开源聊天模型

对应教程章节：第 10 章 - LangChain 快速上手与 HelloWorld → HuggingFace 集成

知识点速览：
- HuggingFace 有两种调用方式：
    1. 本地运行（pipeline）：下载模型到本机，需要 PyTorch，7B 模型需要数 GB 显存，不适合入门
    2. 远程 API（endpoint）：调用 HuggingFace 的云端服务，只需 token，本机无需 GPU ← 我们用这种
- 远程 API 有两种写法（功能完全相同，任选其一）：
    写法 A：init_chat_model 统一入口（与 LangChainV1.0.py 风格一致，需额外传 backend 参数）
    写法 B：HuggingFaceEndpoint + ChatHuggingFace 显式写法（更直观，层次更清晰）
- 模型 ID 格式为 "作者/模型名"，与 HuggingFace 网页路径一致

How to run?
$ python3 案例与源码-2-LangChain框架/01-helloworld/LangChain_huggingfaceV1.0.py
"""

# ========== 1. 导入依赖 ==========
import os
from dotenv import load_dotenv

load_dotenv(encoding="utf-8")

# ========== 写法 A：init_chat_model 统一入口（推荐，与其他章节风格一致）==========
from langchain.chat_models import init_chat_model

# init_chat_model 支持 HuggingFace 远程 API，需额外传两个参数：
# - backend="endpoint"：指定远程 API 模式（默认是本地 pipeline，会触发下载）
# - huggingfacehub_api_token：HuggingFace 专用的 token 参数名（注意不叫 api_key）
model = init_chat_model(
    model="Qwen/Qwen2.5-7B-Instruct",
    model_provider="huggingface",
    backend="endpoint",                                 # 关键：指定远程 API 模式
    huggingfacehub_api_token=os.getenv("HF_TOKEN"),    # HuggingFace token 参数名
    max_new_tokens=1024,                                # 模型最多生成的 token 数，默认 512，调大避免回答被截断
)

# ========== 3. 交互式对话循环 ==========
# input() 读取控制台输入，程序会一直等待，直到用户输入 "quit" 或 "exit"
# response.usage_metadata 包含本次调用消耗的 token 数（输入 + 输出）
# 远程调用（endpoint）：token 由 HuggingFace 服务器计算并返回
# 本地运行（pipeline）：token 由本机 tokenizer 统计，不产生任何费用

print("模型已就绪，输入问题开始对话（输入 quit 或 exit 退出）")
print("-" * 50)

while True:
    user_input = input("你：").strip()

    if user_input.lower() in ("quit", "exit"):
        print("再见！")
        break

    if not user_input:       # 防止空输入直接回车
        continue

    response = model.invoke(user_input)

    print(f"模型回复正文：{response.content}")
    print(f"厂商返回的原始元数据：{response.response_metadata}")
    print(f"LangChain 统一整理后的用量信息：{response.usage_metadata}")
    print(f"工具调用信息：{response.tool_calls}")
    print(f"厂商扩展字段：{response.additional_kwargs}")

    # usage_metadata 结构：{'input_tokens': N, 'output_tokens': N, 'total_tokens': N}
    usage = response.usage_metadata
    print(f"[token 消耗] 输入: {usage['input_tokens']}  输出: {usage['output_tokens']}  合计: {usage['total_tokens']}")
    print("-" * 50)


# ========== 写法 B：HuggingFaceEndpoint + ChatHuggingFace 显式写法 ==========
# 与写法 A 完全等价，但拆成两步，层次更清晰：
# 第一步 HuggingFaceEndpoint = 负责连接远程 API（相当于"拨号"）
# 第二步 ChatHuggingFace     = 套一层聊天接口，让它支持 invoke/stream 等 LangChain 标准方法

# from langchain_huggingface import ChatHuggingFace, HuggingFaceEndpoint
#
# llm = HuggingFaceEndpoint(
#     repo_id="Qwen/Qwen2.5-7B-Instruct",
#     huggingfacehub_api_token=os.getenv("HF_TOKEN"),
# )
# model = ChatHuggingFace(llm=llm)
#
# print(model.invoke("Who are you?").content)


"""
【输出示例】
I am Qwen, a large language model created by Alibaba Cloud...

【常见错误】
- 401 Unauthorized: HF_TOKEN 未设置或 token 无效，在 .env 文件中添加 HF_TOKEN=hf_xxxxx
- 503 Service Unavailable: 模型正在冷启动，等待 30 秒后重试（免费 API 限制）
- model_not_supported: 该模型未在 HuggingFace router 注册为聊天模型，换一个模型试试
"""
