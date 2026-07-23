/**
 * 对应 Python：LangChainV1.0.py
 * 现代写法：通过 Spring 自动配置注入 ChatClient（统一入口）
 *
 * Python 1.0 风格：
 *   model = init_chat_model(model="qwen-plus", model_provider="openai", ...)
 *   print(model.invoke("你是谁").content)
 *
 * Java 对应：
 *   Spring Boot 读取 application.properties 里的配置，
 *   自动装配 ChatModel / ChatClient，注入到你的类里直接用。
 *   → 对应 Python 的"统一入口 + 配置文件读取"风格
 *
 * application.properties（对应 .env 文件）：
 *   spring.ai.openai.api-key=${QWEN_API_KEY}
 *   spring.ai.openai.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
 *   spring.ai.openai.chat.options.model=qwen-plus
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class LangChainV1_0 {

    public static void main(String[] args) {
        SpringApplication.run(LangChainV1_0.class, args);
    }
}

@Component
class HelloRunner implements CommandLineRunner {

    // Spring 自动注入 ChatModel（对应 Python 的 init_chat_model 返回值）
    // 具体用哪个模型由 application.properties 决定，代码里不写死
    @Autowired
    private ChatModel chatModel;

    // ChatClient 是更高层的流式 API（Spring AI 1.0 推荐）
    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {

        // ========== 写法 A：直接用 ChatModel.call()（底层一点）==========
        // Python: model.invoke("你是谁").content
        String content = chatModel
                .call(new org.springframework.ai.chat.messages.UserMessage("你是谁"))
                .getResult().getOutput().getText();
        System.out.println(content);

        System.out.println("*".repeat(50));

        // ========== 写法 B：用 ChatClient（更流畅，Spring AI 推荐）==========
        // Python: model.invoke("你是谁").content
        ChatClient chatClient = chatClientBuilder.build();
        String reply = chatClient.prompt()
                .user("你是谁")
                .call()
                .content();   // 直接拿正文字符串，等同于 .content
        System.out.println(reply);

        // ========== 多模型：注入第二个模型（对应 Python 里的 model2）==========
        // Python:
        //   model2 = init_chat_model(model="deepseek-v3", ...)
        //   print(model2.invoke("你是谁").content)
        //
        // Java：在 application.properties 里配置第二个模型，
        //       或用 @Qualifier 注解区分多个 ChatModel bean（见 LangChainMoreV1_0.java）
    }
}

/*
【Java vs Python 对照】
Python .env 文件                     ↔  Java application.properties
Python init_chat_model(model=...)    ↔  Java @Autowired ChatModel（Spring 自动装配）
Python model.invoke("...").content   ↔  Java chatClient.prompt().user("...").call().content()
Python 多实例（llm_qwen, llm_ds）    ↔  Java @Qualifier 区分多个 ChatModel bean
*/
