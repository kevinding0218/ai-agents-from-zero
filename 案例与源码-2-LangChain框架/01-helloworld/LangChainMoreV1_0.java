/**
 * 对应 Python：LangChain_MoreV1.0.py
 * 多模型共存：同一应用里同时使用通义 + DeepSeek
 *
 * Python:
 *   llm_qwen     = init_chat_model(model="qwen-plus",       model_provider="openai", ...)
 *   llm_deepseek = init_chat_model(model="deepseek-v4-flash", model_provider="deepseek", ...)
 *   print(llm_qwen.invoke("你是谁").content)
 *   print(llm_deepseek.invoke("你是谁").content)
 *
 * Java 思路：
 *   用 @Bean 手动定义两个 ChatModel，用 @Qualifier 在注入时区分。
 *   application.properties 里分别配好两组 api-key / base-url。
 *
 * application.properties：
 *   # 通义（阿里百炼 OpenAI 兼容接口）
 *   qwen.api-key=${QWEN_API_KEY}
 *   qwen.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
 *   qwen.model=qwen-plus
 *
 *   # DeepSeek 官方接口
 *   deepseek.api-key=${DEEPSEEK_API_KEY}
 *   deepseek.base-url=https://api.deepseek.com
 *   deepseek.model=deepseek-chat
 */

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class LangChainMoreV1_0 {
    public static void main(String[] args) {
        SpringApplication.run(LangChainMoreV1_0.class, args);
    }
}

// ========== 手动定义两个 ChatModel Bean ==========
@Configuration
class MultiModelConfig {

    // 通义模型（对应 Python 的 llm_qwen）
    @Bean("qwenModel")
    public ChatModel qwenModel(
            @Value("${qwen.api-key}") String apiKey,
            @Value("${qwen.base-url}") String baseUrl,
            @Value("${qwen.model}") String model) {

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
    }

    // DeepSeek 模型（对应 Python 的 llm_deepseek）
    // DeepSeek 官方接口也是 OpenAI 兼容格式，所以同样用 OpenAiChatModel
    @Bean("deepseekModel")
    public ChatModel deepseekModel(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url}") String baseUrl,
            @Value("${deepseek.model}") String model) {

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
    }
}

// ========== 同时使用两个模型 ==========
@Component
class MultiModelRunner implements CommandLineRunner {

    // @Qualifier 指定用哪个 Bean（对应 Python 里的变量名区分）
    @Autowired @Qualifier("qwenModel")
    private ChatModel llmQwen;

    @Autowired @Qualifier("deepseekModel")
    private ChatModel llmDeepseek;

    @Override
    public void run(String... args) {
        // Python: print(llm_qwen.invoke("你是谁").content)
        String qwenReply = llmQwen
                .call(new org.springframework.ai.chat.messages.UserMessage("你是谁"))
                .getResult().getOutput().getText();
        System.out.println("通义回复：" + qwenReply);

        System.out.println("*".repeat(70));

        // Python: print(llm_deepseek.invoke("你是谁").content)
        String deepseekReply = llmDeepseek
                .call(new org.springframework.ai.chat.messages.UserMessage("你是谁"))
                .getResult().getOutput().getText();
        System.out.println("DeepSeek 回复：" + deepseekReply);
    }
}

/*
【Java vs Python 对照】
Python llm_qwen / llm_deepseek  ↔  Java @Bean("qwenModel") / @Bean("deepseekModel")
Python 变量名区分多实例           ↔  Java @Qualifier("beanName") 区分注入
Python .env 里的多组 KEY         ↔  Java application.properties 里的多组前缀配置
*/
