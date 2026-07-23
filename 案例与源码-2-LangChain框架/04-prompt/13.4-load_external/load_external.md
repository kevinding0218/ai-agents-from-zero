# 从文件加载提示词（JSON / YAML）

> 对应教程：[第 13 章 - 提示词与消息模板](../13-提示词与消息模板.md) §8

---

## 8、从文件加载提示词

当 Prompt 还很短时，把它直接写在 Python 代码里问题不大；但只要你开始做真实项目，就会很快遇到几个问题：Prompt 越来越长，代码越来越乱；产品、运营、算法同学希望一起改 Prompt；需要保留多个版本做 A/B 测试；想把 Prompt 与代码逻辑分离。

这时候，把 Prompt 放到 **JSON / YAML** 等外部文件中，就会更工程化。LangChain 提供了 `load_prompt(...)`，可以根据文件内容加载为模板对象。对于本章案例来说，最常见的是 `_type: "prompt"`，即加载为 `PromptTemplate`。

### 8.1 从 JSON 加载

**`prompt.json`：**

```json
{
  "_type": "prompt",
  "input_variables": ["name", "what"],
  "template": "请{name}讲一个{what}的故事"
}
```

**加载并使用：**

```python
from langchain_core.prompts import load_prompt

template = load_prompt("prompt.json", encoding="utf-8")
print(template.format(name="张三", what="搞笑"))
```

【案例源码】[PromptLoadFromJson.py](PromptLoadFromJson.py) | [prompt.json](prompt.json)

### 8.2 从 YAML 加载

YAML 与 JSON 的核心思路完全相同，只是文件格式更适合人工阅读，也更方便写注释。对团队协作来说，很多场景会更喜欢 YAML。

运行这类案例时，先确认**当前工作目录**，否则相对路径可能找不到文件。这一点和第 10 章 HelloWorld 中的 `.env` 读取问题本质很像，都是"脚本运行位置"和"相对路径"的关系。

【案例源码】[PromptLoadFromYaml.py](PromptLoadFromYaml.py) | [prompt.yaml](prompt.yaml)

---

到这里你可以看到，本章知识已经形成了一个比较完整的工程化闭环：

- **消息类型** 解决"输入按什么角色组织"
- **调用方式** 解决"输入如何发给模型"
- **PromptTemplate / ChatPromptTemplate** 解决"输入如何复用"
- **MessagesPlaceholder** 解决"历史对话如何动态插入"
- **JSON / YAML 外置文件** 解决"模板如何协作与版本管理"

---

## 章节思考题

1. 普通字符串 Prompt 和消息模板最大的工程差别是什么？

   **参考思路：** 普通字符串适合临时调用，消息模板更适合长期维护。它把系统规则、用户输入、历史消息和变量占位分清楚，后续改规则、换输入、接多轮历史都会更稳。

2. 什么时候应该用 `MessagesPlaceholder`，而不是把历史对话拼成一大段字符串？

   **参考思路：** 需要保留消息角色、顺序和多轮结构时，应使用 `MessagesPlaceholder`。拼字符串会丢掉角色信息，也不利于后续和记忆、工具调用、消息对象体系衔接。

3. 如果一个模板变量越来越多，你会如何判断它是不是该拆分？

   **参考思路：** 看变量是否服务同一个任务、是否来自同一层上下文、是否经常一起变化。如果一个模板同时管规则、业务输入、检索结果、历史消息和输出格式，可能就该拆成更小的模板或链路节点。

4. 把提示词放到外部文件有什么好处和风险？

   **参考思路：** 好处是便于版本管理、运营调整和复用；风险是变量名、格式和代码调用容易不一致。外置后要配套校验、示例输入和变更记录，不能只把文本搬出去。

## 本章小结

- **Prompt 的本质**：Prompt 不是"随便写一句话"，而是对模型输入进行结构化组织。随着项目复杂度提升，输入会从纯字符串演化成多角色消息，再进一步演化成模板、占位符与外部配置文件。
- **消息与调用**：聊天模型常见输入包括 `str`、消息对象列表、元组列表、字典列表；常见调用方式包括 `invoke / ainvoke`、`stream / astream`、`batch / abatch`。返回值通常是 `AIMessage`，正文一般通过 `.content` 读取。
- **模板与工程化**：`PromptTemplate` 适合文本模板，`ChatPromptTemplate` 适合聊天模型与多角色场景，`MessagesPlaceholder` 是多轮历史拼接的关键；将 Prompt 放入 JSON / YAML 更适合真实项目中的版本管理、多人协作与 A/B 测试。

**建议下一步：** 继续学习第 14 章 输出解析器，把本章的"输入组织"与"输出结构化"连起来；再配合第 15 章 LCEL 与链式调用，就能形成 LangChain 中最核心的"输入 → 模型 → 输出 → 链式编排"主线。
