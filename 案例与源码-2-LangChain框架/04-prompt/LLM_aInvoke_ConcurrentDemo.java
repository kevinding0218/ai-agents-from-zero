/**
 * 对应 Python：13.1-invoke/LLM_aInvoke_ConcurrentDemo.py
 * 顺序 invoke 对比 asyncio.gather() 并发；本地 Ollama vs 远程 HuggingFace
 *
 * Python:
 *   # 顺序
 *   for m in [ollama_model, hf_model]:
 *       response = m.invoke([HumanMessage("你是谁？")])
 *
 *   # 并发（asyncio.gather 同时发两个请求）
 *   results = await asyncio.gather(
 *       ollama_model.ainvoke([HumanMessage("你是谁？")]),
 *       hf_model.ainvoke([HumanMessage("你是谁？")]),
 *   )
 *
 * Java 对应：CompletableFuture.allOf 实现完全相同的并发效果。
 *
 * application.properties：
 *   # Ollama 本地
 *   spring.ai.ollama.base-url=http://localhost:11434
 *   spring.ai.ollama.chat.options.model=qwen3:4b
 *   # HuggingFace 远程（在代码里手动构建第二个 ChatModel）
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
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
public class LLM_aInvoke_ConcurrentDemo {
    public static void main(String[] args) {
        SpringApplication.run(LLM_aInvoke_ConcurrentDemo.class, args);
    }
}

@Component
class ConcurrentDemoRunner implements CommandLineRunner {

    @Autowired
    private OllamaChatModel ollamaChatModel;  // 本地 Ollama

    @Override
    public void run(String... args) throws Exception {
        // 本地 Ollama 模型
        ChatClient ollamaClient = ChatClient.create(ollamaChatModel);

        // HuggingFace 远程模型（手动构建）
        // Python: hf_model = init_chat_model(model_provider="huggingface", backend="endpoint", ...)
        OpenAiApi hfApi = OpenAiApi.builder()
                .baseUrl("https://api-inference.huggingface.co/models/Qwen/Qwen2.5-7B-Instruct/v1")
                .apiKey(System.getenv("HF_TOKEN"))
                .build();
        ChatClient hfClient = ChatClient.create(
                OpenAiChatModel.builder()
                        .openAiApi(hfApi)
                        .defaultOptions(OpenAiChatOptions.builder().model("tgi").build())
                        .build()
        );

        List<ChatClient> models = List.of(ollamaClient, hfClient);
        List<String> names = List.of("Ollama（本地）", "HuggingFace（远程）");
        String question = "你是谁？";

        // ─── 顺序调用 ─────────────────────────────────────────────────
        // Python: for m in [ollama_model, hf_model]: m.invoke(...)
        System.out.println("【顺序调用】");
        long startSeq = System.currentTimeMillis();

        for (int i = 0; i < models.size(); i++) {
            String content = models.get(i).prompt().user(question).call().content();
            System.out.println(names.get(i) + "：" + content);
        }
        System.out.println("顺序用时：" + (System.currentTimeMillis() - startSeq) + " ms\n");

        // ─── 并发调用 ─────────────────────────────────────────────────
        // Python: results = await asyncio.gather(ollama.ainvoke(...), hf.ainvoke(...))
        System.out.println("【并发调用（等价 asyncio.gather）】");
        long startPar = System.currentTimeMillis();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<CompletableFuture<String>> futures = models.stream()
                .map(m -> CompletableFuture.supplyAsync(
                        () -> m.prompt().user(question).call().content(),
                        executor
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        System.out.println("并发用时：" + (System.currentTimeMillis() - startPar) + " ms");

        for (int i = 0; i < names.size(); i++) {
            System.out.println(names.get(i) + "：" + futures.get(i).join());
        }

        executor.shutdown();
    }
}

/*
【Java vs Python 对照】
Python asyncio.gather(m1.ainvoke(...), m2.ainvoke(...))
  ↔ Java CompletableFuture.allOf(future1, future2).join()

Python for m in [ollama_model, hf_model]: m.invoke(...)
  ↔ Java for (ChatClient m : models) { m.prompt()...call()... }

【性能直觉】
如果 Ollama 需要 3s、HuggingFace 需要 5s：
顺序：3 + 5 = 8s
并发：max(3, 5) = 5s
并发永远是更优选择，只要两个请求互相独立。
*/
