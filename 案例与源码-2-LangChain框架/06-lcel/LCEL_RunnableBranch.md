# 4.3 RunnableBranch（分支链）

> 所属章节：[15 - LCEL 与链式调用](15-LCEL与链式调用.md) › 第 4 节 › 4.3

---

如果顺序链解决的是"按顺序做"，那么 `RunnableBranch` 解决的就是：**根据输入内容，决定走哪条链。**

它很像编程里的 `if / elif / else`，只是现在被放进了 Runnable 体系中。典型结构是：

```python
RunnableBranch(
    (条件1, 链1),
    (条件2, 链2),
    默认链,
)
```

执行时会依次判断条件：

- 第一个命中的条件对应的链会被执行
- 如果都不命中，就走默认链

这在项目里非常常见，比如：

- 根据语言选择不同翻译链
- 根据用户意图选择不同客服流程
- 根据问题类型选择不同 Prompt 或模型

本章配套案例就是一个非常典型的"语言路由"例子：

- 输入里有"日语"关键词，走日语翻译链
- 输入里有"韩语"关键词，走韩语翻译链
- 否则默认走英语翻译链

【案例源码】`LCEL_RunnableBranchDemo.py`

[LCEL_RunnableBranchDemo.py](LCEL_RunnableBranchDemo.py)

这个案例和实际项目非常贴近，因为很多真实业务并不是"所有请求走同一条链"，而是"先判断，再分流"。

---

**相关章节：**

- 上一节：[4.2 RunnableSequence（顺序链）](LCEL_RunnableSequence.md)
- 下一节：[4.4 Multi-Step Chain（多步串行链）](LCEL_MultiStepChain.md)
