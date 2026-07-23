/**
 * 对应 Python：13.2-prompt_templates/PromptTemplate_PartialVariables.py
 * partial_variables：创建时预填变量 + partial() 方法运行时预填
 *
 * Python 两种 partial 用法：
 *   # 创建时预填（partial_variables 参数）
 *   template = PromptTemplate(
 *       template="语言：{language}，问题：{question}",
 *       partial_variables={"language": "Python"},
 *   )
 *   result = template.format(question="什么是循环？")  # language 已预填
 *
 *   # 运行时 partial()（在已有模板上再预填）
 *   template2 = template.partial(question="什么是函数？")
 *   result2 = template2.format()  # 两个变量都已预填
 *
 * Java 对应：
 *   Spring AI PromptTemplate 没有直接的 partial_variables 参数，
 *   但可以用 Map 预先准备好部分变量，再 merge 剩余变量，或用工厂方法封装。
 */

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class PromptTemplate_PartialVariables {
    public static void main(String[] args) {
        SpringApplication.run(PromptTemplate_PartialVariables.class, args);
    }
}

// Java 辅助类：模拟 Python PromptTemplate 的 partial_variables 功能
class PartialPromptTemplate {
    private final PromptTemplate template;
    private final Map<String, Object> partialVars;

    public PartialPromptTemplate(String templateStr, Map<String, Object> partialVars) {
        this.template = new PromptTemplate(templateStr);
        this.partialVars = new HashMap<>(partialVars);
    }

    // Python: template.format(question="...") → 合并 partial + 新变量
    public String format(Map<String, Object> additionalVars) {
        Map<String, Object> allVars = new HashMap<>(partialVars);
        allVars.putAll(additionalVars);
        return template.render(allVars);
    }

    // Python: template.partial(question="...") → 返回新模板，再预填一个变量
    public PartialPromptTemplate partial(Map<String, Object> moreVars) {
        Map<String, Object> newPartial = new HashMap<>(partialVars);
        newPartial.putAll(moreVars);
        return new PartialPromptTemplate(template.getTemplate(), newPartial);
    }
}

@Component
class PartialVariablesRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // Python: PromptTemplate(..., partial_variables={"language": "Python"})
        PartialPromptTemplate template = new PartialPromptTemplate(
                "语言：{language}，问题：{question}",
                Map.of("language", "Python")  // 预填 language
        );

        // Python: template.format(question="什么是循环？")
        // Java: 只需提供未预填的 question
        String result1 = template.format(Map.of("question", "什么是循环？"));
        System.out.println("预填 language 后的结果：" + result1);

        // Python: template2 = template.partial(question="什么是函数？")
        // Java: partial() 返回新的 PartialPromptTemplate，再追加预填 question
        PartialPromptTemplate template2 = template.partial(Map.of("question", "什么是函数？"));

        // Python: template2.format()  ← 两个变量都已预填
        String result2 = template2.format(Map.of());
        System.out.println("两个变量都预填后的结果：" + result2);

        System.out.println("\n--- Spring AI 原生写法：直接 render 时传完整 Map ---");
        // 最简单的 Java 写法：直接在 render() 时传全部变量
        PromptTemplate nativeTemplate = new PromptTemplate("语言：{language}，问题：{question}");
        Map<String, Object> allVars = Map.of("language", "Python", "question", "什么是循环？");
        System.out.println(nativeTemplate.render(allVars));
    }
}

/*
【Java vs Python 对照】
Python partial_variables={"language": "Python"}
  ↔ Java PartialPromptTemplate(template, Map.of("language","Python"))（手动封装）
  或直接在 render(Map.of("language","Python","question","...")) 传全部变量

Python template.partial(question="...")
  ↔ Java partialTemplate.partial(Map.of("question","..."))

Python template.format(question="...")（合并 partial + 新变量）
  ↔ Java partialTemplate.format(Map.of("question","..."))

【说明】
Spring AI PromptTemplate 没有 partial_variables 内置参数，
最常见的 Java 做法是在调用 render() 时直接传完整的 Map，而不用 partial 技巧。
如果确实需要预填，封装一个简单的包装类是最清晰的方案。
*/
