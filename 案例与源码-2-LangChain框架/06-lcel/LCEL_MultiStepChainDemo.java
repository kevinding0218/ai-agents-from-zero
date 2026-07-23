/**
 * 对应 Python：LCEL_MultiStepChainDemo.py
 * 多步串行链：chain1 → lambda 数据适配 → chain2
 *
 * Python:
 *   chain1 = prompt1 | model | parser1   # 中文介绍主题
 *   chain2 = prompt2 | model | parser2   # 翻译成英文
 *
 *   # 串行：chain1 输出作为 chain2 的输入
 *   # lambda 做数据适配：str → {"input": str}，以匹配 chain2 的 {input} 占位符
 *   full_chain = chain1 | (lambda content: {"input": content}) | chain2
 *
 *   result = full_chain.invoke({"topic": "langchain"})
 *
 * Java 对应：
 *   两步 invoke：第一步结果作为第二步的输入，中间用 lambda 适配数据格式。
 *   Function<String, String> 组合代替 | 运算符。
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@SpringBootApplication
public class LCEL_MultiStepChainDemo {
    public static void main(String[] args) {
        SpringApplication.run(LCEL_MultiStepChainDemo.class, args);
    }
}

@Component
class MultiStepRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MultiStepRunner.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient model = chatClientBuilder.build();

        // ─── 子链 1：中文介绍（对应 Python chain1 = prompt1 | model | parser1）──
        // Python: chain1 = prompt1 | model | parser1  → 输出 str（中文介绍）
        Function<String, String> chain1 = topic ->
                model.prompt()
                        .system("你是一个知识渊博的计算机专家，请用中文简短回答")
                        .user("请简短介绍什么是" + topic)
                        .call()
                        .content();

        // ─── 子链 2：翻译成英文（对应 Python chain2 = prompt2 | model | parser2）──
        // Python: chain2 = prompt2 | model | parser2  → 输入 {"input": content}，输出 str
        Function<String, String> chain2 = chineseContent ->
                model.prompt()
                        .system("你是一个翻译助手，将用户输入内容翻译成英文")
                        .user(chineseContent)   // 直接传中文内容（不用 {input} 占位）
                        .call()
                        .content();

        // ─── 单独执行 chain1，打印中间结果 ──────────────────────────────────
        String result1 = chain1.apply("langchain");
        logger.info("chain1 中文结果：{}", result1);

        // ─── Python: full_chain = chain1 | (lambda content: {"input": content}) | chain2 ──
        // Java: Function.andThen() 串联两个函数，等价 chain1 | lambda | chain2
        Function<String, String> fullChain = chain1.andThen(chain2);
        // 注：Python lambda 做的 str → {"input": str} 数据适配，
        //     Java 里直接把 str 传给 chain2.user()，无需 Map 包装

        // Python: result = full_chain.invoke({"topic": "langchain"})
        String result = fullChain.apply("langchain");
        logger.info("full_chain 英文翻译：{}", result);
    }
}

/*
【Java vs Python 多步串行链对照】

Python:
  chain1 = prompt1 | model | parser1              ↔  Java Function<String, String> chain1 = ...
  lambda content: {"input": content}              ↔  Java 省略（直接传 content，无需 dict 包装）
  chain2 = prompt2 | model | parser2              ↔  Java Function<String, String> chain2 = ...
  full_chain = chain1 | lambda | chain2            ↔  Java chain1.andThen(chain2)
  result = full_chain.invoke({"topic":"langchain"}) ↔  Java fullChain.apply("langchain")

【Python lambda 数据适配 vs Java】
Python chain2 的 prompt2 用了 {input} 占位符，所以 chain1 输出的 str 要包成 {"input": str}。
Java 直接把 str 传给 .user()，无需 dict 包装，代码更干净。

【andThen vs | 运算符】
Python | 运算符：是 LangChain Runnable 的重载，把两个 Runnable 串成 RunnableSequence。
Java andThen()：是 java.util.function.Function 的标准方法，把两个函数串联。
两者都是"函数组合"，语法不同但语义一致。
*/
