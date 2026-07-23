# 15 - LCEL 与链式调用

---

**本章课程目标：**

- 理解 **Runnable** 的定位，知道 LangChain 为什么要把 Prompt、Model、Parser、Tool、Chain 都抽象成统一的“可执行组件”。
- 理解 **LCEL**（LangChain Expression Language，LangChain 表达式语言）到底解决了什么问题，掌握 `|` 管道符背后的链式组合思想。
- 能读懂并运行本章全部案例：**顺序链、分支链、多步串行链、并行链、函数链**，建立后续学习 [记忆与对话历史](16-记忆与对话历史（含Redis基础）.md)、[Tools 工具调用](17-Tools工具调用.md)、[RAG](19-RAG检索增强生成.md)、[Agent](21-Agent智能体.md) 的基础。

**学习建议：** 先亲手跑通 `prompt | model | parser`，再谈分支、并行和自定义函数节点。读代码时盯住两件事：每个 Runnable 收什么、吐什么；组合之后还能不能继续被 `invoke`、`stream`、`batch` 调用。这样看 LCEL，就不会只把它当成一个管道符语法。

---

## 1、Runnable 与统一调用方式

### 1.1 前置知识点：抽象基类（ABC）

如果你第一次接触 **抽象基类**，不用把它想得太复杂。对这一章来说，你只需要先建立一个足够实用的理解：

**抽象基类，就是先规定“这一类对象应该具备哪些共同能力”，但不急着规定“每个对象内部具体怎么做”。**

这里说的就是一种“统一规则”或“公共协议”。

比如框架设计者先约定：

- 只要你属于“可执行组件”
- 你就应该支持某些统一方法
- 例如单次调用、批量调用、流式调用、异步调用

这样后面无论这个对象到底是 Prompt、Model、Parser，还是整条 Chain，使用者都可以按同一种方式去理解和调用它。这就是抽象基类最有价值的地方：**先把共同规则定下来，再让不同对象去实现自己的具体行为。**

放到 LangChain 里，你可以把 **Runnable** 理解成这样一种核心抽象接口。它背后的思想就是：

- Prompt 是一种可执行组件
- Model 是一种可执行组件
- Parser 是一种可执行组件
- Chain 也是一种可执行组件

既然它们都属于“可执行组件”这一大类，那么就应该尽量遵守同一套调用协议。所以这一节你不用死记 `ABC`、`abstractmethod` 这些 Python 细节，只要先抓住一句话：**抽象基类解决的是“先把共同规则定下来”，而 Runnable 正是 LangChain 用来统一这些规则的关键抽象。**

![`langchain_core.runnables.base` 中 Runnable 的类定义与职责说明：可 invoke、batch、stream 并支持组合](../../images/15/15-1-1-1.jpeg)

> **图意说明**：上图截自 LangChain 参考文档。`Runnable` 声明为 `class Runnable(ABC, Generic[Input, Output])`，描述的是“可被调用、批量处理、流式输出、变换与组合”的工作单元；`invoke`/`ainvoke`、`batch`/`abatch`、`stream`/`astream` 等成对出现，`astream_log` 还可流式透出部分中间结果。各方法均可传入 `config`（如标签、元数据）便于追踪与排障；输入/输出/config 的结构信息可通过 `input_schema`、`output_schema`、`config_schema` 等暴露给工具链与 IDE。

### 1.2 统一接口的意义

如果没有统一接口，Prompt、模型、解析器、工具、检索器往往会各自一套入口（例如 `format`、`generate`、`parse`、`run`、`stream` 等混用）。结果是：**组件难拼接、方法名难记、替换实现时调用处到处要改**，也很难把整条流程写成清晰的“从左到右”数据流。

LangChain 的解决思路就是：**把这些“能接收输入并产生输出”的对象，尽量抽象成统一接口。**这个统一接口，就是 **Runnable**。

### 1.3 定义

**Runnable** 是 LangChain 中最核心的抽象之一，本质上就是一个“可执行的数据处理节点”接口。

这里要特别区分一个很容易混淆的点：

- **Runnable 是什么**：它表示“这一类对象是可执行组件”，是一种统一抽象标准。
- **统一调用方式是什么**：它是 Runnable 带来的结果，也就是这些组件都可以尽量用同一套方式去调用，比如 `invoke`、`batch`、`stream`。

所以更准确的结论是，**Runnable 不是“统一调用方式”的翻译，而是“统一调用方式”背后的抽象基础。**

只要某个对象实现了 Runnable 接口，它通常就能用统一方式来调用，比如： `invoke`、`batch`、`stream`、`ainvoke`、`abatch`、`astream`。

在 LangChain 里，很多你已经学过的对象，本质上都属于 Runnable：

- [Prompt 模板](13-提示词与消息模板.md)、[聊天模型](11-Model-I-O与模型接入.md)、[输出解析器](14-输出解析器.md)
- 后面会学到的检索器、工具、甚至整条链
- 更进一步，编译后的 LangGraph 图本身也可以作为 Runnable 被调用

也就是说，后面之所以能把这些组件顺畅地串成一条链，并不是因为“管道符很神奇”，而是：**这些组件本身都被设计成了可组合的 Runnable。**

### 1.4 统一调用方式

这一节先解决一个问题：后面“链式组合”为什么能成立、为什么会好用。

先看你已经学过的三个典型对象：

**1. Prompt 模板**

- 从业务角度看，Prompt 模板最常见的方法是 `format(...)`
- 但从“统一接口、便于后续组合”的角度看，它也可以用 `invoke(...)`

```python
# 业务层面常见写法
prompt_str = prompt.format(topic="LangChain")

# LCEL 统一接口写法
prompt_value = prompt.invoke({"topic": "LangChain"})
```

**2. 模型**

- 从业务角度看，模型最常见的是 `invoke(...)`
- 它接收上一步 Prompt 的输出，再返回 `AIMessage`

```python
ai_message = model.invoke(prompt_value)
```

**3. 解析器**

- 从业务角度看，解析器常见有 `parse(...)`
- 但在 LCEL 里也可以统一通过 `invoke(...)` 接收上一步结果

```python
result = parser.invoke(ai_message)
```

这样一来，三步就被统一成了同一种风格：

```python
prompt_value = prompt.invoke({"question": "什么是 LangChain？"})
ai_message = model.invoke(prompt_value)
result = parser.invoke(ai_message)
```

这背后最大的价值是：**组件之间更容易替换**；**流程更容易串联**；**链本身也可以继续被当作一个组件使用**；**中间结果传递方式更统一，不需要每一步都手写很多适配代码**。

这就是后面 `prompt | model | parser` 能成立的根本原因。

### 1.5 常用方法

实现了 Runnable 接口的对象，通常都支持下面这些核心方法：

| 方法               | 作用                       | 适合场景                   |
| ------------------ | -------------------------- | -------------------------- |
| `invoke(input)`    | 同步处理单个输入           | 最常用，单次调用           |
| `batch(inputs)`    | 同步批量处理多个输入       | 批量任务、离线任务         |
| `stream(input)`    | 同步流式处理               | 打字机输出、长文本生成     |
| `ainvoke(input)`   | 异步处理单个输入           | 异步服务、高并发           |
| `abatch(inputs)`   | 异步批量处理               | 异步批量任务               |
| `astream(input)`   | 异步流式处理               | 异步聊天 UI、流式返回      |
| `astream_log(...)` | 流式输出并可选带上中间步骤 | 调试链、观察多节点执行过程 |

可以把它们理解成：

- `invoke` 是最基础的“执行一次”
- 其他方法是在同步 / 异步、单条 / 批量、一次性 / 流式这几个维度上的扩展

所以当你看到“Prompt、Model、Parser、Chain 都支持 invoke”时，不要把它理解成“它们完全一样”，而要理解成：**它们不同，但都遵守同一套调用协议。**

### 1.6 在实际项目中的价值

这一点在小案例里不一定立刻感受到，但放到实际项目里就很常见。比如一个典型企业问答系统，可能会经历这样的演进：

1. 一开始只是 `prompt + model`
2. 后来为了稳定输出，加上 `parser`
3. 再后来要分中文和英文回答，加上 `branch`
4. 再后来要同时返回摘要与原文观点，加上 `parallel`
5. 再后来要插入自定义日志、埋点、字段映射，又加上 `lambda`

如果没有 Runnable 这层统一抽象，每增加一步，代码都要改很多地方；而在 LCEL 下，这些变化更像“往链上加节点”。这也是为什么 LangChain 的链式调用会让人觉得“像拼积木”。

也正因为这种统一性，后面你会发现检索器、工具、记忆模块甚至 LangGraph 节点，理解起来都会更顺。很多更复杂的高级能力，本质上都是在 Runnable 这层统一抽象之上继续往上搭。

---

## 2、LCEL 简介

### 2.1 定义

**LCEL** 是 **LangChain Expression Language** 的缩写，中文通常叫 **LangChain 表达式语言**。它的作用很直接：**用一种声明式、可组合的方式，把多个 Runnable 连接起来。**

最经典的 LCEL 写法就是：

```python
chain = prompt | model | parser
result = chain.invoke({"question": "什么是 LangChain？"})
```

所以你先把 LCEL 理解成：**“用 `|（管道符）` 或其他组合方式，把多个 Runnable 连接起来的一套表达方法。”**

但 LCEL 不只是“把对象连起来”，它还在帮我们做两件事：

- 把前一步输出自动传给后一步
- 尽量减少不同节点之间手写适配的样板代码

### 2.2 LCEL 不只是语法糖

很多人第一次看到 `prompt | model | parser`，会觉得它只是“写起来更短”。但 LCEL 的价值远不止省代码。

它真正带来的好处有三层：

**1. 表达更清晰**

传统写法：

```python
prompt_value = prompt.invoke({"question": "什么是 LangChain？"})
ai_message = model.invoke(prompt_value)
result = parser.invoke(ai_message)
```

LCEL 写法：

```python
chain = prompt | model | parser
result = chain.invoke({"question": "什么是 LangChain？"})
```

第二种写法更像在表达“流程本身”，而不是只是在堆代码。

**2. 更容易组合**

- 用 LCEL 连接出来的结果，本身还可以继续参与组合：

- 一条顺序链可以放进分支链
- 一条顺序链可以放进并行链
- 并行链的输出还可以继续交给下一步处理

而且在很多常见场景里，LCEL 还会帮我们自动处理“前一步结果如何传给下一步”这类重复性工作。只有当前后节点的输入输出结构不匹配时，我们才需要自己插入 `lambda` 或 `RunnableLambda` 做一次映射。

**3. 统一支持同步、异步、批量、流式**

根据 LangChain 官方参考，`RunnableSequence` 和 `RunnableParallel` 这类组合结构，会自动继承 Runnable 的同步、异步、批量、流式能力。这意味着你不只是得到了一条“可读的链”，还得到了一条“可统一执行的链”。

### 2.3 LCEL 的核心组合思想

在入门阶段，本章最重要的不是死记类名，而是理解 LCEL 背后的几种核心组合思想：

- **顺序组合**：前一步输出作为下一步输入
- **条件路由**：根据输入选择不同链
- **并行组合**：同一输入同时喂给多条链

这里再补充一层组合视角：

- **RunnableSequence** 和 **RunnableParallel** 是两种最核心的组合原语
- 很多其他 Runnable 结构，本质上都可以看作在这两种基础组合能力上的扩展或变体

而 `RunnableLambda` 则相当于在组合过程中插入一段你自己的 Python 逻辑，让你可以做：

- 中间结果调试
- 输入结构映射
- 输出结果整理
- 简单业务判断

也就是说，LCEL 的本质并不是“多了几个类”，而是：**LangChain 开始让你用“数据流编排”的方式思考 LLM 应用。**

### 2.4 LCEL 和 Chain 的关系

这一点特别容易混淆，所以单独强调一下：

- **LCEL 是什么**：它是构建流程的一种表达方式，重点在“怎么写”。
- **Chain 是什么**：它是通过 LCEL 或其他组合方式构建出来的可执行流程，重点在“最后得到了什么”。

所以可以先用一句最容易记的话来理解：**LCEL 是构建链的方法，Chain 是构建出来的流程。**

### 2.5 补充：stream 与 batch 如何从链上继承

**Q：有没有例子能说明"可流式、可批处理"跟 LCEL 的关系？**

#### 流式（stream）

没有 LCEL 时，`model.stream()` 返回 token 流，但 `parser.invoke()` 期待完整的 `AIMessage`，两者接不上：

```python
# 没有 LCEL：只能在循环里打印，parser 插不进来
for chunk in model.stream(prompt_value):
    print(chunk.content, end="")
```

用 LCEL 之后，整条链直接支持 `.stream()`：

```python
chain = prompt | model | StrOutputParser()

for chunk in chain.stream({"question": "什么是 LangChain？"}):
    print(chunk, end="", flush=True)
```

`StrOutputParser` 被设计成"来一个 chunk 就透传一个 chunk"，所以整条链都流起来了，不需要手写任何拼接逻辑。

#### 批量（batch）

没有 LCEL 时，批量只能 for 循环，一条一条串行：

```python
results = []
for q in questions:
    pv = prompt.invoke({"question": q})
    msg = model.invoke(pv)
    results.append(parser.invoke(msg))
```

用 LCEL 之后，`chain.batch()` 内部自动并发：

```python
chain = prompt | model | StrOutputParser()

results = chain.batch([
    {"question": "什么是 LangChain？"},
    {"question": "什么是 LCEL？"},
    {"question": "什么是 Runnable？"},
])
# 三个请求并发发出，总耗时接近一条的耗时，而不是三倍
```

**关键点**：stream / batch 是 Runnable 接口自带的能力，`|` 组合出来的新链自动继承。换任何一个 parser 或 model，`chain.stream()` 和 `chain.batch()` 还是能直接用，不需要为每种组合单独实现。

---

## 3、Chain 结构

### 3.1 定义

在 LangChain 语境里，**Chain（链）** 可以简单理解成：**把多个 Runnable 按某种规则组合起来后，形成的一段可执行流程。**

这段流程可以很简单，也可以很复杂：

- 简单时，就是 `prompt | model`
- 常见时，是 `prompt | model | parser`
- 复杂时，可能是分支 + 并行 + 自定义函数节点混合组成的流程

最关键的一点是：**链本身也是 Runnable。**

这意味着链不会成为“终点对象”，它依然可以继续被组合。比如：

- 一条顺序链可以作为 `RunnableBranch` 的分支
- 一条顺序链可以放进 `RunnableParallel`
- 并行结果还可以继续交给后面的节点处理

### 3.2 典型结构

这一节开始，我们不再重点讨论“怎么写链”，而是看“链通常长什么样”。这一章和前面三章的关系非常紧密。

如果把 [第 13 章](13-提示词与消息模板.md)、[第 11 章](11-Model-I-O与模型接入.md)、[第 14 章](14-输出解析器.md) 放在一起看，你会发现最典型的一条链就是：

1. **Prompt**：组织输入
2. **Model**：调用模型生成结果
3. **Parser**：把结果解析成业务更容易使用的形式

也就是：

```python
chain = prompt | model | parser
```

这条链不是偶然写法，而是 LangChain 里最经典、最基础的组合方式。

---

## 4、链式调用基础用法与案例

### 4.1 几种链的选择与对比

| 类型           | 典型写法 / 类名                                  | 执行方式               | 输入 → 输出     | 典型场景                     |
| -------------- | ------------------------------------------------ | ---------------------- | --------------- | ---------------------------- |
| **顺序链**     | `prompt -> model -> parser` / `RunnableSequence` | 一步接一步执行         | 单输入 → 单输出 | 最基础的问答、抽取、摘要     |
| **分支链**     | `RunnableBranch(...)`                            | 按条件只走其中一条子链 | 单输入 → 单输出 | 意图路由、多语言路由         |
| **多步串行链** | 多条子链继续串联                                 | 前一步结果给后一步     | 单输入 → 单输出 | 先总结再翻译、先整理再生成   |
| **并行链**     | `RunnableParallel({...})`                        | 多条子链同时执行       | 单输入 → 多输出 | 中英文同时生成、多模型并跑   |
| **函数链**     | `RunnableLambda(func)`                           | 在链中插入 Python 函数 | 取决于函数      | 字段映射、调试、轻量业务逻辑 |

如果你现在只想快速建立直觉，可以这样选：

- **只有一条直线流程**：先用顺序链
- **要按条件切换不同流程**：用分支链
- **要把多个模型调用前后串起来**：用多步串行链
- **要同一输入同时跑多条链**：用并行链
- **要插入自定义 Python 逻辑**：用函数链

---

### 4.2 RunnableSequence（顺序链）

详见 → [LCEL_RunnableSequence.md](LCEL_RunnableSequence.md)

### 4.3 RunnableBranch（分支链）

详见 → [LCEL_RunnableBranch.md](LCEL_RunnableBranch.md)

### 4.4 Multi-Step Chain（多步串行链）

详见 → [LCEL_MultiStepChain.md](LCEL_MultiStepChain.md)

### 4.5 RunnableParallel（并行链）

详见 → [LCEL_RunnableParallel.md](LCEL_RunnableParallel.md)

### 4.6 RunnableLambda（函数链）

详见 → [LCEL_RunnableLambda.md](LCEL_RunnableLambda.md)

### 4.7 补充：其他常见 Runnable 结构

除了本章重点展开的几种结构，LangChain 里还有一些在实际项目里很常见、但初学阶段不必深入的 Runnable 组件。

| 名称                    | 作用                                                    | 什么时候会用到                               |
| ----------------------- | ------------------------------------------------------- | -------------------------------------------- |
| `RunnablePassthrough`   | 接收输入后原样透传，也可顺手往输出中补充字段            | 想保留原始输入、给结果加键、做轻量上下文拼装 |
| `RunnableWithFallbacks` | 为某个 Runnable 配置兜底逻辑，失败后回退到备用 Runnable | 模型调用失败、主链失败后自动切备用方案       |
| `RunnableBinding`       | 为 Runnable 预绑定 `config`、默认参数等                 | 同一链在不同环境复用、可配置温度与模型等     |

你现在不需要立刻掌握它们，但可以先建立印象：**Runnable 世界不只有“顺序、分支、并行、lambda”这几种，LangChain 提供了一整套可执行节点家族。**

### 4.8 重试与兜底

真实项目里，链路不是每次都能顺利跑完：模型接口会超时，结构化输出可能偶尔不合规，外部服务也可能短暂失败。LCEL 的好处之一，是可以把这类稳定性处理挂在 Runnable 上，而不是散落在业务代码各处。

最常见的两个动作是：

| 写法                    | 作用                                | 适合场景                       |
| ----------------------- | ----------------------------------- | ------------------------------ |
| `with_retry(...)`       | 当前 Runnable 失败后自动重试        | 临时网络抖动、模型偶发格式错误 |
| `with_fallbacks([...])` | 主 Runnable 失败后切到备用 Runnable | 主模型不可用、主链策略失败     |

简化示例：

```python
safe_chain = chain.with_retry(stop_after_attempt=3)

fallback_chain = primary_chain.with_fallbacks([backup_chain])
```

这类写法不改变链的主流程，却能让项目更接近生产环境。入门阶段不需要把参数都背下来，先记住：**LCEL 不只负责”怎么串”，也能帮助你把重试、兜底、配置这些工程能力放到链上。**

#### 补充：with_retry 遇到系统性错误也有用吗？

**Q：如果 prompt 让模型返回动物信息，但 parser 期待 `Human` 字段，retry 能救吗？**

不能。`with_retry` 只对**偶发性失败**有效，对**系统性失败**无效：

| 失败类型 | 例子 | retry 有用吗 |
| -------- | ---- | ------------ |
| 偶发性（transient） | 网络超时、模型偶尔输出多余字符导致 JSON 解析失败 | 有用，再试一次大概率能过 |
| 系统性（deterministic） | prompt 让模型返回动物，但 parser 期待 `Human` 字段 | 没用，每次结果都一样，retry 只是浪费钱 |

prompt 和 schema 根本对不上时，每次模型都会返回动物数据，`Human` 校验每次都过不了，retry 几次结果相同。

**系统性失败应该从根本上修复，而不是靠 retry：**

```python
# 正确做法：把期望 schema 嵌入 prompt，让模型知道要返回什么结构
parser = PydanticOutputParser(pydantic_object=Human)
prompt = ChatPromptTemplate.from_messages([
    (“system”, “按照以下格式返回结果：\n{format_instructions}”),
    (“human”, “{question}”),
]).partial(format_instructions=parser.get_format_instructions())

chain = prompt | model | parser
# 这时偶尔格式出错才适合加 with_retry
safe_chain = chain.with_retry(stop_after_attempt=3)
```

`BeanOutputConverter.getFormat()` / `parser.get_format_instructions()` 的价值就在这里——把期望的 JSON schema 直接喂给模型，减少模型猜测，之后 `with_retry` 才真正有意义。

---

**章节思考题：**

1. LCEL 帮你解决的不是“少写几行代码”，而是什么问题？

   **参考思路：** 它让输入格式化、模型调用、解析、分支、并行和后处理都变成可组合的 Runnable。重点是流程结构清楚、可复用、可流式、可批处理，而不是管道符看起来简洁。

2. 读一条 LCEL 链时，你会如何判断每一段是否设计合理？

   **参考思路：** 看每个 Runnable 的输入和输出是否清楚，是否只做一类事情，是否方便单独测试。链路问题很多不是模型错，而是前后节点的数据结构没对上。

3. `RunnableLambda` 什么时候是好用的胶水，什么时候会变成坏味道？

   **参考思路：** 少量字段转换、清洗、路由判断适合用它；如果里面塞了大量业务逻辑、外部副作用和复杂异常处理，就应该拆成明确函数、工具或服务，而不是藏在链里。

4. 遇到“先分类、再分流、部分并行、最后汇总”的任务，你会如何拆链？

   **参考思路：** 先用顺序链做分类，再用分支链选择路线，可并行的检索或分析用并行链，最后用汇总节点统一输出。先画数据流，再写 LCEL，会比直接拼管道稳。

**本章小结：**

- **Runnable** 是 LangChain 中统一的“可执行组件”抽象。Prompt、Model、Parser、Tool、Chain 之所以能被统一调用，是因为它们在 Runnable 这层被约束成了同一种接口风格。
- **LCEL** 是把多个 Runnable 组合成链的表达式语言。它最核心的价值，不只是 `|` 写起来简洁，而是让流程变得声明式、可组合、可扩展。
- **链的核心主线** 是前面几章内容的自然延伸：把 [提示词模板](13-提示词与消息模板.md)、[模型调用](11-Model-I-O与模型接入.md)、[输出解析器](14-输出解析器.md) 组织成一条可执行流程。
- **常见链结构** 中，顺序链是基础，分支链解决路由问题，多步串行链解决前后步骤依赖问题，并行链解决多路同时处理问题，RunnableLambda 解决自定义逻辑如何插入链；重试与兜底则解决链路稳定性问题。
- 从掌握结果看，学完本章后，你至少应该：明白 `Runnable` 为什么是 LangChain 里最重要的统一抽象，知道 Prompt、Model、Parser、Chain 都能被统一调用；能熟练读懂并写出 `prompt | model | parser` 这种最基础 LCEL 链；能区分顺序链、分支链、多步串行链、并行链、函数链的使用场景。

**建议下一步：** 先把本章 5 个案例都跑一遍，重点观察“每一步输入输出是什么、为什么能接上下一步”；然后继续学习 [第 16 章 记忆与对话历史](16-记忆与对话历史（含Redis基础）.md) 和 [第 17 章 Tools 工具调用](17-Tools工具调用.md)，你会更清楚链式调用如何进一步扩展成带状态、带工具、带决策能力的 LLM 应用。
