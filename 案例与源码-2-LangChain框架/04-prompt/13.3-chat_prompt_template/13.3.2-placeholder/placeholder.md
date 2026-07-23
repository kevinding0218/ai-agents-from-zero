# MessagesPlaceholder：消息占位符

> 对应教程：[第 13 章 - 提示词与消息模板](../../13-提示词与消息模板.md) §7.5
> 上级说明：[chat_prompt_template.md](../chat_prompt_template.md)

---

## 核心概念

**模型本身没有记忆。** 每次调用都是全新的——你不把历史传进去，它就不知道之前说过什么。

`MessagesPlaceholder` 的作用：在模板里预留一个"槽位"，调用时把上一轮的对话历史整块塞进去，让模型"假装记得"之前的内容。

```
没有 memory → 发给模型：System + "我叫什么？"                          → 不知道
有  memory  → 发给模型：System + [我叫亮仔] + [好的亮仔] + "我叫什么？" → 亮仔
```

## 两种写法（效果完全相同）

```python
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

# 显式写法（推荐学习阶段使用，意图更清晰）
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个助手"),
    MessagesPlaceholder("memory"),       # ← 这里留一个槽位
    ("human", "{question}"),
])

# 隐式写法（元组简写，不需要额外 import）
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个助手"),
    ("placeholder", "{memory}"),         # ← 等价写法
    ("human", "{question}"),
])
```

调用时传入历史消息列表：

```python
from langchain_core.messages import HumanMessage, AIMessage

prompt.invoke({
    "memory": [
        HumanMessage("我叫亮仔"),
        AIMessage("好的，亮仔你好"),
    ],
    "question": "我叫什么？"
})
```

## 本目录文件

| 文件 | 演示内容 |
| --- | --- |
| [ChatPromptTemplate_ExplicitPlaceholder.py](ChatPromptTemplate_ExplicitPlaceholder.py) | 显式写法 `MessagesPlaceholder("memory")` |
| [ChatPromptTemplate_ImplicitPlaceholder.py](ChatPromptTemplate_ImplicitPlaceholder.py) | 隐式写法 `("placeholder", "{memory}")` |
| [ChatPromptTemplate_MemoryDemo.py](ChatPromptTemplate_MemoryDemo.py) | **有无 memory 的真实对比**：实际调用模型，直观看到差别 |
