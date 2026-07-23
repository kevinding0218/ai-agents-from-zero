/**
 * 对应 Python：LangChain_huggingface_chat_model_remote.py
 * HuggingFace 远程 API：调用 HuggingFace 云端聊天模型，本机不需要 GPU
 *
 * Python:
 *   model = init_chat_model(
 *       model="Qwen/Qwen2.5-7B-Instruct",
 *       model_provider="huggingface",
 *       backend="endpoint",
 *       huggingfacehub_api_token=os.getenv("HF_TOKEN"),
 *   )
 *   response = model.invoke("你是谁")
 *   print(response.content)
 *
 * Java 对应：
 *   Spring AI 有 HuggingFace 集成，通过 OpenAI 兼容的 Inference API 调用，
 *   HuggingFace 的大部分模型支持 OpenAI 兼容端点。
 *
 * 依赖（pom.xml）：
 *   <dependency>
 *       <groupId>org.springframework.ai</groupId>
 *       <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
 *   </dependency>
 *
 * application.properties：
 *   spring.ai.openai.api-key=${HF_TOKEN}
 *   spring.ai.openai.base-url=https://api-inference.huggingface.co/models/Qwen/Qwen2.5-7B-Instruct/v1
 *   spring.ai.openai.chat.options.model=tgi   # HuggingFace Inference API 的模型名固定为 tgi
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@SpringBootApplication
public class HuggingFaceRemote {
    public static void main(String[] args) {
        SpringApplication.run(HuggingFaceRemote.class, args);
    }
}

@Component
class HuggingFaceChatRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient chatClient = chatClientBuilder.build();

        System.out.println("模型已就绪，输入问题开始对话（输入 quit 或 exit 退出）");
        System.out.println("-".repeat(50));

        // Python: while True: user_input = input("你：").strip()
        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            System.out.print("你：");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("quit") || userInput.equalsIgnoreCase("exit")) {
                System.out.println("再见！");
                break;
            }

            if (userInput.isEmpty()) continue;  // 防止空输入

            // Python: response = model.invoke(user_input)
            var response = chatClient.prompt()
                    .user(userInput)
                    .call()
                    .chatResponse();

            // Python: print(f"模型回复正文：{response.content}")
            System.out.println("模型回复正文：" + response.getResult().getOutput().getText());

            // Python: print(f"LangChain 统一整理后的用量信息：{response.usage_metadata}")
            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                System.out.printf("[token 消耗] 输入: %d  输出: %d  合计: %d%n",
                        usage.getPromptTokens(),
                        usage.getGenerationTokens(),
                        usage.getTotalTokens());
            }
            System.out.println("-".repeat(50));
        }
    }
}

/*
【Java vs Python 对照】
Python huggingfacehub_api_token=...   ↔  Java application.properties api-key=${HF_TOKEN}
Python model.invoke(user_input)       ↔  Java chatClient.prompt().user(input).call().chatResponse()
Python response.content               ↔  Java response.getResult().getOutput().getText()
Python response.usage_metadata        ↔  Java response.getMetadata().getUsage()
Python while True + input()           ↔  Java while + Scanner.nextLine()

【注意】
HuggingFace 的 base-url 格式：
  https://api-inference.huggingface.co/models/{model-id}/v1
  这是 HuggingFace 的 OpenAI 兼容端点，Spring AI 用 OpenAiChatModel 就能接
*/
