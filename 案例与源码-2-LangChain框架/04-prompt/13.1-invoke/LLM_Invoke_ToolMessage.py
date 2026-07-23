"""
【案例】ToolMessage 完整工具调用流程

【核心问题：模型怎么知道调用哪个 tool？】

  答：模型不"知道"，它是根据 tool 的 description（描述）来推理的。
  你给模型一份"工具菜单"（tools 列表），每个工具都有 name + description。
  模型读用户的问题，再看菜单里哪个工具的描述最匹配，就决定调用哪个。

  例如：用户问"北京天气怎样？"
        菜单里有 get_weather（"查询指定城市的天气"）
                  calculator（"执行数学计算"）
        模型看到"天气" → 匹配 get_weather → 调用它

  ⚠️ 如果描述写得不清楚，模型可能选错工具。
  所以 description 是 tool 最重要的字段。

【整体流程：同一个问题经历了两次 invoke】

  用户："北京今天天气怎么样？"
        ↓
  [第一次 invoke] 模型看到问题，发现需要查天气
                  → 不直接回答，而是说"我要调用 get_weather"
                  → 返回 AIMessage（content 为空，tool_calls 有内容）
        ↓
  [你的代码] 真正执行 get_weather("北京") → 拿到结果
        ↓
  把结果包成 ToolMessage 追加到消息列表
        ↓
  [第二次 invoke] 模型现在有了工具结果
                  → 终于可以回答用户了
                  → 返回 AIMessage（content 有最终答案）

  ⚠️ Step 5 不是新问题！是同一个问题的"续"。
  用户只问了一次，但模型需要两次 invoke 才能完成：
    第一次：决定用工具
    第二次：拿到工具结果后，生成最终答案

How to run?
$ python3 案例与源码-2-LangChain框架/04-prompt/13.1-invoke/LLM_Invoke_ToolMessage.py
"""

from langchain_ollama import ChatOllama
from langchain_core.messages import HumanMessage, SystemMessage, AIMessage, ToolMessage

model = ChatOllama(
    base_url="http://localhost:11434",
    model="qwen3:4b",
    reasoning=False,
)

# ══════════════════════════════════════════════════════════════
# 工具菜单：告诉模型有哪些工具可以用
# ══════════════════════════════════════════════════════════════
# 这里有两个工具，模型会根据用户的问题自己选择调用哪个。
# description 是关键 —— 模型靠它来判断"这个工具是干什么的"。
# 如果你只有一个工具，模型只能选那一个；
# 如果有多个，模型会推理哪个最合适，甚至可以先后调用多个。
tools = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "查询指定城市的当前天气",   # ← 模型靠这句话决定要不要调用这个工具
            "parameters": {
                "type": "object",
                "properties": {
                    "city": {"type": "string", "description": "城市名，如：北京、上海"}
                },
                "required": ["city"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "calculator",
            "description": "执行数学计算，输入一个数学表达式字符串",  # ← 不同描述，模型用来区分两个工具
            "parameters": {
                "type": "object",
                "properties": {
                    "expression": {"type": "string", "description": "数学表达式，如：12 * 34 + 5"}
                },
                "required": ["expression"],
            },
        },
    },
]

# 真正执行工具的函数（你自己写的逻辑，模型不执行，只是"请求"调用）
def get_weather(city: str) -> str:
    fake_data = {
        "北京": "晴天，气温 28°C，东风 3 级，空气质量良",
        "上海": "多云，气温 32°C，东南风 2 级，湿度 80%",
    }
    return fake_data.get(city, f"{city} 的天气数据暂不可用")

def calculator(expression: str) -> str:
    try:
        return str(eval(expression))
    except Exception as e:
        return f"计算错误：{e}"

# 工具名 → 函数的映射表，用于根据模型的选择找到对应函数
tool_registry = {
    "get_weather": get_weather,
    "calculator": calculator,
}

# ══════════════════════════════════════════════════════════════
# 用户问题
# ══════════════════════════════════════════════════════════════
messages = [
    SystemMessage(content="你是一个助手，需要时请使用提供的工具。"),
    HumanMessage(content="北京今天天气怎么样？"),   # ← 只有一个问题，整个流程都围绕这个问题
]

# ══════════════════════════════════════════════════════════════
# 第一次 invoke：模型决定用哪个工具
# ══════════════════════════════════════════════════════════════
# bind_tools(tools) = 把工具菜单附加给模型
# 此时模型会：
#   1. 读用户问题
#   2. 看工具菜单里哪个 description 匹配
#   3. 返回 AIMessage，content 为空，tool_calls 里写明要调用哪个工具+参数
#
# ⚠️ 注意：模型在这里"只是提出请求"，它不会真正执行工具。
#    真正执行是下面你的代码做的。
print("=" * 60)
print("第一次 invoke：模型决定调用哪个工具")
print("=" * 60)
first_response = model.bind_tools(tools).invoke(messages)

print(f"content（为空）: {repr(first_response.content)}")
print(f"tool_calls（模型选了哪个工具）: {first_response.tool_calls}")
# 预期输出：
# content: ''  ← 模型没有直接回答，因为它需要先查天气
# tool_calls: [{'name': 'get_weather', 'args': {'city': '北京'}, 'id': 'xxx'}]
#                              ↑ 模型选了 get_weather 而不是 calculator，
#                                因为问题是"天气"，匹配 get_weather 的 description

# ══════════════════════════════════════════════════════════════
# 你的代码：读取模型的选择，真正执行工具
# ══════════════════════════════════════════════════════════════
print("\n" + "=" * 60)
print("你的代码：执行工具")
print("=" * 60)

tool_call = first_response.tool_calls[0]
tool_name    = tool_call["name"]    # "get_weather"（模型选的）
tool_args    = tool_call["args"]    # {"city": "北京"}（模型填的参数）
tool_call_id = tool_call["id"]      # 唯一 ID，ToolMessage 必须用这个对应

# 用 tool_registry 找到对应的函数并执行
tool_fn = tool_registry[tool_name]
tool_result = tool_fn(**tool_args)

print(f"模型选择的工具：{tool_name}")
print(f"模型填的参数：{tool_args}")
print(f"工具执行结果：{tool_result}")

# ══════════════════════════════════════════════════════════════
# 把工具结果封装成 ToolMessage，追加到消息列表
# ══════════════════════════════════════════════════════════════
# 消息列表现在变成：
#   [System, Human("北京天气？"), AIMessage(tool_calls), ToolMessage(天气结果)]
# 这整个列表代表"到目前为止发生的一切"，下一次 invoke 会全部发给模型。
messages.append(first_response)                   # AIMessage（第一次模型回复）
messages.append(ToolMessage(
    content=tool_result,
    tool_call_id=tool_call_id,  # ← 必须和上面的 tool_call["id"] 一致
))

print("\n当前完整消息列表：")
for i, msg in enumerate(messages):
    role = type(msg).__name__
    preview = str(msg.content)[:50] if msg.content else "(空，只有 tool_calls)"
    print(f"  [{i}] {role:20s} → {preview}")

# ══════════════════════════════════════════════════════════════
# 第二次 invoke：模型拿到工具结果，回答原始问题
# ══════════════════════════════════════════════════════════════
# ⚠️ 这不是新问题！用户只问了一次"北京今天天气怎么样？"
# 第二次 invoke 是同一问题的"续"：
#   模型现在有了 ToolMessage 里的天气数据，
#   终于可以生成自然语言答案回复用户了。
print("\n" + "=" * 60)
print("第二次 invoke：模型结合工具结果，回答原始问题")
print("=" * 60)
final_response = model.bind_tools(tools).invoke(messages)
print(f"最终回答: {final_response.content}")

"""
【如果有多个工具，模型会怎么选？】

  试着把用户问题改成 "12 乘以 34 等于多少？"，
  模型会选 calculator 而不是 get_weather，
  因为 calculator 的 description 是"执行数学计算"，更匹配。

  tool_calls 会变成：
  [{'name': 'calculator', 'args': {'expression': '12 * 34'}, 'id': 'xxx'}]

【实际应用中你不需要手写这些步骤】
  LangChain Agent 会自动循环：invoke → 判断有没有 tool_calls → 执行工具 → 再 invoke → 直到没有 tool_calls 为止。
  本文件是为了让你看清楚每一步发生了什么。
"""
