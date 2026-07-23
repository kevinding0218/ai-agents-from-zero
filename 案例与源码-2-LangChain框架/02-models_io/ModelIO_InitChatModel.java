/**
 * 对应 Python：ModelIO_Init_chat_model.py
 * 工厂函数：通过 model_provider 字符串统一创建不同 provider 的模型
 *
 * Python:
 *   llm = init_chat_model(
 *       model="qwen2.5:4b",
 *       model_provider="ollama",
 *       base_url="http://localhost:11434",
 *   )
 *   # 支持 5 种 provider：openai / deepseek / tongyi / ollama / huggingface
 *
 * Java 对应思路：
 *   Python 的 init_chat_model 是一个运行时 provider 工厂，根据字符串决定实例化哪个类。
 *   Java 推荐用 Spring 配置 + @Bean 多实例来实现，也可以写一个工厂方法。
 *
 * application.properties（多 provider 示例）：
 *   # Ollama（本地）
 *   spring.ai.ollama.base-url=http://localhost:11434
 *   spring.ai.ollama.chat.options.model=qwen2.5:4b
 *
 *   # OpenAI 兼容（通义）
 *   qwen.api-key=${QWEN_API_KEY}
 *   qwen.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
 *   qwen.model=qwen-plus
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class ModelIO_InitChatModel {
    public static void main(String[] args) {
        SpringApplication.run(ModelIO_InitChatModel.class, args);
    }
}

// ========== Java 工厂方法（等价于 Python 的 init_chat_model）==========
// Python: llm = init_chat_model(model=..., model_provider=..., ...)
// Java:   工厂方法根据 provider 字符串创建对应 ChatModel
class ChatModelFactory {

    public static ChatModel create(String provider, String model, String apiKey, String baseUrl) {
        return switch (provider) {
            // Python: model_provider="openai" 或 "tongyi"（阿里云兼容接口）
            case "openai", "tongyi" -> {
                OpenAiApi api = OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build();
                yield OpenAiChatModel.builder()
                        .openAiApi(api)
                        .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                        .build();
            }
            // Python: model_provider="deepseek"
            case "deepseek" -> {
                OpenAiApi api = OpenAiApi.builder()
                        .baseUrl("https://api.deepseek.com")
                        .apiKey(apiKey)
                        .build();
                yield OpenAiChatModel.builder()
                        .openAiApi(api)
                        .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                        .build();
            }
            // Python: model_provider="ollama"
            // Ollama 用 Spring AI 的 OllamaChatModel（通过自动配置或手动构建）
            case "ollama" -> throw new UnsupportedOperationException(
                    "Ollama: 请用 spring-ai-ollama-spring-boot-starter 自动配置，" +
                    "或手动构建 OllamaChatModel");
            // Python: model_provider="huggingface"
            case "huggingface" -> {
                OpenAiApi api = OpenAiApi.builder()
                        .baseUrl("https://api-inference.huggingface.co/models/" + model + "/v1")
                        .apiKey(apiKey)
                        .build();
                yield OpenAiChatModel.builder()
                        .openAiApi(api)
                        .defaultOptions(OpenAiChatOptions.builder().model("tgi").build())
                        .build();
            }
            default -> throw new IllegalArgumentException("不支持的 provider: " + provider);
        };
    }
}

@Component
class InitModelRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // Python: llm = init_chat_model(model="qwen2.5:4b", model_provider="ollama", ...)
        // Java: 只演示 Qwen（Ollama 需要额外 starter，这里用 Qwen 通义代替）
        String apiKey = System.getenv("QWEN_API_KEY");
        ChatModel llm = ChatModelFactory.create(
                "openai",
                "qwen-plus",
                apiKey,
                "https://dashscope.aliyuncs.com/compatible-mode/v1"
        );

        // Python: response = llm.invoke("你是谁？")
        ChatClient chatClient = ChatClient.create(llm);
        String content = chatClient.prompt().user("你是谁？").call().content();

        // Python: print(response.content)
        System.out.println("模型回答：" + content);
    }
}

/*
【Java vs Python 对照】
Python init_chat_model(model=..., model_provider="openai", ...)
  ↔ Java ChatModelFactory.create("openai", model, apiKey, baseUrl)

Python model_provider="tongyi"   ↔ Java "openai" + 通义 base-url（OpenAI 兼容）
Python model_provider="deepseek" ↔ Java "deepseek" + DeepSeek base-url
Python model_provider="ollama"   ↔ Java spring-ai-ollama-spring-boot-starter（自动配置）
Python model_provider="huggingface" ↔ Java "openai" + HuggingFace Inference API base-url

【关键差异】
Python init_chat_model 是 LangChain 的内置工厂，根据字符串自动选 class。
Java 没有等价的内置工厂，需要手动写 switch 或用 Spring @Bean 配置多个实例。
*/
