"""
【案例】HuggingFace 本地运行方式（用最小模型 SmolLM2-135M 测试本地执行）

本地运行 vs 远程 API 对比：

┌─────────────────┬──────────────────────────┬──────────────────────────┐
│                 │ 本地运行（此文件）          │ 远程 API（huggingfaceV1） │
├─────────────────┼──────────────────────────┼──────────────────────────┤
│ 模型存放位置     │ 下载到你的电脑              │ 存放在 HuggingFace 服务器  │
│ 首次运行        │ 下载模型文件（7B ≈ 14 GB）  │ 直接调用，秒级响应         │
│ 运行速度        │ 取决于本机 CPU/GPU         │ 取决于网络和对方服务器      │
│ 需要 GPU        │ 强烈建议（无 GPU 极慢）     │ 不需要                    │
│ 需要安装        │ transformers + PyTorch    │ 只需 langchain-huggingface │
│ 网络依赖        │ 仅首次下载需要网络          │ 每次调用都需要网络          │
│ 隐私            │ 数据不离开本机              │ 数据发送到 HuggingFace     │
│ 费用            │ 免费（下载后）              │ 免费（有速率限制）          │
└─────────────────┴──────────────────────────┴──────────────────────────┘

安装依赖（仅本地运行需要）：
    pip3 install torch
    # 注：pip3 install torch 2>&1 | tail -5 是加了两个过滤器，让输出更简洁：
    #   2>&1    → 把错误信息（stderr）合并到普通输出（stdout），防止遗漏报错
    #   | tail -5 → 只显示最后 5 行，跳过几百行下载进度条，直接看结果
    #   平时自己用 pip3 install torch 即可，完整进度条反而更直观

如何查看本机有什么 GPU（在 Python 中运行）：
    import torch
    print(torch.backends.mps.is_available())  # True = Mac Apple Silicon GPU 可用
    print(torch.cuda.is_available())           # True = NVIDIA GPU 可用
    # 本机结果：MPS=True, CUDA=False → 使用 Apple M 系列芯片 GPU

How to run?
$ python3 案例与源码-2-LangChain框架/01-helloworld/LangChain_huggingface_chat_model_local.py
"""

# ========== 1. 导入依赖 ==========
import os
import torch
from dotenv import load_dotenv
from transformers import pipeline as hf_pipeline, AutoTokenizer  # AutoTokenizer 用于手动统计 token
from langchain_huggingface import HuggingFacePipeline, ChatHuggingFace
from langchain.chat_models import init_chat_model

load_dotenv(encoding="utf-8")

MODEL_ID = "HuggingFaceTB/SmolLM2-135M-Instruct"  # 最小聊天模型，仅 135M 参数，约 270 MB

# tokenizer 两种写法都需要（用于手动统计 token，本地 pipeline 不返回 usage_metadata）
tokenizer = AutoTokenizer.from_pretrained(MODEL_ID)


# ========== 写法 A：init_chat_model 统一入口 ==========
# 优点：与其他章节风格一致，写法简洁
# 限制：device 只接受整数（-1=CPU, 0=第一块CUDA GPU），不支持 "mps" 字符串
#       → Mac Apple Silicon GPU (MPS) 无法通过此写法使用，只能跑 CPU
if torch.cuda.is_available():
    device_int = 0      # CUDA 第一块 GPU
else:
    device_int = -1     # CPU（MPS 也走这里，因为 init_chat_model 不认识 "mps"）

print(f"写法A 使用设备：{'CUDA' if device_int == 0 else 'CPU（MPS 不支持此写法）'}")

model = init_chat_model(
    model=MODEL_ID,
    model_provider="huggingface",
    device=device_int,  # 整数：-1=CPU, 0=CUDA GPU（MPS 不支持）
)


# ========== 写法 B：transformers pipeline + HuggingFacePipeline + ChatHuggingFace ==========
# 优点：支持所有设备包括 MPS（Apple Silicon GPU），控制粒度更细
# 写法更繁琐，但灵活性更高

# if torch.backends.mps.is_available():
#     device_str = "mps"
# elif torch.cuda.is_available():
#     device_str = "cuda"
# else:
#     device_str = "cpu"
#
# print(f"写法B 使用设备：{device_str}")
#
# pipe = hf_pipeline("text-generation", model=MODEL_ID, device=device_str)
# llm = HuggingFacePipeline(pipeline=pipe)
# model = ChatHuggingFace(llm=llm)

print("模型已就绪，输入问题开始对话（输入 quit 或 exit 退出）")
print("-" * 50)

while True:
    user_input = input("你：").strip()

    if user_input.lower() in ("quit", "exit"):
        print("再见！")
        break

    if not user_input:
        continue

    response = model.invoke(user_input)

    print(f"模型回复正文：{response.content}")
    print(f"厂商返回的原始元数据：{response.response_metadata}")
    print(f"LangChain 统一整理后的用量信息：{response.usage_metadata}")
    print(f"工具调用信息：{response.tool_calls}")
    print(f"厂商扩展字段：{response.additional_kwargs}")


    # 本地 pipeline 不返回 usage_metadata（None），需要用 tokenizer 手动统计
    # tokenizer.encode() 把文字转成 token ID 列表，len() 计算数量
    input_tokens = len(tokenizer.encode(user_input))
    output_tokens = len(tokenizer.encode(response.content))
    print(f"[token 消耗] 输入: {input_tokens}  输出: {output_tokens}  合计: {input_tokens + output_tokens}（本地统计，不计费）")
    print("-" * 50)

"""
【实际输出示例】（SmolLM2-135M，本机 CPU 运行）
模型已就绪，输入问题开始对话（输入 quit 或 exit 退出）
--------------------------------------------------
你：who are you?
模型：I'm a helpful AI assistant named SmolLM, a botanist and gardener...
[token 消耗] 输入: 20  输出: 38  合计: 58
--------------------------------------------------

注意：135M 模型非常小，回答质量较差（比如它有时以为自己是植物学家）。
这是正常现象——模型越小，能力越弱。本文件的目的只是验证本地运行流程，不追求回答质量。

【首次运行过程】
1. 自动从 HuggingFace 下载模型权重到 ~/.cache/huggingface/（约 270 MB）
2. 加载到内存
3. 在本机 CPU 上推理并输出结果
第二次运行直接从缓存加载，无需重新下载。
"""
