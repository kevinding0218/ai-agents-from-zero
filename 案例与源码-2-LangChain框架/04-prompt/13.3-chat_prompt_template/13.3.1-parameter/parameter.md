# ChatPromptTemplate 入参写法对比

> 对应教程：[第 13 章 - 提示词与消息模板](../../13-提示词与消息模板.md) §7.2
> 上级说明：[chat_prompt_template.md](../chat_prompt_template.md)

---

`ChatPromptTemplate.from_messages([...])` 里的每一条消息，可以用三种写法表达，效果完全等价，只是风格不同。

| 写法 | 示例 | 适合场景 |
| --- | --- | --- |
| **元组** | `("system", "你是{name}")` | 最简洁，教学和业务代码里都很常见 |
| **字典** | `{"role": "system", "content": "你是{name}"}` | 贴近 OpenAI 风格 JSON |
| **Message 类** | `SystemMessage(content="你是{name}")` | 最显式，角色最清楚，适合复杂场景 |

## 三种写法对比示例

```python
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_core.prompts import ChatPromptTemplate

# 元组写法（最常用）
prompt_tuple = ChatPromptTemplate.from_messages([
    ("system", "你是助手，名字叫{name}。"),
    ("human", "{question}")
])

# 字典写法（OpenAI 风格）
prompt_dict = ChatPromptTemplate.from_messages([
    {"role": "system", "content": "你是助手，名字叫{name}。"},
    {"role": "user", "content": "{question}"}
])

# Message 类写法（最显式）
prompt_msg = ChatPromptTemplate.from_messages([
    SystemMessage(content="你是助手，名字叫{name}。"),
    HumanMessage(content="{question}")
])
```

三者 `format_messages(name="小问", question="你好")` 的结果完全相同。

## 本目录文件

| 文件 | 演示内容 |
| --- | --- |
| [ChatPromptTemplate_TupleParam.py](ChatPromptTemplate_TupleParam.py) | 元组写法 `("role", "content")` |
| [ChatPromptTemplate_DictParam.py](ChatPromptTemplate_DictParam.py) | 字典写法 `{"role": ..., "content": ...}` |
| [ChatPromptTemplate_MessageParam.py](ChatPromptTemplate_MessageParam.py) | Message 类写法 `SystemMessage(...)` |
