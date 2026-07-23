/**
 * 对应 Python：LCEL_RunnableParallelDemo.py
 * 并行链：同一输入同时跑多条子链，汇总结果为 dict
 *
 * Python:
 *   chain1 = prompt1 | model | parser1   # 中文介绍
 *   chain2 = prompt2 | model | parser2   # 英文介绍
 *
 *   parallel_chain = RunnableParallel({"chinese": chain1, "english": chain2})
 *   result = parallel_chain.invoke({"topic": "langchain"})
 *   # result = {"chinese": "LangChain 是...", "english": "LangChain is..."}
 *
 *   parallel_chain.get_graph().print_ascii()  # 打印 ASCII 流程图
 *
 * Java 对应：
 *   CompletableFuture.allOf() 并发两条链，结果合并为 Map。
 *   等价 RunnableParallel 的并发效果。
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class LCEL_RunnableParallelDemo {
    public static void main(String[] args) {
        SpringApplication.run(LCEL_RunnableParallelDemo.class, args);
    }
}

@Component
class ParallelRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ParallelRunner.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) throws Exception {
        ChatClient model = chatClientBuilder.build();

        // Python: topic = "langchain"
        String topic = "langchain";

        // ─── Python 子链 1：中文介绍 ────────────────────────────────────────
        // Python: prompt1 = ChatPromptTemplate([("system","请用中文简短回答"), ("human","介绍{topic}")])
        //         chain1 = prompt1 | model | parser1
        // Java:   lambda 封装子链（等价 chain1）
        var chain1 = (java.util.function.Supplier<String>) () ->
                model.prompt()
                        .system("你是一个知识渊博的计算机专家，请用中文简短回答")
                        .user("请简短介绍什么是" + topic)
                        .call()
                        .content();

        // ─── Python 子链 2：英文介绍 ────────────────────────────────────────
        // Python: prompt2 = ChatPromptTemplate([("system","请用英文简短回答"), ...])
        //         chain2 = prompt2 | model | parser2
        var chain2 = (java.util.function.Supplier<String>) () ->
                model.prompt()
                        .system("你是一个知识渊博的计算机专家，请用英文简短回答")
                        .user("请简短介绍什么是" + topic)
                        .call()
                        .content();

        // ─── Python: parallel_chain = RunnableParallel({"chinese": chain1, "english": chain2}) ──
        // Java: CompletableFuture 并发两条链
        System.out.println("【RunnableParallel 等价：并发两条子链】");

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        // Python: result = parallel_chain.invoke({"topic": "langchain"})
        // Java: 同时发两个请求，等全部完成，合并结果
        CompletableFuture<String> futureChinese = CompletableFuture.supplyAsync(chain1, executor);
        CompletableFuture<String> futureEnglish = CompletableFuture.supplyAsync(chain2, executor);

        CompletableFuture.allOf(futureChinese, futureEnglish).join();

        // Python: result = {"chinese": "LangChain 是...", "english": "LangChain is..."}
        Map<String, String> result = new LinkedHashMap<>();
        result.put("chinese", futureChinese.join());
        result.put("english", futureEnglish.join());

        // Python: logger.info(result)
        logger.info("并行结果：{}", result);

        executor.shutdown();

        System.out.println("\n" + "=".repeat(50));

        // Python: parallel_chain.get_graph().print_ascii()  ← 打印数据流图
        // Java: 手动描述结构（Spring AI 没有内置 ASCII 图打印）
        System.out.println("【并行链 ASCII 图（手动描述，等价 get_graph().print_ascii()）】");
        System.out.println("""
                            +-----------------------------+
                            | Parallel<chinese,english>Input |
                            +-----------------------------+
                                   ***           ***
                                ***                 ***
                    +--------------------+    +--------------------+
                    | system: 中文       |    | system: 英文       |
                    +--------------------+    +--------------------+
                               *                          *
                    +-----------+                +-----------+
                    | ChatModel |                | ChatModel |
                    +-----------+                +-----------+
                               *                          *
                    +-----------+                +-----------+
                    | .content()|                | .content()|
                    +-----------+                +-----------+
                                   ***           ***
                            +------------------------------+
                            | Parallel<chinese,english>Output |
                            +------------------------------+
                """);
    }
}

/*
【Java vs Python RunnableParallel 对照】
Python RunnableParallel({"chinese": chain1, "english": chain2})
  ↔ Java CompletableFuture.supplyAsync(chain1) + CompletableFuture.supplyAsync(chain2)

Python result = parallel_chain.invoke({"topic": "langchain"})
  → result = {"chinese": "...", "english": "..."}
  ↔ Java CompletableFuture.allOf(f1, f2).join()
          result = Map.of("chinese", f1.join(), "english", f2.join())

Python parallel_chain.get_graph().print_ascii()
  ↔ Java 手动描述（Spring AI 无内置图打印）

【性能原理】
Python RunnableParallel 内部用 asyncio 并发执行多条子链。
Java CompletableFuture 用线程池（或虚拟线程）并发，效果完全一致。
总时间 ≈ max(chain1_time, chain2_time)，而不是两者之和。
*/
