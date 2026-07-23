# 4.2 RunnableSequence（顺序链）

> 所属章节：[15 - LCEL 与链式调用](15-LCEL与链式调用.md) › 第 4 节 › 4.2

---

`RunnableSequence` 是最常见、也最重要的链类型之一。LangChain 官方参考里明确说明，它几乎出现在所有基础链式流程中。

它的核心规则很简单：**前一个 Runnable 的输出，直接作为后一个 Runnable 的输入。**

所以：

```python
chain = prompt | model | parser
```

本质上就是一个 `RunnableSequence`。

你也可以显式写成：

```python
from langchain_core.runnables import RunnableSequence

chain = RunnableSequence(first=prompt, middle=[model], last=parser)
```

但在实际开发中，更常见、更推荐的仍然是管道符写法，因为可读性更好。

这类链最典型的场景，就是把前面几章的核心内容一次性串起来：

- Prompt 组织输入
- Model 调用模型
- Parser 把输出转成字符串或结构化结果

【案例源码】`LCEL_RunnableSequenceDemo.py`

[LCEL_RunnableSequenceDemo.py](LCEL_RunnableSequenceDemo.py)

这个案例建议重点看一下，因为它不仅演示了"分步执行"，也演示了"直接把三步写成一条链再一次 invoke"。这正是 LCEL 的核心体验。

---

**相关章节：**

- 上一节：[4.1 几种链的选择与对比](15-LCEL与链式调用.md#41-几种链的选择与对比)
- 下一节：[4.3 RunnableBranch（分支链）](LCEL_RunnableBranch.md)
