/**
 * 对应 Python：StandardDesc.py
 * 企业级工程化写法：封装初始化、日志、异常处理、invoke + stream 两种调用方式
 *
 * Python 要点：
 *   - init_llm_client() 封装初始化逻辑
 *   - logger.info / logger.error 代替 print
 *   - try/except ValueError / LangChainException / Exception
 *   - llm.invoke("...").content          → 一次性返回
 *   - for chunk in llm.stream("..."): print(chunk.content, end="")  → 流式
 *
 * Java 对应：
 *   - @Service 封装业务逻辑（对应 init_llm_client 函数）
 *   - SLF4J + Logback 记日志（对应 Python logging）
 *   - try/catch 异常分类处理
 *   - chatClient.prompt().call().content()     → 一次性
 *   - chatClient.prompt().stream().content()   → 响应式流（Flux<String>）
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class StandardDesc {
    public static void main(String[] args) {
        SpringApplication.run(StandardDesc.class, args);
    }
}

// ========== Service 层：封装 LLM 调用逻辑（对应 Python 的 init_llm_client + main） ==========
@Service
class LlmService {

    // Python: logger = logging.getLogger(__name__)
    private static final Logger logger = LoggerFactory.getLogger(LlmService.class);

    // Spring 自动注入，配置读 application.properties（对应 Python 的 init_llm_client()）
    private final ChatClient chatClient;

    @Autowired
    public LlmService(ChatClient.Builder builder) {
        // 对应 Python: llm = ChatOpenAI(model=..., api_key=..., temperature=0.7, max_tokens=2048)
        this.chatClient = builder
                .defaultOptions(org.springframework.ai.openai.OpenAiChatOptions.builder()
                        .temperature(0.7)
                        .maxTokens(2048)
                        .build())
                .build();
        logger.info("LLM客户端初始化成功");  // 对应 Python: logger.info("LLM客户端初始化成功")
    }

    // 一次性调用（对应 Python 的 llm.invoke("...")）
    public String invoke(String question) {
        // Python: response = llm.invoke(question); return response.content
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    // 流式调用（对应 Python 的 for chunk in llm.stream("..."): print(chunk.content, end="")）
    // Java 用 Flux<String>（Project Reactor）表达流，是响应式的，不是简单的 for 循环
    public Flux<String> stream(String question) {
        return chatClient.prompt()
                .user(question)
                .stream()
                .content();   // 每个元素是一小块文本，等同于 Python 的 chunk.content
    }
}

// ========== Runner：程序入口，调用 Service（对应 Python 的 main()） ==========
@Component
class StandardRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StandardRunner.class);

    @Autowired
    private LlmService llmService;

    @Override
    public void run(String... args) {
        // ===== try/except → try/catch =====
        try {
            // ----- invoke（一次性） -----
            // Python: response = llm.invoke(question); logger.info(f"回答：{response.content}")
            String question = "你是谁";
            String answer = llmService.invoke(question);
            logger.info("问题：{}", question);
            logger.info("回答：{}", answer);

            // ----- stream（流式，打字机效果） -----
            System.out.println("==================== 以下是流式输出");
            System.out.println("*".repeat(50));

            // Python: for chunk in llm.stream("..."): print(chunk.content, end="")
            // Java:   Flux.subscribe()，每个元素到来时打印，不换行
            llmService.stream("介绍下 Spring AI，300字以内")
                    .subscribe(
                            chunk -> System.out.print(chunk),  // 每个 chunk 打印，不换行
                            err   -> logger.error("流式出错：{}", err.getMessage()),
                            ()    -> System.out.println()      // 流结束后换行
                    );

            // 注意：stream 是异步的，实际项目里需要 block() 或在 WebFlux 上下文里用
            // 这里为演示简化，真实代码用：
            // llmService.stream("...").collectList().block();

        } catch (IllegalArgumentException e) {
            // 对应 Python: except ValueError
            logger.error("配置错误：{}", e.getMessage());
        } catch (org.springframework.ai.retry.NonTransientAiException e) {
            // 对应 Python: except LangChainException（模型调用失败）
            logger.error("模型调用失败：{}", e.getMessage());
        } catch (Exception e) {
            // 对应 Python: except Exception
            logger.error("未知错误：{}", e.getMessage());
        }
    }
}

/*
【Java vs Python 对照】
Python logging.getLogger(__name__)     ↔  Java LoggerFactory.getLogger(ClassName.class)
Python logger.info(f"回答：{x}")       ↔  Java logger.info("回答：{}", x)
Python def init_llm_client()           ↔  Java @Service + 构造函数注入
Python llm.invoke("...").content       ↔  Java chatClient.prompt().user("...").call().content()
Python for chunk in llm.stream("...") ↔  Java Flux<String>.subscribe(chunk -> print(chunk))
Python except ValueError               ↔  Java catch (IllegalArgumentException e)
Python except LangChainException       ↔  Java catch (NonTransientAiException e)
Python if __name__ == "__main__"       ↔  Java main() + CommandLineRunner（Spring 入口）
*/
