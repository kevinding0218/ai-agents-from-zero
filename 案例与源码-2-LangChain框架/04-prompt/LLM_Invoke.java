/**
 * 对应 Python：13.1-invoke/LLM_Invoke.py
 * 基础调用：System + Human 消息，返回 AIMessage
 *
 * Python:
 *   messages = [
 *       SystemMessage("你是一个AI助手"),
 *       HumanMessage("你是谁"),
 *   ]
 *   response = model.invoke(messages)
 *   print(type(response))    # <class 'langchain_core.messages.ai.AIMessage'>
 *   print(response.content)
 *
 * application.properties：
 *   spring.ai.openai.api-key=${QWEN_API_KEY}
 *   spring.ai.openai.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
 *   spring.ai.openai.chat.options.model=qwen-plus
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootApplication
public class LLM_Invoke {
    public static void main(String[] args) {
        SpringApplication.run(LLM_Invoke.class, args);
    }
}

@Component
class InvokeRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient model = chatClientBuilder.build();

        // Python: messages = [SystemMessage("..."), HumanMessage("...")]
        var messages = List.of(
                new SystemMessage("你是一个AI助手"),
                new UserMessage("你是谁")
        );

        // Python: response = model.invoke(messages)
        ChatResponse response = model.prompt()
                .messages(messages)
                .call()
                .chatResponse();

        // Python: print(type(response))  → <class 'langchain_core.messages.ai.AIMessage'>
        System.out.println("类型：" + response.getResult().getOutput().getClass().getName());

        // Python: print(response.content)
        System.out.println("内容：" + response.getResult().getOutput().getText());
    }
}

/*
【Java vs Python 对照】
Python SystemMessage("...")    ↔  Java new SystemMessage("...")
Python HumanMessage("...")     ↔  Java new UserMessage("...")
Python model.invoke(messages)  ↔  Java chatClient.prompt().messages(messages).call().chatResponse()
Python type(response)          ↔  Java response.getResult().getOutput().getClass().getName()
Python response.content        ↔  Java response.getResult().getOutput().getText()
                               （或 chatClient.prompt()...call().content() 更简洁）
*/
