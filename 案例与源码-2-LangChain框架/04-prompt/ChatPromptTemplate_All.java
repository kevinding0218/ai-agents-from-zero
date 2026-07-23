/**
 * 对应 Python：13.3-chat_prompt_template/ 全系列文件
 *   ChatPromptTemplate_Constructor.py      → 构造函数
 *   ChatPromptTemplate_FormatMessages.py   → from_messages + format_messages / invoke / format
 *   13.3.1-parameter/
 *     ChatPromptTemplate_TupleParam.py     → 元组 ("role", "content")
 *     ChatPromptTemplate_DictParam.py      → 字典 {"role":..., "content":...}
 *     ChatPromptTemplate_MessageParam.py   → Message 类
 *   13.3.2-placeholder/
 *     ChatPromptTemplate_ExplicitPlaceholder.py → MessagesPlaceholder("memory")
 *     ChatPromptTemplate_ImplicitPlaceholder.py → ("placeholder", "{memory}")
 *     ChatPromptTemplate_MemoryDemo.py    → 有/无历史 memory 对比
 *
 * Java 对应：
 *   Spring AI 用 List<Message> + ChatClient 实现全部功能。
 *   没有 ChatPromptTemplate 这个独立类，但所有概念一一对应。
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class ChatPromptTemplate_All {
    public static void main(String[] args) {
        SpringApplication.run(ChatPromptTemplate_All.class, args);
    }
}

@Component
class ChatPromptRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient model = chatClientBuilder.build();

        // ─── 构造函数 / from_messages / TupleParam / DictParam / MessageParam ─────
        // 这 5 个文件展示的是「同一件事的不同写法」：定义消息列表
        // Python 支持元组、字典、Message 类 3 种参数，最终都得到同样的消息列表
        // Java 只有一种写法：Message 对象列表（最清晰，类型安全）

        System.out.println("【方式对比：Python 3 种参数 → Java 1 种】");

        // Python 元组写法：[("system", "你是AI开发工程师"), ("human", "你能做什么？")]
        // Python 字典写法：[{"role":"system","content":"你是AI开发工程师"}, ...]
        // Python Message类：[SystemMessage("你是AI开发工程师"), HumanMessage("你能做什么？")]
        // ↓↓↓ Java 统一使用 Message 类写法 ↓↓↓
        List<Message> messages = List.of(
                new SystemMessage("你是AI开发工程师，你的名字是小谷AI。"),
                new UserMessage("你能帮我做什么？"),
                new AssistantMessage("我能开发很多AI应用。"),
                new UserMessage("7 + 5等于多少")
        );

        // Python: prompt = chatPromptTemplate.format_messages(name="...", thing="...", user_input="...")
        // Java: 直接构建 List<Message>（占位符已在 Message 里填好）
        String result = model.prompt()
                .messages(messages)
                .call()
                .content();
        System.out.println("模型回答：" + result);

        System.out.println("\n" + "=".repeat(50));

        // ─── format_messages / invoke / format 三种方式 ──────────────────────────
        // Python:
        //   format_messages(**vars) → List[Message]（推荐）
        //   invoke({...})           → ChatPromptValue
        //   format(**vars)          → str（纯文本，不保留角色结构）
        //
        // Java: 只有 List<Message>，没有格式差异，直接传给模型

        System.out.println("【format_messages / invoke / format 对照】");

        // Python: format_messages → List[Message] → 最终发给 model.invoke(prompt)
        // Java 等价：直接构建 List<Message> → chatClient.prompt().messages(...)
        List<Message> chatMessages = List.of(
                new SystemMessage("你是一个python开发工程师，请回答我提出的问题"),
                new UserMessage("堆排序怎么写？")
        );

        String answer = model.prompt().messages(chatMessages).call().content();
        System.out.println("format_messages 等价结果：" + answer);

        System.out.println("\n" + "=".repeat(50));

        // ─── MessagesPlaceholder（显式/隐式）+ MemoryDemo ─────────────────────────
        // Python MessagesPlaceholder("memory") 的作用：在模板里预留一段历史消息的位置
        // Java 等价：直接在 List<Message> 里 addAll(historyMessages)

        System.out.println("【MessagesPlaceholder 有/无历史 memory 对比】");

        // 情况 A：没有历史 memory（空列表）
        List<Message> noHistory = new ArrayList<>();
        noHistory.add(new SystemMessage("你是一个简洁的助手，回答请控制在30字以内"));
        // MessagesPlaceholder([]) → 插入空列表，相当于没有历史
        noHistory.add(new UserMessage("请问我的名字叫什么？"));

        System.out.println("情况 A（无历史）：");
        System.out.println(model.prompt().messages(noHistory).call().content());

        // 情况 B：有历史 memory（插入历史消息）
        List<Message> withHistory = new ArrayList<>();
        withHistory.add(new SystemMessage("你是一个简洁的助手，回答请控制在30字以内"));
        // Python: MessagesPlaceholder("memory") → invoke 时把历史列表插入这里
        // Java: 直接 addAll 历史消息
        withHistory.addAll(List.of(
                new UserMessage("我的名字叫亮仔，是一名程序员"),
                new AssistantMessage("好的，亮仔你好！")
        ));
        withHistory.add(new UserMessage("请问我的名字叫什么？"));

        System.out.println("情况 B（有历史）：");
        System.out.println(model.prompt().messages(withHistory).call().content());
    }
}

/*
【Java vs Python ChatPromptTemplate 全面对照】

Python ChatPromptTemplate 的 3 种参数格式：
  元组：[("system","内容"), ("human","内容")]
  字典：[{"role":"system","content":"内容"}, ...]
  类：  [SystemMessage("内容"), HumanMessage("内容")]
  ↔ Java 只有 Message 类：List.of(new SystemMessage(""), new UserMessage(""))

Python format_messages(**vars)  → List[Message]  ↔  Java List<Message>（直接构建）
Python invoke({...})            → ChatPromptValue ↔  Java Prompt（chatClient.prompt(prompt)）
Python format(**vars)           → str             ↔  Java String（手动拼接，不常用）

Python MessagesPlaceholder("memory")              ↔  Java list.addAll(historyMessages)
Python ("placeholder", "{memory}")（隐式写法）    ↔  Java 同上，只是语法不同

Python 情况 A（memory=[]）                        ↔  Java 不添加历史消息
Python 情况 B（memory=[HumanMsg,AIMsg]）          ↔  Java withHistory.addAll(List.of(new UserMessage, new AssistantMessage))

【核心总结】
Python ChatPromptTemplate 提供了丰富的「模板写法」，让 Prompt 可以版本管理和复用。
Java 没有等价的模板对象，但 List<Message> 直接构建同样清晰、类型安全，而且更直接。
MessagesPlaceholder 在 Java 里就是 list.addAll() —— 把历史消息塞进列表的某个位置。
*/
