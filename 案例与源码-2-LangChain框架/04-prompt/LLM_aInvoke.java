/**
 * 对应 Python：13.1-invoke/LLM_aInvoke.py
 * 异步调用：ainvoke() 与 asyncio
 *
 * Python:
 *   import asyncio
 *   async def main():
 *       response = await model.ainvoke([HumanMessage("你是谁？")])
 *       print(response.content)
 *   asyncio.run(main())
 *
 * Java 对应：
 *   Spring AI 的 ChatClient 基于 Project Reactor，流式 API 天生异步。
 *   非流式的 .call().content() 在当前线程阻塞（等价 asyncio.run 阻塞等结果）。
 *   真正异步可用 CompletableFuture 或直接返回 Mono/Flux。
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class LLM_aInvoke {
    public static void main(String[] args) {
        SpringApplication.run(LLM_aInvoke.class, args);
    }
}

@Component
class AInvokeRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) throws Exception {
        ChatClient model = chatClientBuilder.build();

        // ─── 方式 A：CompletableFuture（最直接类比 asyncio）────────────
        // Python: response = await model.ainvoke([HumanMessage("你是谁？")])
        System.out.println("【方式 A：CompletableFuture 异步（类比 asyncio）】");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(
                () -> model.prompt().user("你是谁？").call().content()
        );

        // Python: asyncio.run(main()) 阻塞等结果
        String result = future.get();
        System.out.println("异步结果：" + result);

        // ─── 方式 B：Reactor Mono（Spring 响应式风格）──────────────────
        // Python: await model.ainvoke(...) 返回协程
        // Java:   Mono<String> 是"延迟计算"的包装，subscribe 时才真正执行
        System.out.println("\n【方式 B：Mono 响应式（Spring WebFlux 风格）】");

        Mono<String> responseMono = Mono.fromCallable(
                () -> model.prompt().user("你是谁？").call().content()
        ).subscribeOn(Schedulers.boundedElastic());

        // .block() 等价于 asyncio.run() 的阻塞等结果
        String result2 = responseMono.block();
        System.out.println("Mono 结果：" + result2);
    }
}

/*
【Java vs Python 对照】
Python import asyncio                     ↔  Java import reactor.core / CompletableFuture
Python async def main()                   ↔  Java CompletableFuture.supplyAsync(...)
Python await model.ainvoke(messages)      ↔  Java future.get() 或 Mono.block()
Python asyncio.run(main())                ↔  Java future.get()（阻塞等完成）

【Python asyncio vs Java 异步模型差异】
Python: asyncio 是协程（coroutine），单线程事件循环，await 挂起当前任务
Java:   CompletableFuture 基于线程池，Reactor Mono 基于事件循环
两者效果上都能"异步发起请求 + 等待结果"，只是实现机制不同。
*/
