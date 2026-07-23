/**
 * 对应 Python：
 *   13.2-prompt_templates/13.2.1-method/PromptTemplate_FormatMethod.py   → format() 返回 str
 *   13.2-prompt_templates/13.2.1-method/PromptTemplate_InvokeMethod.py   → invoke() 返回 PromptValue
 *   13.2-prompt_templates/13.2.1-method/PromptTemplate_PartialMethod.py  → partial() 预填变量
 *
 * Python 三种方法的区别：
 *   template.format(role="老师", question="...")   → 返回 str（纯字符串）
 *   template.invoke({"role":"老师", ...})          → 返回 StringPromptValue（可继续 .to_string() 或 .to_messages()）
 *   template.partial(role="老师")                  → 返回新 PromptTemplate（已预填 role）
 *
 * invoke() 的 StringPromptValue 可以：
 *   .to_string()   → 转成 str
 *   .to_messages() → 转成 [HumanMessage(content="...")]（自动包装为消息列表）
 */

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Map;

@SpringBootApplication
public class PromptTemplate_FormatAndInvokeMethod {
    public static void main(String[] args) {
        SpringApplication.run(PromptTemplate_FormatAndInvokeMethod.class, args);
    }
}

@Component
class FormatInvokeMethodRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        PromptTemplate template = new PromptTemplate(
                "你是一个专业的{role}工程师，请回答：{question}"
        );

        // ─── Python: template.format(role="python开发", question="冒泡排序怎么写？") → str ───
        // Java: .render() 对应 format()，返回 String
        String formatResult = template.render(Map.of("role", "python开发", "question", "冒泡排序怎么写？"));
        System.out.println("【format 方法】返回 String：");
        System.out.println(formatResult);
        System.out.println("类型：" + formatResult.getClass().getSimpleName());

        System.out.println("\n" + "=".repeat(50));

        // ─── Python: template.invoke({...}) → StringPromptValue ───────────────────
        // Java: .create() 对应 invoke()，返回 Prompt 对象（等价 PromptValue）
        Prompt promptValue = template.create(Map.of("role", "python开发", "question", "冒泡排序怎么写？"));
        System.out.println("【invoke 方法】返回 Prompt（PromptValue）：");
        System.out.println(promptValue);

        // Python: prompt_value.to_string() → 转为纯字符串
        System.out.println("to_string：" + promptValue.getContents());

        // Python: prompt_value.to_messages() → [HumanMessage(content="...")]
        // Java: prompt.getInstructions() → List<Message>
        System.out.println("to_messages：" + promptValue.getInstructions());

        System.out.println("\n" + "=".repeat(50));

        // ─── Python: template.partial(role="python开发") → 新 PromptTemplate ───────
        // Java: 手动预填，见 PromptTemplate_PartialVariables.java
        System.out.println("【partial 方法】预填 role 后只需提供 question：");
        // 简单模拟：用 Map 先填一个变量
        Map<String, Object> preFilledVars = Map.of("role", "python开发");
        // 合并剩余变量时再 putAll
        java.util.Map<String, Object> allVars = new java.util.HashMap<>(preFilledVars);
        allVars.put("question", "堆排序怎么写？");
        System.out.println(template.render(allVars));
    }
}

/*
【Java vs Python 三种方法对照】

Python format(role="...", question="...")    ↔  Java render(Map.of("role","...","question","..."))
  返回：str                                      返回：String

Python invoke({"role":"...", "question":"..."}) ↔  Java create(Map.of("role","...","question","..."))
  返回：StringPromptValue                         返回：Prompt

Python promptValue.to_string()               ↔  Java prompt.getContents()
Python promptValue.to_messages()             ↔  Java prompt.getInstructions()（List<Message>）

Python template.partial(role="...")          ↔  Java 手动 Map.of("role","...") + putAll（无内置 partial）
  返回：新 PromptTemplate（已预填 role）

【实用建议】
- 只需字符串：用 render()（Python 的 format()）
- 需要 Prompt 对象接入链式调用：用 create()（Python 的 invoke()）
- 需要消息列表发给模型：用 prompt.getInstructions() 或直接 chatClient.prompt(prompt).call()
*/
