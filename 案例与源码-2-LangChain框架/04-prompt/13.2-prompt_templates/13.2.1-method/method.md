# PromptTemplate 常用方法

> 对应教程：[第 13 章 - 提示词与消息模板](../../13-提示词与消息模板.md) §6.3
> 上级说明：[prompt_templates.md](../prompt_templates.md)

---

同一个 `PromptTemplate` 对象，可以用三种方式"用出来"，返回值类型不同，适合不同场景。

| 方法 | 返回值 | 适合场景 |
| --- | --- | --- |
| `format(...)` | `str`（普通字符串） | 最常用，拿到字符串后直接传给模型 |
| `invoke({...})` | `PromptValue` 对象 | 接入 LCEL 链式调用时更自然 |
| `partial(...)` | 新的 `PromptTemplate` | 先固定部分变量，后续只传剩余变量 |

## 用法示例

```python
from langchain_core.prompts import PromptTemplate

template = PromptTemplate.from_template(
    "你是一个专业的{role}工程师，请回答：{question}"
)

# format → str（最常用）
prompt_str = template.format(role="Python开发", question="二分查找怎么写？")
# result: "你是一个专业的Python开发工程师，请回答：二分查找怎么写？"

# invoke → PromptValue（LCEL 链里用）
prompt_value = template.invoke({"role": "Python开发", "question": "冒泡排序怎么写？"})
prompt_value.to_string()    # 转成字符串
prompt_value.to_messages()  # 转成消息列表

# partial → 新模板（固定不变的变量）
python_template = template.partial(role="Python开发")
prompt_str = python_template.format(question="快速排序怎么写？")
# 不用每次都传 role 了
```

## 选择建议

- **刚入门** → 优先用 `format`，简单直接
- **做 LCEL 链式调用** → 用 `invoke`，返回的 `PromptValue` 可以直接接下一个节点
- **同一模板长期复用、只有一个变量经常变** → 用 `partial` 固定不变的部分

## 本目录文件

| 文件 | 演示内容 |
| --- | --- |
| [PromptTemplate_FormatMethod.py](PromptTemplate_FormatMethod.py) | `format(...)` → 返回 `str` |
| [PromptTemplate_InvokeMethod.py](PromptTemplate_InvokeMethod.py) | `invoke({...})` → 返回 `PromptValue` |
| [PromptTemplate_PartialMethod.py](PromptTemplate_PartialMethod.py) | `partial(...)` → 返回固定了部分变量的新模板 |
