/**
 * 对应 Python：13.1-invoke/LLM_Stream.py
 * 流式输出：打字机效果 + chunk 检查两种模式
 *
 * Python:
 *   # 模式一：打字机效果
 *   for chunk in model.stream([HumanMessage("你是谁？")]):
 *       print(chunk.content, end="", flush=True)
 *
 *   # 模式二：检查每个 chunk 的元数据
 *   for chunk in model.stream([HumanMessage("你是谁？")]):
 *       print(f"chunk type: {type(chunk)}")
 *       print(f"chunk content: {chunk.content}")
 *
 * Java 对应：
 *   Spring AI stream() 返回 Flux<String>（纯文本流）或 Flux<ChatResponse>（含元数据）。
 *   打字机效果用 .stream().content().subscribe()。
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class LLM_Stream {
    public static void main(String[] args) {
        SpringApplication.run(LLM_Stream.class, args);
    }
}

@Component
class StreamRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) throws InterruptedException {
        ChatClient model = chatClientBuilder.build();

        // ─── 模式一：打字机效果 ─────────────────────────────────────────
        // Python: for chunk in model.stream(...): print(chunk.content, end="", flush=True)
        System.out.println("【模式一：打字机效果】");
        CountDownLatch latch1 = new CountDownLatch(1);

        model.prompt()
                .user("你是谁？")
                .stream()
                .content()   // Flux<String>，每个元素是一小块文本
                .subscribe(
                        chunk -> System.out.print(chunk),          // 不换行
                        err   -> System.err.println("出错：" + err),
                        ()    -> { System.out.println(); latch1.countDown(); }
                );

        latch1.await();  // 等待流完成

        System.out.println("\n" + "=".repeat(50));

        // ─── 模式二：检查每个 chunk 的内容 ──────────────────────────────
        // Python: for chunk in model.stream(...):
        //             print(f"chunk type: {type(chunk)}")
        //             print(f"chunk content: {chunk.content}")
        System.out.println("【模式二：chunk 检查】");
        CountDownLatch latch2 = new CountDownLatch(1);

        model.prompt()
                .user("你是谁？")
                .stream()
                .chatResponse()  // Flux<ChatResponse>，可访问完整 chunk 对象
                .subscribe(
                        chunk -> {
                            String text = chunk.getResult().getOutput().getText();
                            System.out.println("chunk 类型：" + chunk.getClass().getSimpleName());
                            System.out.println("chunk 内容：" + text);
                        },
                        err -> System.err.println("出错：" + err),
                        latch2::countDown
                );

        latch2.await();
    }
}

/*
【Java vs Python 对照】
Python for chunk in model.stream(messages)
  ↔ Java model.prompt().stream().content().subscribe(chunk -> ...)

Python print(chunk.content, end="")
  ↔ Java .subscribe(chunk -> System.out.print(chunk))

Python type(chunk)      ↔  Java chunk.getClass().getSimpleName()
Python chunk.content    ↔  Java chunk.getResult().getOutput().getText()（chatResponse 流）

【注意】
Python stream() 是同步生成器（可直接 for 循环）。
Java stream() 返回 Flux<T>（响应式），需要用 subscribe() 或 block()。
真实项目里用 .collectList().block() 等待全部完成，或在 WebFlux 上下文里直接返回 Flux。
*/
