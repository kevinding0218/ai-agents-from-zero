/**
 * 对应 Python：LCEL_RunnableSequenceDemo.py
 * 顺序链：Prompt → Model → Parser 用 | 管道符连接，组成 RunnableSequence
 *
 * Python:
 *   chat_prompt = ChatPromptTemplate.from_messages([...])
 *   model = init_chat_model(...)
 *   parser = StrOutputParser()
 *
 *   # 分步调用（等价最终的链式）
 *   prompt = chat_prompt.invoke({"role":"AI助手","question":"..."})
 *   result = model.invoke(prompt)
 *   response = parser.invoke(result)
 *
 *   # LCEL 链式写法
 *   chain = chat_prompt | model | parser   # RunnableSequence
 *   result_chain = chain.invoke({"role":"AI助手","question":"..."})
 *   print(type(chain))  # <class 'langchain_core.runnables.base.RunnableSequence'>
 *
 * Java 对应：
 *   Spring AI ChatClient 的链式 API 天然等价于 LCEL 的顺序链。
 *   .system() → .user() → .call() → .content() 就是 prompt | model | parser 的 Java 写法。
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class LCEL_RunnableSequenceDemo {
    public static void main(String[] args) {
        SpringApplication.run(LCEL_RunnableSequenceDemo.class, args);
    }
}

@Component
class RunnableSequenceRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RunnableSequenceRunner.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient model = chatClientBuilder.build();

        // ─── 分步调用（等价 Python 的 chat_prompt.invoke → model.invoke → parser.invoke）──
        System.out.println("【分步调用（prompt → model → parser 各自 invoke）】");

        // Python: prompt = chat_prompt.invoke({"role":"AI助手","question":"..."})
        var messages = List.of(
                new SystemMessage("你是一个AI助手，请简短回答我提出的问题"),
                new UserMessage("什么是LangChain，简洁回答100字以内")
        );
        logger.info("消息（等价 chat_prompt.invoke）：{}", messages);

        // Python: result = model.invoke(prompt)  → AIMessage
        ChatResponse result = model.prompt()
                .messages(messages)
                .call()
                .chatResponse();
        logger.info("模型原始输出（AIMessage）：{}", result);

        // Python: response = parser.invoke(result)  → str
        String response = result.getResult().getOutput().getText();
        logger.info("解析后结果（str）：{}", response);
        logger.info("结果类型：{}", response.getClass().getSimpleName());

        System.out.println("\n" + "=".repeat(60));

        // ─── 链式写法（等价 Python 的 chain = prompt | model | parser）────────
        System.out.println("【链式写法（等价 chain = chat_prompt | model | parser）】");

        // Python: chain = chat_prompt | model | parser
        // Java:   ChatClient 的链式 API 就是 LCEL 顺序链的 Java 版本
        //         .system() → prompt 部分
        //         .call()   → model 部分
        //         .content() → parser 部分（取出字符串）

        // Python: result_chain = chain.invoke({"role":"AI助手","question":"..."})
        String resultChain = model.prompt()
                .system("你是一个AI助手，请简短回答我提出的问题")
                .user("什么是LangChain，简洁回答100字以内")
                .call()
                .content();   // 整个链一步完成

        logger.info("Chain执行结果：{}", resultChain);
        logger.info("Chain执行结果类型：{}", resultChain.getClass().getSimpleName());

        // Python: print(type(chain))  → <class 'langchain_core.runnables.base.RunnableSequence'>
        // Java: ChatClient 本身就是链式 API，不需要单独的 RunnableSequence 类
        System.out.println("Java 类型：ChatClient（内置链式 API，等价 RunnableSequence）");
    }
}

/*
【Java vs Python RunnableSequence 对照】

Python 分步调用：
  prompt_value = chat_prompt.invoke(vars)    ↔  Java List<Message> messages = ...
  ai_message   = model.invoke(prompt_value)  ↔  Java chatResponse = chatClient.prompt().messages(msgs).call().chatResponse()
  text         = parser.invoke(ai_message)   ↔  Java text = chatResponse.getResult().getOutput().getText()

Python 链式（LCEL）：
  chain = chat_prompt | model | parser
  result = chain.invoke({"role":"...", "question":"..."})
  ↔
  Java 链式（Spring AI）：
  result = chatClient.prompt().system("...").user("...").call().content()

Python type(chain) → RunnableSequence
  ↔ Java type = ChatClient（无单独 RunnableSequence 类，链式 API 内置）

【核心洞察】
LCEL 的 | 运算符在 Python 里是后来加上的语法糖，本质是把 Runnable 对象串联。
Java Spring AI 从设计之初就是 Fluent Builder API（链式调用），两者目标相同，写法不同。
实质等价：prompt | model | parser ↔ .system().user().call().content()
*/
