/**
 * 对应 Python：ModelIO_Qwen.py
 * 通义 Qwen 原生接口（ChatTongyi）：invoke + stream
 *
 * Python:
 *   from langchain_community.chat_models import ChatTongyi
 *   llm = ChatTongyi(model="qwen-plus", dashscope_api_key=os.getenv("aliQwen-api"))
 *
 *   # invoke（一次性）
 *   messages = [HumanMessage(content="你是谁？")]
 *   response = llm.invoke(messages)
 *   print(response.content)
 *
 *   # stream（流式）
 *   for chunk in llm.stream(messages):
 *       print(chunk.content, end="", flush=True)
 *
 * Java 对应：
 *   通义（阿里百炼）的接口与 OpenAI 完全兼容，Spring AI 用 OpenAiChatModel + base-url 覆盖。
 *   Python ChatTongyi 不需要手动配置 base_url（它内置了），Java 用 OpenAI 兼容接口等价。
 *
 * application.properties：
 *   spring.ai.openai.api-key=${QWEN_API_KEY}
 *   spring.ai.openai.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
 *   spring.ai.openai.chat.options.model=qwen-plus
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class ModelIO_Qwen {
    public static void main(String[] args) {
        SpringApplication.run(ModelIO_Qwen.class, args);
    }
}

@Component
class QwenRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(QwenRunner.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient llm = chatClientBuilder.build();

        // ─── invoke（一次性）───────────────────────────────────────────
        // Python: messages = [HumanMessage(content="你是谁？")]
        //         response = llm.invoke(messages)
        String response = llm.prompt()
                .user("你是谁？")
                .call()
                .content();

        // Python: print(response.content)
        logger.info("invoke 结果：{}", response);

        System.out.println("=".repeat(50));

        // ─── stream（流式，打字机效果）─────────────────────────────────
        // Python: for chunk in llm.stream(messages): print(chunk.content, end="", flush=True)
        // Java:   Flux<String>，每个元素是一小块文本
        logger.info("stream 流式输出：");
        llm.prompt()
                .user("你是谁？")
                .stream()
                .content()
                .subscribe(
                        chunk -> System.out.print(chunk),   // 打字机效果，不换行
                        err   -> logger.error("流式出错：{}", err.getMessage()),
                        ()    -> System.out.println()       // 流结束换行
                );
        // 注：subscribe 是异步的，真实项目中用 .collectList().block() 等待完成
    }
}

/*
【Java vs Python 对照】
Python ChatTongyi(dashscope_api_key=...)      ↔  Java application.properties 配通义 base-url + api-key
Python llm.invoke([HumanMessage("...")])      ↔  Java chatClient.prompt().user("...").call().content()
Python response.content                       ↔  Java .content()（ChatClient 高级 API 直接返回字符串）
Python for chunk in llm.stream(messages)      ↔  Java .stream().content()（返回 Flux<String>）
Python print(chunk.content, end="")          ↔  Java .subscribe(chunk -> System.out.print(chunk))

【注意】
Python ChatTongyi 是通义专属 provider（不需要手动配 base_url）。
Java 用 OpenAI 兼容接口覆盖 base-url，功能完全等价。
*/
