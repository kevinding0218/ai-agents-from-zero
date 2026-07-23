/**
 * 对应 Python：LangChainV0.3.py
 * 旧式写法：直接实例化 OpenAiChatModel，手动传入 api_key / base_url
 *
 * Python 旧式：
 *   llm = ChatOpenAI(model="...", api_key="...", base_url="...")
 *
 * Java 对应：
 *   直接 new OpenAiChatModel(api, options)，不走 Spring 自动配置
 *   → 等价于 Python 的"硬编码 / 手动配置"写法
 *
 * 依赖：spring-ai-openai-spring-boot-starter
 */

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

public class LangChainV0_3 {

    public static void main(String[] args) {

        // ========== 第 1 版：硬编码（不推荐，API Key 会进代码库）==========
        // Python:
        //   llm = ChatOpenAI(model="qwen-plus", api_key="sk-xxx", base_url="...")

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-你自己的key")   // ← 不推荐，演示用
                .build();

        OpenAiChatModel llm = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-v3.2")
                        .build())
                .build();

        // ========== 第 2 版（推荐）：从环境变量读取 ==========
        // Python:
        //   api_key = os.getenv("QWEN_API_KEY")

        String apiKey = System.getenv("QWEN_API_KEY");   // 对应 os.getenv("QWEN_API_KEY")

        OpenAiApi apiFromEnv = OpenAiApi.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey(apiKey)
                .build();

        OpenAiChatModel llmFromEnv = OpenAiChatModel.builder()
                .openAiApi(apiFromEnv)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("deepseek-v3.2")
                        .build())
                .build();

        // ========== 调用：invoke 对应 llm.invoke("你是谁") ==========
        // Python:
        //   response = llm.invoke("你是谁")
        //   print(response)          # 完整对象
        //   print(response.content)  # 只取正文

        var response = llmFromEnv.call(
                new org.springframework.ai.chat.messages.UserMessage("你是谁")
        );

        System.out.println(response);                          // 完整对象（含元数据）
        System.out.println(response.getResult().getOutput().getText());  // 只取正文
    }
}

/*
【Java vs Python 对照】
Python ChatOpenAI(...)         ↔  Java OpenAiChatModel.builder()...build()
Python os.getenv("KEY")        ↔  Java System.getenv("KEY")
Python response.content        ↔  Java response.getResult().getOutput().getText()
Python print(response)         ↔  Java System.out.println(response)
*/
