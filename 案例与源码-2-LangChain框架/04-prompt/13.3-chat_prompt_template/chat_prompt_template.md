# 对话提示词模板（ChatPromptTemplate）

> 对应教程：[第 13 章 - 提示词与消息模板](../13-提示词与消息模板.md) §7

---

## 7、对话提示词模板（ChatPromptTemplate）

### 7.1 简介

如果说 `PromptTemplate` 更适合"单条文本输入"，那么 `ChatPromptTemplate` 就是为"聊天模型场景"准备的模板类。

它比 `PromptTemplate` 更贴合真实项目，因为真实聊天应用往往不只是"一句话"，而是：

- 一条系统设定
- 一条或多条用户消息
- 可能还有历史 AI 回复
- 可能再插入工具结果或历史上下文

这时，把所有内容写成一大段纯文本虽然也能跑，但不如把它们拆成带角色的消息来得清晰。

所以你可以把 `ChatPromptTemplate` 简单理解成：**"面向多角色消息的模板系统"。**

### 7.2 常用参数

`ChatPromptTemplate` 的核心不是单个 `template` 字符串，而是一组"消息模板"。每一项都可以是下面这些形式：

| 类型                  | 说明                                          |
| --------------------- | --------------------------------------------- |
| 元组                  | `("system", "你是{name}")`                    |
| 字典                  | `{"role": "system", "content": "你是{name}"}` |
| Message 类            | `SystemMessage(content="你是{name}")`         |
| `MessagesPlaceholder` | 在模板中预留一段"消息列表占位"，见 §7.5       |

三种常规写法里，没有绝对的谁对谁错，可以按下面的经验来选：

- **元组**：最简洁，教学和业务代码里都很常见
- **字典**：更贴近 OpenAI 风格 JSON，方便和网关数据结构对齐
- **Message 类**：最显式，角色最清楚，适合教学和复杂场景

```python
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_core.prompts import ChatPromptTemplate

prompt1 = ChatPromptTemplate.from_messages([
    ("system", "你是助手，名字叫{name}。"),
    ("human", "{question}")
])

prompt2 = ChatPromptTemplate.from_messages([
    {"role": "system", "content": "你是助手，名字叫{name}。"},
    {"role": "user", "content": "{question}"}
])

prompt3 = ChatPromptTemplate.from_messages([
    SystemMessage(content="你是助手，名字叫{name}。"),
    HumanMessage(content="{question}")
])
```

【案例源码】[13.3.1-parameter/ChatPromptTemplate_TupleParam.py](13.3.1-parameter/ChatPromptTemplate_TupleParam.py) | [13.3.1-parameter/ChatPromptTemplate_DictParam.py](13.3.1-parameter/ChatPromptTemplate_DictParam.py) | [13.3.1-parameter/ChatPromptTemplate_MessageParam.py](13.3.1-parameter/ChatPromptTemplate_MessageParam.py)

### 7.3 常用方法

| 方法                   | 返回值              | 使用建议                                    |
| ---------------------- | ------------------- | ------------------------------------------- |
| `format_messages(...)` | `List[BaseMessage]` | 最直观，得到消息列表后交给模型              |
| `invoke({...})`        | `ChatPromptValue`   | 适合与 LangChain 链条衔接，也可直接交给模型 |
| `format(...)`          | `str`               | 适合查看最终拼接效果，不推荐作为聊天主写法  |

这三个方法最容易混淆，建议这样理解：

- `format_messages`：我要的是"消息列表"
- `invoke`：我要的是"PromptValue 对象"
- `format`：我要的是"纯字符串"

```python
from langchain_core.prompts import ChatPromptTemplate

chat_prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个{role}，请回答我提出的问题"),
    ("human", "请回答：{question}")
])

# 方式一：得到消息列表
messages = chat_prompt.format_messages(
    role="python开发工程师",
    question="堆排序怎么写"
)
result = model.invoke(messages)

# 方式二：得到 ChatPromptValue
prompt_value = chat_prompt.invoke({
    "role": "python开发工程师",
    "question": "快速排序怎么写"
})
result = model.invoke(prompt_value)

# 方式三：得到纯字符串
prompt_str = chat_prompt.format(
    role="python开发工程师",
    question="快速排序怎么写"
)
print(prompt_str)
```

对实际项目来说，建议优先采用这两种：

- `format_messages(...) -> model.invoke(messages)`
- `invoke({...}) -> model.invoke(prompt_value)`

因为这两种方式都能保留清晰的消息角色结构。

【案例源码】[ChatPromptTemplate_FormatMessages.py](ChatPromptTemplate_FormatMessages.py)

### 7.4 创建方式

`ChatPromptTemplate` 常见也有两种创建方式：

- `ChatPromptTemplate.from_messages([...])`
- `ChatPromptTemplate([...])`

```python
from langchain_core.prompts import ChatPromptTemplate

messages = [
    ("system", "你是一个{role}，请回答我提出的问题"),
    ("human", "请回答：{question}")
]

chat_prompt1 = ChatPromptTemplate.from_messages(messages)
chat_prompt2 = ChatPromptTemplate(messages)

print(chat_prompt1.format_messages(role="python开发工程师", question="堆排序怎么写"))
print(chat_prompt2.format_messages(role="python开发工程师", question="堆排序怎么写"))
```

经验上更推荐优先使用 `from_messages(...)`，因为可读性更好，也更符合官方文档和社区示例的主流写法。

【案例源码】[ChatPromptTemplate_Constructor.py](ChatPromptTemplate_Constructor.py)

### 7.5 MessagesPlaceholder

MessagesPlaceholder（消息占位符）这是本章最重要的知识点之一，很多人一开始会觉得它抽象，但一旦理解，就会发现它几乎是多轮对话、记忆、历史上下文拼接的关键。

**先说结论：`MessagesPlaceholder` 的作用，就是在模板里先留出一段"消息列表的位置"，等真正调用时再把历史对话整块塞进去。**

![MessagesPlaceholder 将历史消息动态插入 ChatPromptTemplate：模板先留占位，运行时再填入多轮消息](../../../images/13/13-7-5-1.svg)

为什么它重要？因为真实项目里的"历史对话"往往不是固定写死的：

- 有时只有 2 轮
- 有时有 10 轮
- 有时还要先裁剪、总结、过滤

如果没有占位符，你就只能在代码里手动拼接 `HumanMessage`、`AIMessage`，又乱又难维护。

它常见有两种写法：

- **显式写法**：`MessagesPlaceholder("memory")`
- **隐式写法**：`("placeholder", "{memory}")`

二者的核心思想完全一样，只是语法风格不同。

典型结构通常是：

- 系统设定
- 历史消息占位
- 当前用户问题

```python
from langchain_core.messages import HumanMessage, AIMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个资深的Python应用开发工程师，请认真回答我提出的Python相关的问题"),
    MessagesPlaceholder("memory"),
    ("human", "{question}")
])

prompt_value = prompt.invoke({
    "memory": [
        HumanMessage(content="我的名字叫亮仔，是一名程序员"),
        AIMessage(content="好的，亮仔你好")
    ],
    "question": "请问我的名字叫什么？"
})

print(prompt_value.to_string())
```

这段代码的价值在于，它让模板本身变得非常稳定，而把"历史有几轮、具体内容是什么"延迟到运行时再决定。后面学第 16 章 记忆与对话历史时，你会频繁看到这种模式。

> **关键理解：模型本身没有记忆。** 每次调用都是全新的。`memory` 变量名只是你传进去的历史消息列表——没有传，模型就不知道之前说了什么；传了，模型才"假装记得"。  
> 详见对比演示：[13.3.2-placeholder/ChatPromptTemplate_MemoryDemo.py](13.3.2-placeholder/ChatPromptTemplate_MemoryDemo.py)

【案例源码】  
显式：[13.3.2-placeholder/ChatPromptTemplate_ExplicitPlaceholder.py](13.3.2-placeholder/ChatPromptTemplate_ExplicitPlaceholder.py)  
隐式：[13.3.2-placeholder/ChatPromptTemplate_ImplicitPlaceholder.py](13.3.2-placeholder/ChatPromptTemplate_ImplicitPlaceholder.py)  
真实对比演示：[13.3.2-placeholder/ChatPromptTemplate_MemoryDemo.py](13.3.2-placeholder/ChatPromptTemplate_MemoryDemo.py)
