/**
 * 对应 Python：13.2-prompt_templates/PromptTemplate_Combined.py
 * 用 + 运算符合并两个 PromptTemplate
 *
 * Python:
 *   t1 = PromptTemplate.from_template("你是一个{role}，")
 *   t2 = PromptTemplate.from_template("请回答：{question}")
 *   combined = t1 + t2   # 合并成一个模板："你是一个{role}，请回答：{question}"
 *   result = combined.format(role="老师", question="AI是什么？")
 *
 * Java 对应：
 *   Java 没有运算符重载，用字符串拼接模板后构建新 PromptTemplate。
 *   或者用 String.join / 字符串格式化组合模板字符串。
 */

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Map;

@SpringBootApplication
public class PromptTemplate_Combined {
    public static void main(String[] args) {
        SpringApplication.run(PromptTemplate_Combined.class, args);
    }
}

@Component
class CombinedRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        String t1Str = "你是一个{role}，";
        String t2Str = "请回答：{question}";

        // Python: combined = t1 + t2
        // Java: 字符串拼接后构建新 PromptTemplate（等价效果）
        String combinedStr = t1Str + t2Str;
        PromptTemplate combined = new PromptTemplate(combinedStr);

        // Python: result = combined.format(role="老师", question="AI是什么？")
        String result = combined.render(Map.of("role", "老师", "question", "AI是什么？"));
        System.out.println("合并后模板字符串：" + combinedStr);
        System.out.println("format 结果：" + result);

        System.out.println("\n--- 更灵活的 Java 写法：String.format 或 StringBuilder ---");
        // 如果模板逻辑更复杂，可以用 StringBuilder 分段拼接
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个{role}，");
        sb.append("你的专长是{specialty}，");
        sb.append("请回答：{question}");

        PromptTemplate multiPart = new PromptTemplate(sb.toString());
        String result2 = multiPart.render(Map.of(
                "role", "AI工程师",
                "specialty", "LangChain",
                "question", "如何使用向量数据库？"
        ));
        System.out.println("多段合并结果：" + result2);
    }
}

/*
【Java vs Python 对照】
Python t1 + t2（运算符重载，合并模板）
  ↔ Java t1Str + t2Str（字符串拼接），再 new PromptTemplate(combinedStr)

Python combined.format(role="...", question="...")
  ↔ Java combined.render(Map.of("role","...","question","..."))

【说明】
Python 的 + 运算符是 LangChain PromptTemplate 重载的，底层是字符串拼接 + 变量列表合并。
Java 没有运算符重载，但字符串拼接 + PromptTemplate 构造器可以实现完全相同的效果，代码更透明。
*/
