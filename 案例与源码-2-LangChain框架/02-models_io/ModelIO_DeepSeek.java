/**
 * 对应 Python：ModelIO_DeepSeek.py
 * DeepSeek 原生接口：temperature / max_tokens / max_retries 参数
 *
 * Python:
 *   llm = ChatDeepSeek(
 *       model="deepseek-chat",
 *       api_key=os.getenv("deepseek-api"),
 *       temperature=0.8,
 *       max_tokens=1024,
 *       max_retries=3,
 *   )
 *   response = llm.invoke("你是谁？")
 *   print(response.content)
 *
 * Java 对应：
 *   DeepSeek 的接口与 OpenAI 完全兼容，Spring AI 用 OpenAiChatModel + base-url 覆盖即可。
 *   temperature / maxTokens 通过 OpenAiChatOptions 设置；重试由 Spring Retry 处理。
 *
 * application.properties：
 *   spring.ai.openai.api-key=${DEEPSEEK_API_KEY}
 *   spring.ai.openai.base-url=https://api.deepseek.com
 *   spring.ai.openai.chat.options.model=deepseek-chat
 *   spring.ai.openai.chat.options.temperature=0.8
 *   spring.ai.openai.chat.options.max-tokens=1024
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class ModelIO_DeepSeek {
    public static void main(String[] args) {
        SpringApplication.run(ModelIO_DeepSeek.class, args);
    }
}

@Component
class DeepSeekRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        // Python: llm = ChatDeepSeek(temperature=0.8, max_tokens=1024, max_retries=3)
        // Java: 运行时覆盖参数（也可在 application.properties 里设默认值）
        ChatClient chatClient = chatClientBuilder
                .defaultOptions(OpenAiChatOptions.builder()
                        .temperature(0.8)
                        .maxTokens(1024)
                        // max_retries → Spring Retry，在 application.properties 里配置
                        // spring.ai.retry.max-attempts=3
                        .build())
                .build();

        // Python: response = llm.invoke("你是谁？")
        String content = chatClient.prompt()
                .user("你是谁？")
                .call()
                .content();

        // Python: print(response.content)
        System.out.println(content);
    }
}

/*
【Java vs Python 对照】
Python ChatDeepSeek(api_key=..., base_url=...) ↔  Java OpenAiChatModel + application.properties base-url
Python temperature=0.8                          ↔  Java OpenAiChatOptions.builder().temperature(0.8)
Python max_tokens=1024                          ↔  Java .maxTokens(1024)
Python max_retries=3                            ↔  Java spring.ai.retry.max-attempts=3（properties）
Python response.content                         ↔  Java chatClient.prompt().call().content()

【注意】
Python ChatDeepSeek 是 LangChain 的 DeepSeek 原生 provider 类；
Java 因为 DeepSeek 兼容 OpenAI 接口，直接用 OpenAiChatModel + base-url 切换即可，无需单独依赖。
*/
