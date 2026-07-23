# 4.5 RunnableParallel（并行链）

> 所属章节：[15 - LCEL 与链式调用](15-LCEL与链式调用.md) › 第 4 节 › 4.5

---

`RunnableParallel` 解决的是另一个常见问题：**同一份输入，我想同时交给多条链处理。**

它的核心特点是：多条子链共享同一输入、同时执行，最后再把结果按键汇总成一个 `dict`。

![RunnableParallel 并行链：同一输入进入多条 Runnable，并将结果汇总成 dict 后交给下游链](../../images/15/15-4-5-1.svg)

从执行机制上理解也很有帮助：

- 同步执行时，LangChain 通常会并发调度这些子 Runnable
- 异步执行时，思路上就是把多个异步任务一起等待后再汇总结果

你不一定需要一开始就记住底层细节，但要知道：**并行链不是"写在一起"，而是真的在"同一输入下多路执行"。**

例如：

```python
parallel_chain = RunnableParallel({
    "chinese": chain1,
    "english": chain2,
})
```

除了显式使用 `RunnableParallel(...)`，LCEL 里还有一个非常实用的写法：**直接用字典表达并行结构**。例如：

```python
parallel_then_summary = {
    "paragraph_1": chain1,
    "paragraph_2": chain2,
} | summary_chain
```

这段写法的含义是：

1. 先并行运行 `chain1` 和 `chain2`
2. 把结果汇总成一个字典
3. 再把这个字典交给后面的 `summary_chain`

这类写法在实际项目里很常见，因为很多业务并不是"并行完就结束"，而是"并行生成多个结果后，再统一分析或总结"。

调用后返回结果可能像这样：

```python
{
    "chinese": "...",
    "english": "..."
}
```

根据 LangChain 官方参考，`RunnableParallel` 是与 `RunnableSequence` 并列的另一个核心组合原语。

这种链在实际项目里也非常常见：

- 同一问题同时生成中英文答案
- 同一问题同时走多个模型做对比
- 同一份输入同时做多个维度分析

下面这个案例就是一个很清晰的例子：

- 一条子链用中文介绍 `LangChain`
- 另一条子链用英文介绍 `LangChain`
- 然后一次性返回两个结果

【案例源码】`LCEL_RunnableParallelDemo.py`

[LCEL_RunnableParallelDemo.py](LCEL_RunnableParallelDemo.py)

这个案例还有一个很有价值的补充点：它调用了 `get_graph().print_ascii()`。这有助于你从"代码链"过渡到"图结构"的理解，为后续学习 LangGraph 做铺垫。

---

**相关章节：**

- 上一节：[4.4 Multi-Step Chain（多步串行链）](LCEL_MultiStepChain.md)
- 下一节：[4.6 RunnableLambda（函数链）](LCEL_RunnableLambda.md)
