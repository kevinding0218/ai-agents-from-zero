/**
 * 对应 Python：ModelIO_ChatOpenAI.py
 * OpenAI 兼容接口：让非 OpenAI 模型（通义、DeepSeek 等）也能用 OpenAI 客户端调用
 *
 * Python:
 *   llm = ChatOpenAI(
 *       model="qwen-plus",
 *       api_key=os.getenv("aliQwen-api"),
 *       base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
 *   )
 *   response = llm.invoke("你是谁？")
 *   print(response.content)
 *
 * Java 对应：
 *   Spring AI 的 OpenAiChatModel 接受 base-url 覆盖，完全等价于 ChatOpenAI(base_url=...)。
 *   配置放在 application.properties，代码里只注入 ChatClient。
 *
 * application.properties：
 *   spring.ai.openai.api-key=${QWEN_API_KEY}
 *   spring.ai.openai.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
 *   spring.ai.openai.chat.options.model=qwen-plus
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class ModelIO_ChatOpenAI {
    public static void main(String[] args) {
        SpringApplication.run(ModelIO_ChatOpenAI.class, args);
    }
}

@Component
class ChatOpenAIRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient chatClient = chatClientBuilder.build();

        // Python: response = llm.invoke("你是谁？")
        ChatResponse response = chatClient.prompt()
                .user("你是谁？")
                .call()
                .chatResponse();

        // Python: print(response)        → AIMessage 对象
        // Python: print(response.content) → 模型返回的文本
        AssistantMessage output = response.getResult().getOutput();
        System.out.println("完整响应对象：" + response);
        System.out.println("文本内容：" + output.getText());

        // Python: print(type(response))  → <class 'langchain_core.messages.ai.AIMessage'>
        System.out.println("类型：" + output.getClass().getName());
    }
}

/*
【Java vs Python 对照】
Python ChatOpenAI(base_url=..., api_key=...)   ↔  Java application.properties 配置 base-url + api-key
Python llm.invoke("...")                        ↔  Java chatClient.prompt().user("...").call().chatResponse()
Python response.content                         ↔  Java response.getResult().getOutput().getText()
Python type(response)                           ↔  Java response.getClass().getName()

【关键点】
base_url 覆盖让非 OpenAI 模型（通义、DeepSeek）接入 ChatOpenAI。
Spring AI 同理：OpenAiChatModel + 自定义 base-url，可接任何 OpenAI 兼容接口。
*/
