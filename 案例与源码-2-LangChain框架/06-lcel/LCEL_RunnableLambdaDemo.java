/**
 * 对应 Python：LCEL_RunnableLambdaDemo.py
 * 函数链：用 RunnableLambda 将普通函数接入 LCEL 链
 *
 * Python:
 *   def debug_print(x):
 *       logger.info(f"中间结果:{x}")
 *       return {"input": x}
 *
 *   # 方式一：直接把函数放在 | 之间，LCEL 自动包装成 Runnable
 *   full_chain = chain1 | debug_print | chain2
 *
 *   # 方式二：显式用 RunnableLambda 包装
 *   debug_node = RunnableLambda(debug_print)
 *   full_chain = chain1 | debug_node | chain2
 *
 * RunnableLambda 的作用：
 *   把普通函数变成 Runnable，便于插入链中间做调试/数据适配/轻量逻辑。
 *
 * Java 对应：
 *   Function<T, R>（Java 函数式接口）就是 RunnableLambda 的等价。
 *   andThen() 组合函数就是 | 运算符的等价。
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
public class LCEL_RunnableLambdaDemo {
    public static void main(String[] args) {
        SpringApplication.run(LCEL_RunnableLambdaDemo.class, args);
    }
}

@Component
class LambdaRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(LambdaRunner.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    // Python: def debug_print(x): logger.info(f"中间结果:{x}"); return {"input": x}
    // Java:   普通方法（等价 debug_print 函数）
    private String debugPrint(String x) {
        logger.info("中间结果：{}", x);
        return x;  // Java 直接返回 str，不需要包成 {"input": x}
    }

    @Override
    public void run(String... args) {
        ChatClient model = chatClientBuilder.build();

        // ─── 子链 1：中文介绍 ─────────────────────────────────────────────────
        Function<String, String> chain1 = topic ->
                model.prompt()
                        .system("你是一个知识渊博的计算机专家，请用中文简短回答")
                        .user("请简短介绍什么是" + topic)
                        .call()
                        .content();

        // ─── 子链 2：翻译成英文 ───────────────────────────────────────────────
        Function<String, String> chain2 = content ->
                model.prompt()
                        .system("你是一个翻译助手，将用户输入内容翻译成英文")
                        .user(content)
                        .call()
                        .content();

        // ─── 方式一：直接把方法引用放在 andThen 里（等价 Python chain1 | debug_print | chain2）──
        // Python: full_chain = chain1 | debug_print | chain2
        //         result1 = full_chain.invoke({"topic": "langchain"})
        // Java:   Function 组合
        Function<String, String> fullChain1 = chain1
                .andThen(this::debugPrint)   // debug_print 作为中间节点
                .andThen(chain2);

        System.out.println("【方式一：方法引用（等价 chain1 | debug_print | chain2）】");
        String result1 = fullChain1.apply("langchain");
        logger.info("最终结果111：{}", result1);

        System.out.println("\n" + "=".repeat(60));

        // ─── 方式二：显式命名 Function 变量（等价 Python RunnableLambda(debug_print)）──
        // Python: debug_node = RunnableLambda(debug_print)
        //         full_chain = chain1 | debug_node | chain2
        // Java:   显式定义 Function 变量（等价 debug_node）
        Function<String, String> debugNode = this::debugPrint;  // 等价 RunnableLambda(debug_print)

        Function<String, String> fullChain2 = chain1
                .andThen(debugNode)   // 显式 debug_node，与方式一效果相同
                .andThen(chain2);

        System.out.println("【方式二：显式 Function 变量（等价 RunnableLambda(debug_print)）】");
        String result2 = fullChain2.apply("langchain");
        logger.info("最终结果222：{}", result2);
    }
}

/*
【Java vs Python RunnableLambda 对照】

Python def debug_print(x): ...
  ↔ Java String debugPrint(String x) { ... }（普通方法）

Python RunnableLambda(debug_print)
  ↔ Java Function<String, String> debugNode = this::debugPrint（方法引用）

Python chain1 | debug_print | chain2（直接放函数，自动包装）
  ↔ Java chain1.andThen(this::debugPrint).andThen(chain2)

Python chain1 | debug_node | chain2（显式 RunnableLambda）
  ↔ Java chain1.andThen(debugNode).andThen(chain2)（效果完全相同）

【RunnableLambda 的核心价值】
Python 版：LCEL 链里所有节点必须是 Runnable，
  普通函数不能直接放进去，RunnableLambda 是"包装器"，让函数变成 Runnable。
Java 版：Function<T,R> 本身就是函数式接口，andThen() 直接组合，
  不需要额外的"包装器"，Java 8+ 函数式 API 天生支持这种组合。

【何时使用函数节点？】
适合做轻量逻辑：打印中间结果（调试）、字段映射（数据适配）、结构转换。
不适合：复杂业务逻辑（改用专门的 Service 或 @Bean）。
*/
