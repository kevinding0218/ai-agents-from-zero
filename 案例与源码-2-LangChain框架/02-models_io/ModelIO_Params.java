/**
 * 对应 Python：ModelIO_Params.py
 * 模型参数：temperature / max_tokens；循环调用以观察随机性
 *
 * Python:
 *   llm = ChatOpenAI(
 *       model="qwen-plus",
 *       temperature=0.9,   # 随机性（0=确定，1=最随机）
 *       max_tokens=200,
 *       ...
 *   )
 *   for i in range(3):
 *       response = llm.invoke("用3行诗描述春天")
 *       print(response)          # 打印完整 AIMessage 对象
 *       print(response.content)  # 打印文本内容
 *
 * Python 中 print(response) 会打印 AIMessage 对象的所有字段（content, additional_kwargs, usage_metadata 等）。
 *
 * application.properties：
 *   spring.ai.openai.api-key=${QWEN_API_KEY}
 *   spring.ai.openai.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
 *   spring.ai.openai.chat.options.model=qwen-plus
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class ModelIO_Params {
    public static void main(String[] args) {
        SpringApplication.run(ModelIO_Params.class, args);
    }
}

@Component
class ParamsRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        // Python: llm = ChatOpenAI(temperature=0.9, max_tokens=200)
        ChatClient llm = chatClientBuilder
                .defaultOptions(OpenAiChatOptions.builder()
                        .temperature(0.9)   // 随机性：0=确定，1=最随机
                        .maxTokens(200)
                        .build())
                .build();

        // Python: for i in range(3): response = llm.invoke(...)
        for (int i = 0; i < 3; i++) {
            System.out.println("=".repeat(50));
            System.out.println("第 " + (i + 1) + " 次调用");

            ChatResponse response = llm.prompt()
                    .user("用3行诗描述春天")
                    .call()
                    .chatResponse();

            // Python: print(response)  → 打印完整 AIMessage 对象（含所有字段）
            System.out.println("完整响应：" + response);

            // Python: print(response.content) → 只打印文本
            String content = response.getResult().getOutput().getText();
            System.out.println("文本内容：" + content);

            // Python: response.usage_metadata → token 用量
            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                System.out.printf("[token 用量] 输入: %d  输出: %d  总计: %d%n",
                        usage.getPromptTokens(),
                        usage.getGenerationTokens(),
                        usage.getTotalTokens());
            }
        }
    }
}

/*
【Java vs Python 对照】
Python temperature=0.9           ↔  Java OpenAiChatOptions.builder().temperature(0.9)
Python max_tokens=200             ↔  Java .maxTokens(200)
Python for i in range(3)          ↔  Java for (int i = 0; i < 3; i++)
Python print(response)            ↔  Java System.out.println(chatResponse)（打印对象 toString）
Python print(response.content)    ↔  Java response.getResult().getOutput().getText()
Python response.usage_metadata    ↔  Java response.getMetadata().getUsage()

【temperature 说明】
temperature 越高，同一问题每次回答越不同（随机性越大）。
循环 3 次调用可直观看到 temperature=0.9 带来的输出差异。
*/
