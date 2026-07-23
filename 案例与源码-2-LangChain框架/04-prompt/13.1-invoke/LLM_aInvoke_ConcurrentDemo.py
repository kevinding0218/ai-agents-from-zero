"""
【案例】ainvoke 并发 vs invoke 顺序 —— 实际耗时对比

【核心问题：单条请求跑起来感觉一样，区别在哪里？】

  答：单条请求时，两者没有区别——你只有一件事要做，等，就是等。
  区别在于"同时发多条请求"时：

  同步 invoke（顺序执行）：
    请求1 → 等待 → 请求2 → 等待 → 请求3 → 等待     总耗时 = 三次相加

  异步 ainvoke（并发执行）：
    请求1 ─────┐
    请求2 ─────┤ 同时发出，谁先回来谁先处理            总耗时 ≈ 最慢那条
    请求3 ─────┘

  下面的代码会实际跑两种方式并打印耗时，差距一目了然。

【什么时候用 ainvoke？】
  - Web 服务同时来了多个用户请求
  - 批量处理（把 100 篇文章同时翻译，而不是一篇一篇等）
  - Agent 里同时调用多个工具或子问题

How to run?
$ python3 案例与源码-2-LangChain框架/04-prompt/13.1-invoke/LLM_aInvoke_ConcurrentDemo.py
"""

import asyncio
import os
import time
from dotenv import load_dotenv
from langchain_ollama import ChatOllama
from langchain.chat_models import init_chat_model

load_dotenv()

# ── 本地模型（Ollama）──────────────────────────────────────────
# 使用本地 Ollama，不需要 API Key，方便直接运行
local_model = ChatOllama(
    base_url="http://localhost:11434",
    model="qwen3:4b",
    reasoning=False,
)

# ── 远程模型（HuggingFace 远程 API）────────────────────────────
# 需要在 .env 里配置 HF_TOKEN，否则跳过远程部分
# 参考：案例与源码-2-LangChain框架/01-helloworld/LangChain_huggingface_chat_model_remote.py
_hf_token = os.getenv("HF_TOKEN")
remote_model = init_chat_model(
    model="Qwen/Qwen2.5-7B-Instruct",
    model_provider="huggingface",
    backend="endpoint",                        # 远程 API 模式，不下载模型到本机
    huggingfacehub_api_token=_hf_token,
    max_new_tokens=256,
) if _hf_token else None

# 三个互相独立的问题——彼此没有依赖，是理想的并发场景
questions = [
    "用一句话解释什么是机器学习",
    "用一句话解释什么是深度学习",
    "用一句话解释什么是强化学习",
]


def _pick_model(usingLocal: bool):
    """根据 usingLocal 标志选择模型，并检查远程模型是否已配置。"""
    if usingLocal:
        return local_model
    if remote_model is None:
        raise RuntimeError("未找到 HF_TOKEN，请在 .env 中配置后重新运行")
    return remote_model


# ══════════════════════════════════════════════════════════════
# 方式 A / C：同步顺序调用（invoke）
# ══════════════════════════════════════════════════════════════
# 每次 invoke 都阻塞：第 1 个回来才开始第 2 个，第 2 个回来才开始第 3 个。
# 总耗时 = 所有单次耗时之和
def run_sequential(usingLocal: bool = True):
    label = "本地 Ollama" if usingLocal else "远程 HuggingFace"
    model = _pick_model(usingLocal)
    print(f"\n【同步顺序调用 - {label}（invoke）】")
    t0 = time.time()
    for i, q in enumerate(questions):
        r = model.invoke(q)
        elapsed = time.time() - t0
        print(f"  问题{i+1} 完成 ({elapsed:.1f}s): {r.content[:50]}...")
    total = time.time() - t0
    print(f"  → 总耗时: {total:.1f}s")
    return total


# ══════════════════════════════════════════════════════════════
# 方式 B / D：异步并发调用（ainvoke + asyncio.gather）
# ══════════════════════════════════════════════════════════════
# asyncio.gather() 同时发起所有请求，等所有结果都回来后才继续。
# 总耗时 ≈ 最慢那条请求的耗时（不是相加）
async def run_concurrent(usingLocal: bool = True):
    label = "本地 Ollama" if usingLocal else "远程 HuggingFace"
    model = _pick_model(usingLocal)
    print(f"\n【异步并发调用 - {label}（ainvoke + asyncio.gather）】")
    t0 = time.time()

    # 创建三个协程（此时还没开始执行，只是"准备好的任务"）
    coroutines = [model.ainvoke(q) for q in questions]

    # gather() 同时启动所有协程，等所有结果回来后统一返回
    results = await asyncio.gather(*coroutines)

    total = time.time() - t0
    for i, r in enumerate(results):
        print(f"  问题{i+1}: {r.content[:50]}...")
    print(f"  → 总耗时: {total:.1f}s")
    return total


# ══════════════════════════════════════════════════════════════
# 主程序：分别跑本地和远程，各自对比顺序 vs 并发
# ══════════════════════════════════════════════════════════════
async def main():
    results = {}

    # ── 本地 Ollama ──────────────────────────────────────────
    print("=" * 60)
    print("本地 Ollama（共享 GPU/CPU，并发加速比低于理论值）")
    print("=" * 60)
    results["local_seq"] = run_sequential(usingLocal=True)
    results["local_con"] = await run_concurrent(usingLocal=True)
    local_speedup = results["local_seq"] / results["local_con"] if results["local_con"] > 0 else 0
    print(f"\n本地：顺序 {results['local_seq']:.1f}s → 并发 {results['local_con']:.1f}s  加速 {local_speedup:.1f}x")

    # ── 远程 HuggingFace ─────────────────────────────────────
    if remote_model:
        print()
        print("=" * 60)
        print("远程 HuggingFace（独立服务器，加速比接近 3x）")
        print("=" * 60)
        results["remote_seq"] = run_sequential(usingLocal=False)
        results["remote_con"] = await run_concurrent(usingLocal=False)
        remote_speedup = results["remote_seq"] / results["remote_con"] if results["remote_con"] > 0 else 0
        print(f"\n远程：顺序 {results['remote_seq']:.1f}s → 并发 {results['remote_con']:.1f}s  加速 {remote_speedup:.1f}x")

        print()
        print("=" * 60)
        print("对比总结")
        print("=" * 60)
        print(f"  本地 Ollama        顺序: {results['local_seq']:.1f}s  并发: {results['local_con']:.1f}s  加速: {local_speedup:.1f}x")
        print(f"  远程 HuggingFace   顺序: {results['remote_seq']:.1f}s  并发: {results['remote_con']:.1f}s  加速: {remote_speedup:.1f}x")
        print()
        print("  结论：远程 API 受益于并发更明显——每条请求打到独立服务器，")
        print("        不存在本地共享算力的竞争问题。")
    else:
        print()
        print("（跳过远程对比：未找到 HF_TOKEN，请在 .env 中配置后重新运行）")


if __name__ == "__main__":
    asyncio.run(main())
