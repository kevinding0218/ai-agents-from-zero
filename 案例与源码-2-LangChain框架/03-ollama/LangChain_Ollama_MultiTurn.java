/**
 * 对应 Python：LangChain_Ollama_MultiTurn.py
 * Ollama 多轮对话：history 列表逐轮增长，跟踪 token 用量
 *
 * Python:
 *   history = [SystemMessage("你是一个AI助手")]
 *   while True:
 *       user_input = input("你：")
 *       history.append(HumanMessage(user_input))
 *       response = model.invoke(history)
 *       history.append(response)      # 把 AIMessage 加回 history
 *       print(response.content)
 *       # token 统计
 *       print(response.response_metadata.get("prompt_eval_count"))  # 输入 token
 *       print(response.response_metadata.get("eval_count"))          # 输出 token
 *
 * 关键点：每轮都把完整 history 发给模型，这是"手动管理记忆"的最简单方式。
 *
 * application.properties：
 *   spring.ai.ollama.base-url=http://localhost:11434
 *   spring.ai.ollama.chat.options.model=qwen3:4b
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class LangChain_Ollama_MultiTurn {
    public static void main(String[] args) {
        SpringApplication.run(LangChain_Ollama_MultiTurn.class, args);
    }
}

@Component
class MultiTurnRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MultiTurnRunner.class);

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @Override
    public void run(String... args) {
        ChatClient model = ChatClient.create(ollamaChatModel);

        // Python: history = [SystemMessage("你是一个AI助手")]
        // Java: ArrayList 可随时追加（对应 Python 的 list.append）
        List<Message> history = new ArrayList<>();
        history.add(new SystemMessage("你是一个AI助手"));

        System.out.println("多轮对话已开始，输入 quit 退出");
        System.out.println("-".repeat(50));

        Scanner scanner = new Scanner(System.in);

        // Python: while True:
        while (scanner.hasNextLine()) {
            System.out.print("你：");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("quit") || userInput.equalsIgnoreCase("exit")) {
                System.out.println("对话结束");
                break;
            }
            if (userInput.isEmpty()) continue;

            // Python: history.append(HumanMessage(user_input))
            history.add(new UserMessage(userInput));

            // Python: response = model.invoke(history)
            ChatResponse response = model.prompt()
                    .messages(history)
                    .call()
                    .chatResponse();

            // Python: history.append(response)  ← 把 AIMessage 加回 history
            String replyText = response.getResult().getOutput().getText();
            history.add(new AssistantMessage(replyText));

            // Python: print(response.content)
            System.out.println("AI：" + replyText);

            // Python: response.response_metadata.get("prompt_eval_count")
            // Python: response.response_metadata.get("eval_count")
            // Java: Ollama 的 token 统计在 ChatGenerationMetadata 里
            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                logger.info("[token 用量] 输入: {}  输出: {}  总计: {}",
                        usage.getPromptTokens(),
                        usage.getGenerationTokens(),
                        usage.getTotalTokens());
            }
            System.out.println("-".repeat(50));
        }
    }
}

/*
【Java vs Python 对照】
Python history = [SystemMessage(...)]        ↔  Java List<Message> history = new ArrayList<>()
Python history.append(HumanMessage(...))     ↔  Java history.add(new UserMessage(...))
Python history.append(response)              ↔  Java history.add(new AssistantMessage(replyText))
Python model.invoke(history)                 ↔  Java chatClient.prompt().messages(history).call().chatResponse()
Python response.content                      ↔  Java response.getResult().getOutput().getText()
Python response.response_metadata["prompt_eval_count"]
  ↔ Java response.getMetadata().getUsage().getPromptTokens()
Python response.response_metadata["eval_count"]
  ↔ Java response.getMetadata().getUsage().getGenerationTokens()

【关键模式】
每轮把完整 history 发出，模型才能"记住"上轮说了什么。
这是最简单的手动记忆方式，无需额外框架支持。
*/
