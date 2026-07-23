/**
 * 对应 Python：LangChain_Ollama.py
 * Ollama 本地模型：reasoning=True 开启 CoT；多角色消息；reasoning_content
 *
 * Python:
 *   from langchain_ollama import ChatOllama
 *   model = ChatOllama(
 *       base_url="http://localhost:11434",
 *       model="qwen3:4b",
 *       reasoning=True,   # 开启推理/思考链（CoT）
 *   )
 *   messages = [
 *       SystemMessage("你是一个AI助手"),
 *       HumanMessage("你是谁"),
 *       AIMessage("我是AI助手"),
 *       HumanMessage("那你能做什么"),
 *   ]
 *   response = model.invoke(messages)
 *   print(response.content)
 *   print(response.additional_kwargs["reasoning_content"])  # 思考过程
 *
 * application.properties：
 *   spring.ai.ollama.base-url=http://localhost:11434
 *   spring.ai.ollama.chat.options.model=qwen3:4b
 *
 * 依赖（pom.xml）：
 *   <dependency>
 *       <groupId>org.springframework.ai</groupId>
 *       <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
 *   </dependency>
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootApplication
public class LangChain_Ollama {
    public static void main(String[] args) {
        SpringApplication.run(LangChain_Ollama.class, args);
    }
}

@Component
class OllamaRunner implements CommandLineRunner {

    @Autowired
    private OllamaChatModel ollamaChatModel;  // Spring AI 自动配置注入

    @Override
    public void run(String... args) {
        // Python: reasoning=True → 启用 CoT 思考，Ollama 的 think 参数
        ChatClient model = ChatClient.builder(ollamaChatModel)
                .defaultOptions(OllamaOptions.builder()
                        .model("qwen3:4b")
                        .think(true)   // reasoning=True 的 Java 等价
                        .build())
                .build();

        // Python: messages = [SystemMessage(...), HumanMessage(...), AIMessage(...), HumanMessage(...)]
        // Java: 多角色消息列表（System / User / Assistant / User）
        var messages = List.of(
                new SystemMessage("你是一个AI助手"),
                new UserMessage("你是谁"),
                new AssistantMessage("我是AI助手"),
                new UserMessage("那你能做什么")
        );

        // Python: response = model.invoke(messages)
        ChatResponse response = model.prompt()
                .messages(messages)
                .call()
                .chatResponse();

        // Python: print(response.content)
        System.out.println("模型回答：" + response.getResult().getOutput().getText());

        // Python: print(response.additional_kwargs["reasoning_content"])
        // Java: Spring AI 把思考内容放在 metadata 或 additionalProperties 里
        var additionalProps = response.getResult().getOutput().getMetadata();
        System.out.println("思考过程 metadata：" + additionalProps);
        // 注：具体字段名取决于 Spring AI 版本；通常通过 getMetadata().get("thinking") 访问
    }
}

/*
【Java vs Python 对照】
Python ChatOllama(base_url=..., model=..., reasoning=True)
  ↔ Java OllamaChatModel（自动配置）+ OllamaOptions.builder().think(true)

Python SystemMessage("...")    ↔  Java new SystemMessage("...")
Python HumanMessage("...")     ↔  Java new UserMessage("...")
Python AIMessage("...")        ↔  Java new AssistantMessage("...")
Python model.invoke(messages)  ↔  Java chatClient.prompt().messages(messages).call().chatResponse()
Python response.content        ↔  Java response.getResult().getOutput().getText()
Python response.additional_kwargs["reasoning_content"]
  ↔ Java response.getResult().getOutput().getMetadata()（具体字段依 Spring AI 版本而定）

【注意】
reasoning=True 对应 Ollama 的 /api/chat 里的 think=true 参数。
Spring AI 的 OllamaOptions 封装了这个字段；早期版本可能需要手动 additionalProperties。
*/
