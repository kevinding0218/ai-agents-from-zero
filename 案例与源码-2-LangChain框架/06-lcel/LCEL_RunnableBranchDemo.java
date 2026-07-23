/**
 * 对应 Python：LCEL_RunnableBranchDemo.py
 * 分支链：根据输入条件路由到不同子链（翻译专家：英语/日语/韩语）
 *
 * Python:
 *   chain = RunnableBranch(
 *       (lambda x: determine_language(x) == "japanese", japanese_prompt | model | parser),
 *       (lambda x: determine_language(x) == "korean",   korean_prompt   | model | parser),
 *       (english_prompt | model | parser),   # 默认分支
 *   )
 *   result = chain.invoke({"query": '请你用韩语翻译："见到你很高兴"'})
 *
 * Java 对应：
 *   没有 RunnableBranch 直接等价类，但可以用普通 if/else 或 switch 实现相同路由逻辑。
 *   这更符合 Java 的代码风格，且更易于阅读和测试。
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class LCEL_RunnableBranchDemo {
    public static void main(String[] args) {
        SpringApplication.run(LCEL_RunnableBranchDemo.class, args);
    }
}

@Component
class BranchRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(BranchRunner.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    // Python: def determine_language(inputs): if "日语" in query: return "japanese" ...
    String determineLanguage(String query) {
        if (query.contains("日语")) return "japanese";
        if (query.contains("韩语")) return "korean";
        return "english";
    }

    // Python: RunnableBranch → 根据条件选分支子链
    // Java: 普通方法封装路由逻辑（等价效果，更清晰）
    String invoke(ChatClient model, Map<String, String> input) {
        String query = input.get("query");
        String lang = determineLanguage(query);

        // Python:
        //   (lambda x: determine_language(x) == "japanese", japanese_prompt | model | parser)
        //   (lambda x: determine_language(x) == "korean",   korean_prompt   | model | parser)
        //   (english_prompt | model | parser)  ← 默认
        // Java: if/else 分支选择对应的 system 消息
        String systemMsg = switch (lang) {
            case "japanese" -> "你是一个日语翻译专家，你叫小日";
            case "korean"   -> "你是一个韩语翻译专家，你叫小韩";
            default         -> "你是一个英语翻译专家，你叫小英";
        };

        // Python: chatPromptTemplate.format_messages(**query_input) → 打印格式化后的 prompt（仅用于日志）
        // 注：这段只是为了演示选了哪条分支，实际执行由 chain.invoke 内部完成
        logger.info("检测到语言类型：{}", lang);
        logger.info("格式化后的提示词：[system]: {}  [human]: {}", systemMsg, query);

        // Python: result = chain.invoke(query_input)  → 执行选中的子链
        // Java:   直接用选中的 system 消息调用模型
        return model.prompt()
                .messages(List.of(
                        new SystemMessage(systemMsg),
                        new UserMessage(query)
                ))
                .call()
                .content();
    }

    @Override
    public void run(String... args) {
        ChatClient model = chatClientBuilder.build();

        // Python: test_queries = [{"query": '请你用韩语翻译："见到你很高兴"'}, ...]
        List<Map<String, String>> testQueries = List.of(
                Map.of("query", "请你用韩语翻译这句话：\"见到你很高兴\""),
                Map.of("query", "请你用日语翻译这句话：\"见到你很高兴\""),
                Map.of("query", "请你用英语翻译这句话：\"见到你很高兴\"")
        );

        // Python: for query_input in test_queries: result = chain.invoke(query_input)
        for (Map<String, String> queryInput : testQueries) {
            String result = invoke(model, queryInput);
            logger.info("输出结果：{}\n", result);
        }
    }
}

/*
【Java vs Python RunnableBranch 对照】

Python RunnableBranch(
    (lambda x: condition1(x), chain1),
    (lambda x: condition2(x), chain2),
    default_chain
)
  ↔
Java switch (determineLanguage(query)) {
    case "japanese" -> japaneseSystemMsg;
    case "korean"   -> koreanSystemMsg;
    default         -> englishSystemMsg;
}

Python chain.invoke({"query": "..."})
  ↔ Java invoke(model, Map.of("query","..."))（封装了路由逻辑的普通方法）

【为什么 Java 用 if/else 而不是 RunnableBranch？】
RunnableBranch 的价值在于：可以把条件和子链组合成一个 Runnable 对象，放进更大的链里。
Java 没有这样的"运算符重载 + Runnable 组合"模式，
直接 if/else 或 switch 更符合 Java 风格，可读性更高，调试也更容易。

如果确实需要更动态的路由（如从配置文件加载），可以用策略模式（Map<String, Function<Input, Output>>）实现。
*/
