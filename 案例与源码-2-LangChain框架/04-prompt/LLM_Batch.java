/**
 * 对应 Python：13.1-invoke/LLM_Batch.py
 * 批量调用：顺序 invoke vs batch()；对比时间差异
 *
 * Python:
 *   questions = ["什么是LangChain", "什么是AI", "什么是Python"]
 *
 *   # 顺序（串行）：逐条调用，总时间 = Σ 各条时间
 *   for q in questions:
 *       model.invoke([HumanMessage(q)])
 *
 *   # batch()（LangChain 内部可并发）：总时间 ≈ 最慢那条
 *   model.batch([[HumanMessage(q)] for q in questions])
 *
 * Java 对应：
 *   Java 没有 batch() 直接对应，但可以用 CompletableFuture.allOf() 或 ExecutorService 并发调用。
 *   Spring WebFlux 的 Mono/Flux 也可以并发多个请求。
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class LLM_Batch {
    public static void main(String[] args) {
        SpringApplication.run(LLM_Batch.class, args);
    }
}

@Component
class BatchRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) throws Exception {
        ChatClient model = chatClientBuilder.build();

        // Python: questions = ["什么是LangChain", "什么是AI", "什么是Python"]
        List<String> questions = List.of("什么是LangChain", "什么是AI", "什么是Python");

        // ─── 顺序调用（等价 Python 的 for q in questions: model.invoke(...)）────
        System.out.println("=".repeat(50));
        System.out.println("【顺序调用】");
        long start = System.currentTimeMillis();

        for (String q : questions) {
            String content = model.prompt().user(q).call().content();
            System.out.println("问：" + q + "\n答：" + content + "\n");
        }
        System.out.println("顺序用时：" + (System.currentTimeMillis() - start) + " ms");

        // ─── 并发调用（等价 Python 的 model.batch(...)）──────────────────────
        System.out.println("=".repeat(50));
        System.out.println("【并发调用（等价 batch）】");
        long start2 = System.currentTimeMillis();

        // Python: model.batch([[HumanMessage(q)] for q in questions])
        // Java: CompletableFuture 并发，等价于 batch() 的并发效果
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<CompletableFuture<String>> futures = questions.stream()
                .map(q -> CompletableFuture.supplyAsync(
                        () -> model.prompt().user(q).call().content(),
                        executor
                ))
                .toList();

        // 等待全部完成，等价 Python batch() 等全部结果
        List<String> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        for (int i = 0; i < questions.size(); i++) {
            System.out.println("问：" + questions.get(i) + "\n答：" + results.get(i) + "\n");
        }
        System.out.println("并发用时：" + (System.currentTimeMillis() - start2) + " ms");

        executor.shutdown();
    }
}

/*
【Java vs Python 对照】
Python model.invoke([HumanMessage(q)])    ↔  Java model.prompt().user(q).call().content()
Python model.batch([msgs1, msgs2, ...])   ↔  Java CompletableFuture.allOf(futures)
Python for q in questions                 ↔  Java for (String q : questions) 或 stream().map()

【性能原理】
顺序：总时间 = T1 + T2 + T3
并发：总时间 ≈ max(T1, T2, T3)
Python batch() 和 Java CompletableFuture 都利用了这个并发优势。
Java 21+ 的虚拟线程（Executors.newVirtualThreadPerTaskExecutor）使并发开销极小。
*/
