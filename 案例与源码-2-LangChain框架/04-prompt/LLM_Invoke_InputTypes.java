/**
 * 对应 Python：13.1-invoke/LLM_Invoke_InputTypes.py
 * 4 种输入格式：Message 对象 / 元组 / 字典 / 字符串
 *
 * Python 支持的 4 种输入格式：
 *   # 1. Message 对象列表
 *   model.invoke([SystemMessage("..."), HumanMessage("...")])
 *
 *   # 2. 元组列表
 *   model.invoke([("system", "..."), ("human", "...")])
 *
 *   # 3. 字典列表（OpenAI 风格）
 *   model.invoke([{"role": "system", "content": "..."}, {"role": "user", "content": "..."}])
 *
 *   # 4. 纯字符串（自动包装为 HumanMessage）
 *   model.invoke("你是谁？")
 *
 * Java / Spring AI 注：
 *   Spring AI ChatClient 的 .user()/.system() 方法对应方式 4 和简化方式 1。
 *   完整 Message 对象列表对应方式 1。
 *   没有元组/字典输入的直接等价，但功能上完全可以覆盖。
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootApplication
public class LLM_Invoke_InputTypes {
    public static void main(String[] args) {
        SpringApplication.run(LLM_Invoke_InputTypes.class, args);
    }
}

@Component
class InputTypesRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient model = chatClientBuilder.build();

        // ─── 方式 1：Message 对象列表（最完整）──────────────────────────
        // Python: model.invoke([SystemMessage("..."), HumanMessage("...")])
        System.out.println("【方式 1：Message 对象列表】");
        String r1 = model.prompt()
                .messages(List.of(
                        new SystemMessage("你是一个AI助手"),
                        new UserMessage("你是谁？")
                ))
                .call()
                .content();
        System.out.println(r1);

        // ─── 方式 2：system() + user() 链式（Spring AI 高级 API）────────
        // Python: model.invoke([("system", "..."), ("human", "...")])
        // Java 没有元组输入，但 ChatClient 的 system()+user() 完全等价
        System.out.println("\n【方式 2：system() + user() 链式调用】");
        String r2 = model.prompt()
                .system("你是一个AI助手")
                .user("你是谁？")
                .call()
                .content();
        System.out.println(r2);

        // ─── 方式 3：字典风格（通过 Map 构建）───────────────────────────
        // Python: model.invoke([{"role": "system", "content": "..."}, {"role": "user", "content": "..."}])
        // Java：没有 dict 直接输入，等价于方式 1（用 Message 对象代替 dict）
        System.out.println("\n【方式 3：字典风格（等价为 Message 对象，Java 无原生字典输入）】");
        // 与方式 1 相同，略

        // ─── 方式 4：纯字符串（自动包装为 HumanMessage）─────────────────
        // Python: model.invoke("你是谁？")
        // Java:   chatClient.prompt().user("你是谁？")  ← 等价
        System.out.println("\n【方式 4：纯字符串输入（自动作为 user 消息）】");
        String r4 = model.prompt()
                .user("你是谁？")
                .call()
                .content();
        System.out.println(r4);
    }
}

/*
【Java vs Python 4 种输入方式对照】

Python 方式 1：[SystemMessage("..."), HumanMessage("...")]
  ↔ Java: chatClient.prompt().messages(List.of(new SystemMessage("..."), new UserMessage("...")))

Python 方式 2：[("system", "..."), ("human", "...")]
  ↔ Java: chatClient.prompt().system("...").user("...")（无元组语法，但 API 更清晰）

Python 方式 3：[{"role": "system", "content": "..."}, ...]
  ↔ Java: 同方式 1（Message 对象代替 dict）

Python 方式 4：model.invoke("你是谁？")
  ↔ Java: chatClient.prompt().user("你是谁？").call()（等价）

【结论】
Python 支持 4 种输入格式是为了方便不同来源的数据直接传入。
Java / Spring AI 主要用 Message 对象 + ChatClient 链式 API，同样简洁且类型安全。
*/
