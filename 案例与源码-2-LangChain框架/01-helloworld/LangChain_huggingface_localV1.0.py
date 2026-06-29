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
"""

# ========== 1. 导入依赖 ==========
import os
from dotenv import load_dotenv
from langchain.chat_models import init_chat_model  # 与远程写法完全相同的入口

load_dotenv(encoding="utf-8")

# ========== 2. 本地运行写法 ==========
# 与远程 API 的区别：
# - 不传 backend="endpoint"，默认 backend="pipeline"（本地运行）
# - 不需要 huggingfacehub_api_token（不调用远程，token 可选）
# - 第一次运行会自动下载模型文件到本机 ~/.cache/huggingface/
model = init_chat_model(
    model="HuggingFaceTB/SmolLM2-135M-Instruct",  # 最小聊天模型，仅 135M 参数，约 270 MB
    model_provider="huggingface",
    # backend 默认就是 "pipeline"（本地运行），不需要写
    # 首次运行会下载到 ~/.cache/huggingface/，之后复用缓存
)

print(model.invoke("Who are you?").content)

"""
【实际输出示例】（SmolLM2-135M，本机 CPU 运行）
I'm a helpful AI assistant named SmolLM, a botanist and gardener...

注意：135M 模型非常小，回答质量较差（比如上面它以为自己是植物学家）。
这是正常现象——模型越小，能力越弱。本文件的目的只是验证本地运行流程，不追求回答质量。

【首次运行过程】
1. 自动从 HuggingFace 下载模型权重到 ~/.cache/huggingface/（约 270 MB）
2. 加载到内存
3. 在本机 CPU 上推理并输出结果
第二次运行直接从缓存加载，无需重新下载。
"""
