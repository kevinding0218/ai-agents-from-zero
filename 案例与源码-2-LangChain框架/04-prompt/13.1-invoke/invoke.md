# 调用大模型的入参与调用方式

> 对应教程：[第 13 章 - 提示词与消息模板](../13-提示词与消息模板.md) §2 §3 §4

---

## 2、调用大模型的入参类型

初学者容易误以为：调用聊天模型时，输入永远只能是"一段字符串"。实际上，LangChain 聊天模型为了适配真实对话场景，通常支持多种输入形态。这些写法表面不同，核心都一样：**把角色、任务、上下文、输入和约束，以合适的消息结构交给模型。**

### 2.1 入参形态总览

同一次 `invoke`，左侧可以是多种类型的输入，中间由聊天模型处理，右侧典型返回值是 `AIMessage`。这也是为什么你在第 11 章会经常看到"正文一般通过 `.content` 读取"。

![聊天模型 invoke：常见入参类型与 AIMessage 输出](../../../images/13/13-2-1-1.png)

常见对应关系如下：

| 入参形态                                     | 调用时大致长什么样                                      | 适合场景                           |
| -------------------------------------------- | ------------------------------------------------------- | ---------------------------------- |
| `str`                                        | `model.invoke("请解释什么是 LangChain")`                | 单轮、轻量、快速试接口             |
| `PromptTemplate.format(...)` 后的字符串      | `model.invoke(prompt_str)`                              | 固定句式 + 少量变量替换            |
| 消息对象列表                                 | `model.invoke([SystemMessage(...), HumanMessage(...)])` | 推荐写法，适合系统提示、多轮对话   |
| `(role, content)` 元组列表                   | `[("system", "..."), ("user", "...")]`                  | 简洁、贴近 ChatPromptTemplate 风格 |
| `{"role": "...", "content": "..."}` 字典列表 | `[{"role":"system","content":"..."}, ...]`              | 贴近 OpenAI 风格 JSON 数据         |

【案例源码】[LLM_Invoke_InputTypes.py](LLM_Invoke_InputTypes.py)

### 2.2 写法一：纯字符串

最简单的输入方式，把整段说明写在一个字符串里交给模型，适合快速试接口、或单轮一句话任务。

```python
resp = model.invoke("用一句话解释什么是 LangChain")
print(resp.content)
```

不过在真实项目里，纯字符串也有明显局限：不方便表达系统角色与用户问题的边界；不方便插入历史对话；不利于后期维护和多人协作。

所以，**纯字符串适合起步，不适合复杂对话场景长期使用。**

### 2.3 写法二：模板 + 占位符

如果一句 Prompt 里只有少数变量会变化，就没必要每次手写整段长文本。更合理的做法，是把固定部分写成模板，把变化部分留成占位符。

这一节本质上仍然是"字符串输入"，只是我们把字符串的生成过程工程化了。

```python
from langchain_core.prompts import PromptTemplate

template = PromptTemplate.from_template(
    "用不超过 50 字介绍：{topic} 是什么？"
)
prompt_str = template.format(topic="LangChain")
resp = model.invoke(prompt_str)
print(resp.content)
```

也就是说：

- **`PromptTemplate` 负责生产输入**
- **`model.invoke(...)` 负责把输入发给模型**

它和 2.2 的区别不在于模型收到了不同类型，而在于**我们是手写字符串，还是用模板生成字符串**。

### 2.4 写法三：多角色消息列表

当你开始做聊天机器人、问答助手、企业知识库、代码助手时，最推荐的入参方式通常不是字符串，而是**消息列表**。

原因很简单：聊天模型更擅长理解"谁在说话"。把输入拆成 `SystemMessage`、`HumanMessage`、`AIMessage` 等不同角色，模型更容易正确理解上下文结构，而不是把所有东西都当成一大段平铺文本。

```python
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage

messages = [
    SystemMessage(content="你是只回答技术问题的助手，回答要简短。"),
    HumanMessage(content="什么是 LangChain？"),
    # 多轮示例：
    # AIMessage(content="LangChain 是用于编排 LLM 应用的框架……"),
    # HumanMessage(content="它和直接调 API 有什么区别？"),
]
resp = model.invoke(messages)
print(resp.content)
```

在实际项目里，这种写法特别常见：

- **系统提示词**放在 `SystemMessage`
- **用户问题**放在 `HumanMessage`
- **历史回复**可放回 `AIMessage`
- **工具执行结果**后续可用 `ToolMessage`

如果你后面要做多轮对话、Agent、RAG，这种消息列表思维会反复用到。

### 2.5 写法四：元组列表与字典列表

除了显式使用 `SystemMessage`、`HumanMessage` 等类，LangChain 聊天模型通常还支持两种常见简写。

- **元组列表**：每项写成 `(role, content)`
- **字典列表**：每项写成 `{"role": "...", "content": "..."}`

这两种写法与消息对象列表在语义上基本等价，只是更接近手写列表或 OpenAI 风格的数据结构。

关于选择策略，可按下面的思路理解：

- **想学得最清楚**：优先用 `Message` 类
- **想写得最简洁**：可以用元组列表
- **想和 OpenAI 风格请求体对齐**：可以用字典列表

无论哪种方式，同步 `invoke` 的返回值通常仍是 `AIMessage`。

---

## 3、入参的消息类型

当你把输入组织成"消息列表"之后，就需要知道每种消息类型代表什么。在 LangChain 官方语境里，最核心的几类消息是：**SystemMessage**、**HumanMessage**、**AIMessage**、**ToolMessage**。

**文档**：https://docs.langchain.com/oss/python/langchain/messages

### 3.1 四类核心消息

| 类型            | 说明                                             | 项目里最常见的用途                  |
| --------------- | ------------------------------------------------ | ----------------------------------- |
| `SystemMessage` | 系统消息，通常用于规定角色、风格、边界、输出格式 | 设定人设、回答规则、拒答策略        |
| `HumanMessage`  | 用户消息，对应用户当前输入                       | 放用户问题、补充条件、后续追问      |
| `AIMessage`     | 模型回复消息                                     | 保存上一轮回复，支持多轮上下文      |
| `ToolMessage`   | 工具执行结果消息                                 | 把外部工具/函数的返回结果回传给模型 |

需要注意两个细节：

1. **LangChain 的类名叫 `HumanMessage`，但很多平台的角色字段写的是 `user`。** 这不是冲突，而是不同层的命名习惯。
2. **旧版本资料里可能会看到 `FunctionMessage`。** 在 LangChain 1.x 语境下，更常见的是 `ToolMessage`。

### 3.2 SystemMessage 的作用

很多新手会把系统提示词和用户问题混在一段字符串里写，这样虽然也能跑，但可维护性很差。更稳妥的做法是把"规则"和"问题"拆开：

- **SystemMessage** 负责定义系统层规则
- **HumanMessage** 负责承载当前用户问题

例如：

- "你是一个法律助手，只回答法律问题"
- "输出请控制在 80 字内"
- "如果超出范围，请明确拒答"

这些内容更适合放在 `SystemMessage` 里，因为它们属于"长期规则"，而不是某一轮具体问题。

**系统提示词是行为边界，用户提示词是当前任务。**

### 3.3 ToolMessage 什么时候会出现

`ToolMessage` 不会在普通问答里频繁出现，它更常见于函数调用、工具调用、Agent 编排场景。

简单理解就是：

- 模型先在 `AIMessage` 里表达"我想调用某个工具"
- 代码真的去执行工具
- 工具结果再以 `ToolMessage` 的形式回传给模型

```python
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage, ToolMessage

messages = [
    SystemMessage(content="你是一位乐于助人的智能小助手"),
    HumanMessage(content="你好，请你介绍一下你自己"),
    AIMessage(content="我是一名人工智能助手，请问您有什么想问的吗？"),
    ToolMessage(
        content='{"population": 21540000, "area": "16410平方公里"}',
        tool_call_id="call_abc123",
    ),
]
```

**最常用的是 System / Human / AI 三类；ToolMessage 是后续 Agent、工具调用章节的重要铺垫。**

【案例源码】[LLM_Invoke_ToolMessage.py](LLM_Invoke_ToolMessage.py)

---

## 4、调用大模型的调用方式

当输入组织好之后，下一步就是把它交给模型。LangChain 聊天模型常见的调用方式有四类：

- **invoke / ainvoke**：一次发一条
- **stream / astream**：一边生成一边返回
- **batch / abatch**：一次发很多条

### 4.1 普通调用（invoke / ainvoke）

- `invoke`：同步调用，最常用，适合单轮问答与脚本演示。
- `ainvoke`：异步调用，适合异步 Web 服务、并发任务和高吞吐场景。

```python
import os
from langchain.chat_models import init_chat_model
from langchain_core.messages import HumanMessage, SystemMessage

model = init_chat_model(
    model="qwen-plus",
    model_provider="openai",
    api_key=os.getenv("aliQwen-api"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
)
messages = [
    SystemMessage(content="你是一个法律助手，只回答法律问题，超出范围回答：非法律问题无可奉告"),
    HumanMessage(content="简单介绍下广告法，一句话 50 字以内")
]
response = model.invoke(messages)
print(type(response))
print(response.content)
```

实际项目里怎么选：

- **命令行脚本、教学示例、简单后台任务**：优先 `invoke`
- **FastAPI、异步服务、并发请求**：优先 `ainvoke`

【案例源码】[LLM_Invoke.py](LLM_Invoke.py) | [LLM_aInvoke.py](LLM_aInvoke.py)

并发对比演示：[LLM_aInvoke_ConcurrentDemo.py](LLM_aInvoke_ConcurrentDemo.py)

### 4.2 流式调用（stream / astream）

- **stream**：同步流式输出
- **astream**：异步流式输出

流式的最大价值不是"更快算完"，而是**更快把正在生成的内容展示给用户**。在聊天机器人、报告生成、代码生成等场景里，用户体验会明显更好。

```python
messages = [
    SystemMessage(content="你叫小问，是一个乐于助人的AI助手"),
    HumanMessage(content="你是谁")
]
for chunk in model.stream(messages):
    print(chunk.content, end="", flush=True)
print()
```

真实项目里，`stream` / `astream` 很常用于：

- 聊天界面的"打字机效果"
- 长回答提前回显
- 减少用户等待焦虑

【案例源码】[LLM_Stream.py](LLM_Stream.py) | [LLM_aStream.py](LLM_aStream.py)

### 4.3 批处理（batch / abatch）

- **batch**：一次提交多条输入，统一获得多条结果
- **abatch**：异步批处理

它特别适合离线任务，而不是交互式聊天。例如：

- 批量摘要一批文档
- 批量清洗问答数据
- 批量评估 Prompt 效果

```python
questions = [
    "什么是 Redis？简洁 100 字以内",
    "Python 的生成器是做什么的？简洁 100 字以内",
]
response = model.batch(questions)
for q, r in zip(questions, response):
    print(f"问题：{q}\n回答：{r.content}\n")
```

> **注意（本地模型）：** 使用 Ollama 等本地模型时，`batch` 并发可能比顺序 `invoke` 更慢，因为多条请求共享同一块 GPU。远程 API 每条请求打到独立服务器，并发才有明显加速。详见 [LLM_Batch_output.txt](LLM_Batch_output.txt)。

【案例源码】[LLM_Batch.py](LLM_Batch.py) | [LLM_aBatch.py](LLM_aBatch.py)

### 4.4 小结

| 场景     | 同步     | 异步      | 适合什么情况        |
| -------- | -------- | --------- | ------------------- |
| 单条调用 | `invoke` | `ainvoke` | 单轮问答、接口服务  |
| 流式输出 | `stream` | `astream` | 聊天 UI、长文本生成 |
| 批量处理 | `batch`  | `abatch`  | 离线任务、批量评估  |

**先会 `invoke`，再学 `stream`，最后再补 `batch` 和异步版本。**
