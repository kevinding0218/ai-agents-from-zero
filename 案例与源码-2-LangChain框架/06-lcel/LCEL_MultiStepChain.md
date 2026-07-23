# 4.4 Multi-Step Chain（多步串行链）

> 所属章节：[15 - LCEL 与链式调用](15-LCEL与链式调用.md) › 第 4 节 › 4.4

---

这一节要学的重点是：**如何把多条子链首尾串起来，形成多步串行流程。**

比如这个案例做的事是：

1. 先让模型用中文介绍某个主题
2. 再把第一步结果转换成第二步所需的输入结构
3. 最后再交给另一条链翻译成英文

也就是一种非常典型的"多步加工"：

- 第一步不是最终结果
- 第二步依赖第一步结果
- 中间还可能需要做一次结构映射

这类串行链在项目里特别常见，比如：

- 先摘要，再翻译
- 先提取要点，再生成报告
- 先检索，再重写，再结构化

【案例源码】`LCEL_MultiStepChainDemo.py`

[LCEL_MultiStepChainDemo.py](LCEL_MultiStepChainDemo.py)

这个案例里最值得你注意的点，不是文件名，而是中间这个映射动作：

```python
(lambda content: {"input": content})
```

它说明一个很常见的现实问题：**前后两步的输入输出结构，不一定天然匹配。** 这也是为什么函数节点和 RunnableLambda 在 LCEL 中很实用。

---

**相关章节：**

- 上一节：[4.3 RunnableBranch（分支链）](LCEL_RunnableBranch.md)
- 下一节：[4.5 RunnableParallel（并行链）](LCEL_RunnableParallel.md)
