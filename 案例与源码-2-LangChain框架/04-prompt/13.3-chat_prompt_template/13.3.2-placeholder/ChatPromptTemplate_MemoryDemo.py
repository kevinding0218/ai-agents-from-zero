"""
【案例】MessagesPlaceholder：有无历史对话的真实对比

对应教程章节：第 13 章 - 提示词与消息模板 → 7.5、MessagesPlaceholder：消息占位符

知识点速览：
- 模型本身没有记忆，每次调用都是全新的。
- MessagesPlaceholder 的作用：把上一轮的消息"塞回"给模型，让它知道之前说了什么。
- 本案例对比「有无历史」的实际回答，让你直观感受 memory 的效果。

How to run?
$ python3 案例与源码-2-LangChain框架/04-prompt/13.3-chat_prompt_template/13.3.2-placeholder/ChatPromptTemplate_MemoryDemo.py
"""

from langchain_core.messages import HumanMessage, AIMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_ollama import ChatOllama

model = ChatOllama(base_url="http://localhost:11434", model="qwen3:4b", reasoning=False)

# 模板：系统设定 + 历史占位 + 当前问题
prompt = ChatPromptTemplate.from_messages(
    [
        ("system", "你是一个简洁的助手，回答请控制在30字以内"),
        MessagesPlaceholder("memory"),
        ("human", "{question}"),
    ]
)

# 模拟「上一轮」的对话记录
history = [
    HumanMessage("我的名字叫亮仔，是一名程序员"),
    AIMessage("好的，亮仔你好！很高兴认识你。"),
]

question = "请问我的名字叫什么？"

# ── 情况 A：没有历史（memory 传空列表）────────────────────────
print("=" * 50)
print("【情况 A】没有历史 memory（模型不知道你说过什么）")
print("=" * 50)
prompt_a = prompt.invoke({"memory": [], "question": question})
print("发给模型的完整 prompt：")
print(prompt_a.to_string())
print()
response_a = model.invoke(prompt_a.to_messages())
print(f"模型回答：{response_a.content}")

print()

# ── 情况 B：有历史（memory 传上一轮记录）────────────────────────
print("=" * 50)
print("【情况 B】有历史 memory（模型能看到你之前说过的话）")
print("=" * 50)
prompt_b = prompt.invoke({"memory": history, "question": question})
print("发给模型的完整 prompt：")
print(prompt_b.to_string())
print()
response_b = model.invoke(prompt_b.to_messages())
print(f"模型回答：{response_b.content}")

"""
【输出示例】
==================================================
【情况 A】没有历史 memory（模型不知道你说过什么）
==================================================
发给模型的完整 prompt：
System: 你是一个简洁的助手，回答请控制在30字以内
Human: 请问我的名字叫什么？

模型回答：对不起，我不知道您的姓名，请您告诉我。

==================================================
【情况 B】有历史 memory（模型能看到你之前说过的话）
==================================================
发给模型的完整 prompt：
System: 你是一个简洁的助手，回答请控制在30字以内
Human: 我的名字叫亮仔，是一名程序员
AI: 好的，亮仔你好！很高兴认识你。
Human: 请问我的名字叫什么？

模型回答：你的名字叫亮仔。
"""
