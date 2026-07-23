/**
 * 对应 Python：13.1-invoke/LLM_aBatch.py
 * 异步批量调用：await model.abatch(...)
 *
 * Python:
 *   async def main():
 *       questions = [
 *           [HumanMessage("什么是LangChain")],
 *           [HumanMessage("什么是AI")],
 *           [HumanMessage("什么是Python")],
 *       ]
 *       responses = await model.abatch(questions)
 *       for r in responses: print(r.content)
 *   asyncio.run(main())
 *
 * Python abatch 内部用 asyncio.gather() 并发执行所有请求。
 *
 * Java 对应：CompletableFuture.allOf() 或 Flux.merge() 实现等价效果。
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
public class LLM_aBatch {
    public static void main(String[] args) {
        SpringApplication.run(LLM_aBatch.class, args);
    }
}

@Component
class ABatchRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) throws Exception {
        ChatClient model = chatClientBuilder.build();

        // Python: questions = [[HumanMessage("...")], [HumanMessage("...")], ...]
        List<String> questions = List.of("什么是LangChain", "什么是AI", "什么是Python");

        // Python: responses = await model.abatch(questions)
        // Java: CompletableFuture 并发，等价于 asyncio.gather()
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        List<CompletableFuture<String>> futures = questions.stream()
                .map(q -> CompletableFuture.supplyAsync(
                        () -> model.prompt().user(q).call().content(),
                        executor
                ))
                .toList();

        // 等全部完成（等价 asyncio.gather()）
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Python: for r in responses: print(r.content)
        for (int i = 0; i < questions.size(); i++) {
            System.out.println("问：" + questions.get(i));
            System.out.println("答：" + futures.get(i).join());
            System.out.println();
        }

        executor.shutdown();
    }
}

/*
【Java vs Python 对照】
Python await model.abatch([msgs1, msgs2, msgs3])
  ↔ Java CompletableFuture.allOf(futures).join()（所有请求并发执行，等全部完成）

Python asyncio.gather(*coroutines)
  ↔ Java CompletableFuture.allOf(...)  或  Flux.merge(...)

Python for r in responses: print(r.content)
  ↔ Java futures.get(i).join()（按顺序取结果）

【为什么并发？】
abatch 和 asyncio.gather 的价值在于"同时发多个请求"，总时间 ≈ 最慢那条，不是各条之和。
CompletableFuture 在 Java 里实现了完全相同的并发效果。
*/
