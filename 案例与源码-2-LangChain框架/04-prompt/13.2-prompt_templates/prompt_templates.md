# 文本提示词模板（PromptTemplate）

> 对应教程：[第 13 章 - 提示词与消息模板](../13-提示词与消息模板.md) §5 §6

---

## 5、提示词模板概览

### 5.1 提示词简介

在真正的项目里，Prompt 几乎不可能永远写死在代码中。原因很现实：

- 用户问题会变
- 角色设定会变
- 输出要求会变
- 业务策略会调整
- 团队成员需要一起维护

如果每次都手写一整段 Prompt，不仅容易重复，还非常难维护。提示词模板的作用，就是把**固定部分沉淀下来，把变化部分改成变量**，从而让同一套提示逻辑可以反复复用。

这和 Python 的 `f-string` 很像：

```python
def hello(name: str) -> None:
    print(f"你好：{name}")

if __name__ == "__main__":
    hello("李四")
```

这里的 `{name}` 就像 Prompt 模板里的占位符，直接把它看成"先留坑，后填值"就行。

### 5.2 提示词模板类型

LangChain 中常见的提示词模板主要有下面几类，本课程重点掌握前两种即可：

| 类型                      | 说明                                         | 本课程定位 |
| ------------------------- | -------------------------------------------- | ---------- |
| **PromptTemplate**        | 面向纯文本模板，填值后通常得到一条字符串     | 重点掌握   |
| **ChatPromptTemplate**    | 面向聊天模型的多角色模板，填值后得到多条消息 | 重点掌握   |
| **FewShotPromptTemplate** | 把若干"示例输入-输出"嵌入提示词              | 了解即可   |
| **PipelinePrompt**        | 把多个子提示按顺序组合                       | 了解即可   |

入门阶段可以先作一个简化理解：

- **单条文本任务**，先看 `PromptTemplate`
- **聊天模型、多角色、多轮对话**，重点看 `ChatPromptTemplate`

### 5.3 Few-shot 模板

Few-shot 的意思是：不要只告诉模型"按什么规则做"，还给它几组"输入应该怎么变成输出"的示例。

它适合下面这类场景：

- 分类标签容易混，需要给几个标准样例
- 输出风格有要求，单靠文字说明不够稳
- 任务规则不复杂，但希望模型模仿固定格式

入门阶段不用急着背 `FewShotPromptTemplate` 的所有参数，先记住一句话：**Few-shot 是把示例变成提示词的一部分，让模型照着样子做。**

---

## 6、文本提示词模板（PromptTemplate）

### 6.1 简介

`PromptTemplate` 是 LangChain 中最基础的模板类，适合把一段文本 Prompt 做成"固定骨架 + 动态变量"的形式。

它最适合的场景是：

- 摘要、改写、翻译、分类等单轮文本任务
- 还不需要明确区分 system / user 角色
- 需要频繁替换少量变量

**PromptTemplate 的结果通常还是字符串，只不过这条字符串不再靠手写，而是由模板生成。**

### 6.2 常用参数

| 参数                | 说明                                     |
| ------------------- | ---------------------------------------- |
| `template`          | 模板字符串，内部可包含 `{变量名}` 占位符 |
| `input_variables`   | 调用时需要传入的变量名列表               |
| `partial_variables` | 在模板创建阶段就预先固定的一部分变量     |

其中，`partial_variables` 尤其值得理解。它的作用很直接：

**把那些"经常不变"的变量先固定住，后续每次只传真正会变化的部分。**

典型例子：

- 系统角色长期固定为"Python 工程师"
- 但用户问题每次都不同

这样一来，你就不用每次都重复传 `role="Python 工程师"`。

### 6.3 常用方法

| 方法            | 返回值                | 适合场景                                     |
| --------------- | --------------------- | -------------------------------------------- |
| `format(...)`   | `str`                 | 最常用，拿到字符串后直接给模型或自己继续拼接 |
| `invoke({...})` | `PromptValue`         | 需要接入 LangChain 链时更自然                |
| `partial(...)`  | 新的 `PromptTemplate` | 先固定部分变量，再多次复用                   |

```python
from langchain_core.prompts import PromptTemplate

template = PromptTemplate.from_template(
    "你是一个专业的{role}工程师，请回答我的问题，我的问题是：{question}"
)

# 1）format：得到 str
prompt_str = template.format(role="python开发", question="二分查找怎么写？")

# 2）invoke：得到 PromptValue
prompt_value = template.invoke({"role": "python开发", "question": "冒泡排序怎么写？"})
prompt_value.to_string()
prompt_value.to_messages()

# 3）partial：固定 role，得到新模板
new_template = template.partial(role="python开发")
prompt_str = new_template.format(question="快速排序怎么写？")
```

对初学者的实用建议是：

- **刚入门时优先用 `format`**
- **做 LCEL 或链式调用时再逐渐理解 `invoke`**
- **同一模板长期复用时再考虑 `partial`**

【案例源码】[13.2.1-method/PromptTemplate_FormatMethod.py](13.2.1-method/PromptTemplate_FormatMethod.py) | [13.2.1-method/PromptTemplate_InvokeMethod.py](13.2.1-method/PromptTemplate_InvokeMethod.py) | [13.2.1-method/PromptTemplate_PartialMethod.py](13.2.1-method/PromptTemplate_PartialMethod.py)

### 6.4 创建方式

`PromptTemplate` 常见有两种创建方式：

- 构造函数：手动指定 `template` 和 `input_variables`
- `from_template(...)`：由 LangChain 自动推断变量名

```python
from langchain_core.prompts import PromptTemplate

# 方式一：构造函数
template = PromptTemplate(
    template="你是一个专业的{role}工程师，请回答：{question}",
    input_variables=["role", "question"]
)
prompt = template.format(role="python开发", question="快速排序怎么写？")

# 方式二：from_template
template = PromptTemplate.from_template("请给我一个关于{topic}的{type}解释。")
prompt = template.format(topic="量子力学", type="详细")
```

从经验上看，可按下面的方式选择：

- **模板简单**：优先 `from_template(...)`
- **你想显式表达变量名**：用构造函数

【案例源码】[PromptTemplate_Constructor.py](PromptTemplate_Constructor.py) | [PromptTemplate_FromTemplate.py](PromptTemplate_FromTemplate.py)

除了创建方式，本章还保留了两个补充案例。

**第一，组合多个模板。**  
当一个 Prompt 由多个子部分拼起来时，可以通过 `+` 组合模板，而不是手写超长字符串。真实项目里，这在"角色说明 + 业务规则 + 当前任务"这种分段组织里很有用。

【案例源码】[PromptTemplate_Combined.py](PromptTemplate_Combined.py)

**第二，比较 `partial_variables` 与 `partial()`。**  
二者都能做到"先固定一部分变量，后续只传剩余变量"，但一个发生在模板创建阶段，一个发生在已有模板基础上。

【案例源码】[PromptTemplate_PartialVariables.py](PromptTemplate_PartialVariables.py)
