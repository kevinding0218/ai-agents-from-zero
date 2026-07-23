/**
 * 对应 Python：LangChain_huggingface_chat_model_local.py
 * 本地运行 HuggingFace 模型（不依赖网络，在本机 CPU/GPU 上推理）
 *
 * Python 做法：
 *   from transformers import pipeline
 *   from langchain_huggingface import HuggingFacePipeline, ChatHuggingFace
 *
 *   pipe  = pipeline("text-generation", model="HuggingFaceTB/SmolLM2-135M-Instruct", device="mps")
 *   llm   = HuggingFacePipeline(pipeline=pipe)
 *   model = ChatHuggingFace(llm=llm)
 *   print(model.invoke("你是谁").content)
 *
 * ============================================================
 * Java / Spring AI 没有直接等价写法。
 *
 * 原因：
 *   Python 的 transformers 库直接加载 PyTorch 模型权重（.safetensors / .bin）并在本机推理。
 *   Java 生态没有等价的"一行代码加载 HuggingFace 模型"的库。
 *
 * Java 里最接近的三种替代方案：
 * ============================================================
 *
 * 方案 A（推荐）：用 Ollama 运行本地模型 + Spring AI Ollama 集成
 * -----------------------------------------------------------
 *   1. 安装 Ollama：https://ollama.com
 *   2. 拉取模型：ollama pull qwen2.5:7b
 *   3. Spring AI 自动配置对接 Ollama（本机 HTTP 接口）
 *
 *   application.properties：
 *     spring.ai.ollama.base-url=http://localhost:11434
 *     spring.ai.ollama.chat.options.model=qwen2.5:7b
 *
 *   依赖：
 *     <dependency>
 *         <groupId>org.springframework.ai</groupId>
 *         <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
 *     </dependency>
 *
 * 方案 B：ONNX Runtime（适合需要嵌入 Java 进程的场景）
 * -----------------------------------------------------------
 *   把 HuggingFace 模型导出成 ONNX 格式，用 onnxruntime-java 在 JVM 内推理。
 *   但需要自己处理 tokenizer、输入格式、输出解析，工程量大，不适合入门。
 *
 * 方案 C：调用 Python 服务（最常见的企业架构）
 * -----------------------------------------------------------
 *   Python FastAPI 跑 HuggingFace 模型，暴露 HTTP 接口。
 *   Java Spring Boot 用 WebClient / RestTemplate 调用。
 *   这也是为什么很多公司会保留"Python 做 AI，Java 做业务"的分层架构。
 *
 * 下面演示方案 A（Ollama）的代码，功能上等价于 Python 本地模型调用：
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Scanner;

// 注意：这里的 @SpringBootApplication 需要引入 spring-ai-ollama-spring-boot-starter
// 而不是 spring-ai-openai-spring-boot-starter
@SpringBootApplication
public class HuggingFaceLocal {
    public static void main(String[] args) {
        SpringApplication.run(HuggingFaceLocal.class, args);
    }
}

@Component
class OllamaLocalRunner implements CommandLineRunner {

    // Ollama 的 ChatClient 由 Spring AI 自动配置，注入方式和 OpenAI 完全一样
    // 对应 Python: model = init_chat_model(model_provider="huggingface", backend="pipeline", ...)
    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient chatClient = chatClientBuilder.build();

        System.out.println("本地模型已就绪（via Ollama），输入 quit 退出");
        System.out.println("-".repeat(50));

        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            System.out.print("你：");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("quit") || userInput.equalsIgnoreCase("exit")) {
                System.out.println("再见！");
                break;
            }
            if (userInput.isEmpty()) continue;

            // Python: response = model.invoke(user_input); print(response.content)
            String reply = chatClient.prompt()
                    .user(userInput)
                    .call()
                    .content();

            System.out.println("模型回复：" + reply);
            // 注意：Ollama 本地模型也不返回 token 用量，这点和 Python 本地 pipeline 一样
            System.out.println("-".repeat(50));
        }
    }
}

/*
【Java vs Python 本地模型对照】
Python  transformers.pipeline()   ↔  无直接等价，推荐用 Ollama 替代
Python  HuggingFacePipeline       ↔  Spring AI OllamaChatModel（自动配置）
Python  device="mps"/"cuda"/"cpu" ↔  Ollama 自动选择本机 GPU/CPU
Python  本地权重文件               ↔  ollama pull <模型名> 下载到本地

【隐含差异】
- Python 可直接加载 HuggingFace 上任意模型（safetensors 格式）
- Java + Ollama 只能用 Ollama 已支持的模型（GGUF 格式，覆盖率约 80%）
- 如果必须用特定 HuggingFace 模型，最实用的方案是 Python FastAPI 暴露接口，Java 调用
*/
