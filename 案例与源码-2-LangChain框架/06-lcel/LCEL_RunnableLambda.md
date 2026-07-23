# 4.6 RunnableLambda（函数链）

> 所属章节：[15 - LCEL 与链式调用](15-LCEL与链式调用.md) › 第 4 节 › 4.6

---

`RunnableLambda` 的价值在于：**把普通 Python 函数也变成 Runnable。**

这听起来像个小功能，但在实际开发里非常实用，因为链式流程经常会遇到这种情况：

- 上一步输出结构不符合下一步输入要求
- 想打印中间结果做调试
- 想做一次字段重命名
- 想插入一点简单业务逻辑

如果没有函数节点，你就得把链拆开，在外面手写很多中间处理代码；有了 `RunnableLambda`，这些逻辑就能被放回链内部。

例如：

```python
from langchain_core.runnables import RunnableLambda

def debug_print(x):
    print(x)
    return {"input": x}

chain = chain1 | RunnableLambda(debug_print) | chain2
```

LangChain 还支持一种更省事的写法：直接把函数放在 `|` 中间，框架会自动包装成 Runnable。

本章配套案例也同时演示了两种写法：

- `chain1 | debug_print | chain2`
- `chain1 | RunnableLambda(debug_print) | chain2`

【案例源码】`LCEL_RunnableLambdaDemo.py`

[LCEL_RunnableLambdaDemo.py](LCEL_RunnableLambdaDemo.py)

从项目角度看，RunnableLambda 很像"链里的胶水层"：

- 不负责模型能力
- 负责把前后节点粘起来

如果前面的"多步串行链"让你感受到"为什么需要中间映射"，这一节就是在回答"中间映射应该怎么优雅地写进链里"。

---

**相关章节：**

- 上一节：[4.5 RunnableParallel（并行链）](LCEL_RunnableParallel.md)
- 下一节：[4.7 其他常见 Runnable 结构](15-LCEL与链式调用.md#47-补充其他常见-runnable-结构)
