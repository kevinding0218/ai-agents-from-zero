"""
【案例】init_chat_model 统一入口 vs 各自的原生 API

【核心问题一：这些模型都能用 init_chat_model 吗？】

  答：大多数可以。init_chat_model 内部维护了一张 model_provider → 对应类 的映射表：

  ┌──────────────────────┬───────────────────────────────────┐
  │ model_provider 值    │ 底层实际使用的类                   │
  ├──────────────────────┼───────────────────────────────────┤
  │ "openai"             │ ChatOpenAI      (langchain-openai) │
  │ "deepseek"           │ ChatDeepSeek    (langchain-deepseek)│
  │ "ollama"             │ ChatOllama      (langchain-ollama) │
  │ "huggingface"        │ HuggingFace     (langchain-huggingface)│
  │ "anthropic"          │ ChatAnthropic   (langchain-anthropic)│
  │ "tongyi"             │ ChatTongyi      (langchain-community)│
  │ "google_genai"       │ ChatGoogleGenAI (langchain-google-genai)│
  └──────────────────────┴───────────────────────────────────┘

  init_chat_model 就是一个"工厂函数"：
  你告诉它用哪个 provider，它帮你 new 出对应的类实例。
  其他参数（api_key、base_url、model）直接透传给那个类。

【核心问题二：既然 init_chat_model 能统一，为什么每个模型还要有自己的原生 API？】

  init_chat_model 的优势：                 原生类（如 ChatOpenAI）的优势：
  ────────────────────────────           ────────────────────────────────
  ✅ 一套代码，随意切换 provider           ✅ 原生特性全部暴露（如流控、原生参数）
  ✅ 适合写通用框架/中间件                 ✅ IDE 能自动补全所有参数名
  ✅ 不需要记各类的 import 路径            ✅ 某些厂商独有功能只在原生类支持
  ❌ 参数提示弱，多传错参不易发现           ❌ 换模型就要改 import + 类名
  ❌ 极个别新模型可能还未注册              ❌ 代码耦合特定厂商

  结论：
  - 学习期 / 快速验证 → init_chat_model（切换成本低）
  - 生产代码 / 需要原生特性 → 用对应的原生类

【对应关系速查】

  本文件 (init_chat_model)    其他文件 (原生类)
  ─────────────────────────   ──────────────────────────────
  provider="openai" + qwen    ↔ ModelIO_ChatOpenAI.py (ChatOpenAI)
  provider="deepseek"         ↔ ModelIO_DeepSeek.py   (ChatDeepSeek)
  provider="tongyi"           ↔ ModelIO_Qwen.py       (ChatTongyi)
  provider="ollama"           ↔ 03-ollama/LangChain_Ollama.py (ChatOllama)
  provider="huggingface"      ↔ 01-helloworld/LangChain_huggingface_*.py

How to run?
$ python3 案例与源码-2-LangChain框架/02-models_io/ModelIO_Init_chat_model.py
"""

from dotenv import load_dotenv
import os
from langchain.chat_models import init_chat_model

load_dotenv(encoding="utf-8")

# ══════════════════════════════════════════════════════════════
# ⚠️ 重要：init_chat_model 在创建实例时就验证 API Key
# 不像普通对象可以先创建再说，如果 API Key 为空会立刻报错。
# 所以下面每段只有在有对应 Key 时才取消注释运行。
# ══════════════════════════════════════════════════════════════

QUESTION = "用一句话解释什么是 LangChain。"

# ── 写法 A：OpenAI 兼容接口（阿里百炼/通义）──────────────────
# 对应原生类：ModelIO_ChatOpenAI.py 里的 ChatOpenAI
# 当模型厂商提供了 OpenAI 兼容端点时（base_url），
# 用 model_provider="openai" + base_url 接入任意兼容厂商。
# init_chat_model 创建 ChatOpenAI 实例，把 base_url/api_key 透传进去。
# 需要：aliQwen-api 已填入 .env
#
# model_qwen = init_chat_model(
#     model="qwen-plus",
#     model_provider="openai",        # → 底层用 ChatOpenAI
#     api_key=os.getenv("aliQwen-api"),
#     base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
# )
# print("【Qwen via OpenAI compat】", model_qwen.invoke(QUESTION).content)

# ── 写法 B：DeepSeek 原生 provider ──────────────────────────
# 对应原生类：ModelIO_DeepSeek.py 里的 ChatDeepSeek
# 有官方集成包（langchain-deepseek）时，无需手写 base_url，
# init_chat_model 会 import ChatDeepSeek 并填入官方地址。
# 需要：deepseek-api 已填入 .env，pip install langchain-deepseek
#
# model_deepseek = init_chat_model(
#     model="deepseek-v4-flash",
#     model_provider="deepseek",      # → 底层用 ChatDeepSeek
#     api_key=os.getenv("deepseek-api"),
# )
# print("【DeepSeek native】", model_deepseek.invoke(QUESTION).content)

# ── 写法 C：通义千问 原生 provider ──────────────────────────
# 对应原生类：ModelIO_Qwen.py 里的 ChatTongyi
# 需要：aliQwen-api 已填入 .env，pip install langchain-community dashscope
#
# model_tongyi = init_chat_model(
#     model="qwen-plus",
#     model_provider="tongyi",        # → 底层用 ChatTongyi
#     api_key=os.getenv("aliQwen-api"),
# )
# print("【Tongyi native】", model_tongyi.invoke(QUESTION).content)

# ── 写法 D：Ollama 本地模型（✅ 本机可直接运行）────────────────
# 对应原生类：03-ollama/ 里的 ChatOllama
# Ollama 在本机运行，不需要 api_key。
# init_chat_model 创建 ChatOllama 实例，默认连接 localhost:11434。
model_ollama = init_chat_model(
    model="qwen3:4b",
    model_provider="ollama",          # → 底层用 ChatOllama
)
print("【Ollama - qwen3:4b】")
print(model_ollama.invoke(QUESTION).content)

# ── 写法 E：HuggingFace 远程 API ────────────────────────────
# 对应原生类：01-helloworld/ 里的 HuggingFaceEndpoint + ChatHuggingFace
# backend="endpoint" 走远程推理 API，不在本地下载模型。
# 需要：HF_TOKEN 已填入 .env，pip install langchain-huggingface
#
# model_hf = init_chat_model(
#     model="Qwen/Qwen2.5-7B-Instruct",
#     model_provider="huggingface",
#     backend="endpoint",             # 关键：远程 API 而非本地下载
#     huggingfacehub_api_token=os.getenv("HF_TOKEN"),
# )
# print("【HuggingFace remote】", model_hf.invoke(QUESTION).content)

# ══════════════════════════════════════════════════════════════
# 关键总结：init_chat_model 的最大价值
# ══════════════════════════════════════════════════════════════
# 无论底层是哪个 provider，invoke / stream / ainvoke 调用方式完全相同。
# 切换模型只需改初始化参数，不需要改任何调用代码。
# 这让你可以写"与 provider 无关"的业务逻辑代码。
