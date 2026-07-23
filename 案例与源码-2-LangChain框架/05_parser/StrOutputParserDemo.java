/**
 * 对应 Python：StrOutputParserDemo.py
 * StrOutputParser：从模型输出中取出纯文本字符串
 *
 * Python:
 *   from langchain_core.output_parsers import StrOutputParser
 *   parser = StrOutputParser()
 *
 *   result = model.invoke(prompt)          # AIMessage 对象
 *   response = parser.invoke(result)        # 取出 result.content → str
 *
 *   # 链式写法（更常用）
 *   chain = prompt | model | parser
 *   response = chain.invoke({"role":"...", "question":"..."})
 *
 * Java 对应：
 *   Spring AI ChatClient.prompt().call().content() 已经直接返回字符串，
 *   等价于 Python 的 model.invoke(prompt).content，无需额外 StrOutputParser。
 *   链式写法对应 Spring AI 的流式 API 链（见 06-lcel/LCEL_RunnableSequenceDemo.java）。
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootApplication
public class StrOutputParserDemo {
    public static void main(String[] args) {
        SpringApplication.run(StrOutputParserDemo.class, args);
    }
}

@Component
class StrParserRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StrParserRunner.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient model = chatClientBuilder.build();

        // ─── 构建消息（等价 Python 的 ChatPromptTemplate + invoke）──────────
        // Python: prompt = chat_prompt.invoke({"role": "AI助手", "question": "..."})
        var messages = List.of(
                new SystemMessage("你是一个AI助手，请简短回答我提出的问题"),
                new UserMessage("什么是LangChain，简洁回答100字以内")
        );
        logger.info("消息列表：{}", messages);

        // ─── 调用模型，得到原始响应 ───────────────────────────────────────
        // Python: result = model.invoke(prompt)  → AIMessage 对象
        ChatResponse result = model.prompt()
                .messages(messages)
                .call()
                .chatResponse();
        logger.info("模型原始输出：{}", result);

        // ─── StrOutputParser：取出纯文本 ──────────────────────────────────
        // Python: parser = StrOutputParser()
        //         response = parser.invoke(result)   → str（从 result.content 取出）
        // Java:   Spring AI 直接提供 .content() 方法，无需单独的 Parser 类
        String response = result.getResult().getOutput().getText();
        logger.info("解析后的纯字符串：{}", response);
        logger.info("结果类型：{}", response.getClass().getSimpleName());

        System.out.println("\n" + "=".repeat(50));
        System.out.println("【更简洁的 Java 写法（等价 prompt | model | parser 链式）】");

        // Python: chain = prompt | model | parser
        //         response = chain.invoke({"role":"AI助手","question":"..."})
        // Java:   直接用 .content() 一步到位
        String direct = model.prompt()
                .system("你是一个AI助手，请简短回答我提出的问题")
                .user("什么是LangChain，简洁回答100字以内")
                .call()
                .content();   // 等价于 model → StrOutputParser → String
        logger.info("链式结果：{}", direct);
    }
}

/*
【Java vs Python 对照】
Python StrOutputParser()                    ↔  Java 无需单独类（.content() 已内置）
Python parser.invoke(result)                ↔  Java result.getResult().getOutput().getText()
Python result.content                       ↔  Java result.getResult().getOutput().getText()
Python chain = prompt | model | parser      ↔  Java chatClient.prompt().system().user().call().content()

【为什么 Python 需要 StrOutputParser？】
LangChain 的 model.invoke() 返回 AIMessage 对象，不是纯字符串。
StrOutputParser 的作用是"取出 AIMessage.content，转成 str"。
这样 parser 就成了 Runnable，可以用 | 组合进链里，流式时也统一处理 chunk。

【为什么 Java 不需要？】
Spring AI ChatClient.prompt().call().content() 已经直接返回字符串，
框架层面就完成了"从响应对象中取出文本"这个动作，无需额外步骤。
*/
