/**
 * 对应 Python：13.1-invoke/LLM_aStream.py
 * 异步流式：async for chunk in model.astream(...)
 *
 * Python:
 *   async def main():
 *       async for chunk in model.astream([HumanMessage("你是谁？")]):
 *           print(chunk.content, end="", flush=True)
 *   asyncio.run(main())
 *
 * Java 对应：
 *   Spring AI 的 .stream() 返回 Flux<String>，本质上就是异步流。
 *   subscribe() 异步处理每个 chunk，与 Python 的 async for 完全等价。
 *   等待流完成用 .blockLast() 或 CountDownLatch。
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class LLM_aStream {
    public static void main(String[] args) {
        SpringApplication.run(LLM_aStream.class, args);
    }
}

@Component
class AStreamRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient model = chatClientBuilder.build();

        // Python: async for chunk in model.astream([HumanMessage("你是谁？")]):
        //             print(chunk.content, end="", flush=True)
        // Java:   Flux<String>.subscribe() 异步处理每个 chunk
        System.out.println("【异步流式输出（等价 Python 的 async for）】");

        // .blockLast() 阻塞直到流结束（相当于 asyncio.run 等待协程完成）
        model.prompt()
                .user("你是谁？")
                .stream()
                .content()
                .doOnNext(chunk -> System.out.print(chunk))   // 每个 chunk 打印，不换行
                .doOnComplete(() -> System.out.println())     // 流结束换行
                .blockLast();                                  // 等待流完成（commandline 场景）
    }
}

/*
【Java vs Python 对照】
Python async for chunk in model.astream(messages)
  ↔ Java .stream().content().doOnNext(chunk -> ...)

Python print(chunk.content, end="", flush=True)
  ↔ Java doOnNext(chunk -> System.out.print(chunk))

Python asyncio.run(main())（等待协程结束）
  ↔ Java .blockLast()（等待 Flux 流完成）

【注意】
Python astream 是异步生成器，await 一次返回一个 chunk，整个 async for 在事件循环里运行。
Java Flux 是响应式流，doOnNext/subscribe 回调在 IO 线程上执行，blockLast() 阻塞调用线程等待完成。
在 Spring WebFlux Controller 里可以直接 return Flux<String>，不需要 blockLast()。
*/
