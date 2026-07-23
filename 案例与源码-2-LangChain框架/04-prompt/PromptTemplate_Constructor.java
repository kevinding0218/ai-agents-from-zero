/**
 * 对应 Python：13.2-prompt_templates/PromptTemplate_Constructor.py
 * PromptTemplate 构造函数：显式指定 input_variables
 *
 * Python:
 *   template = PromptTemplate(
 *       template="你是一个{role}，请回答：{question}",
 *       input_variables=["role", "question"],
 *   )
 *   result = template.invoke({"role": "老师", "question": "AI是什么？"})
 *   # result 是 StringPromptValue，不是纯字符串
 *
 * Python 注意：
 *   .format(...) 返回 str
 *   .invoke(...) 返回 StringPromptValue（PromptValue 对象），可继续 .to_string() 或接链式调用
 *
 * Java 对应：Spring AI PromptTemplate 不区分 format/invoke，统一用 render()/create() 方法。
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Map;

@SpringBootApplication
public class PromptTemplate_Constructor {
    public static void main(String[] args) {
        SpringApplication.run(PromptTemplate_Constructor.class, args);
    }
}

@Component
class ConstructorRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        // Python: PromptTemplate(template="...", input_variables=["role", "question"])
        // Java: Spring AI PromptTemplate 自动解析 {variable}，无需声明 input_variables
        PromptTemplate template = new PromptTemplate("你是一个{role}，请简短回答：{question}");

        // Python: template.format(role="老师", question="AI是什么？")  → str
        String promptStr = template.render(Map.of("role", "老师", "question", "AI是什么？"));
        System.out.println("format 结果（字符串）：" + promptStr);

        // Python: template.invoke({"role":"老师","question":"..."})  → StringPromptValue
        // Java: template.create(vars) → Prompt 对象（等价 PromptValue）
        Prompt prompt = template.create(Map.of("role", "老师", "question", "AI是什么？"));
        System.out.println("invoke 结果（Prompt 对象）：" + prompt);

        // Python: result.to_string()  → 转为纯字符串
        System.out.println("to_string：" + prompt.getContents());

        System.out.println("\n" + "=".repeat(50));

        // 接链式调用：把 Prompt 传给模型
        // Python: model.invoke(result)  ← PromptValue 直接传给模型
        // Java:   chatClient.prompt(prompt).call()
        ChatClient model = chatClientBuilder.build();
        String response = model.prompt(prompt).call().content();
        System.out.println("模型回答：" + response);
    }
}

/*
【Java vs Python 对照】
Python PromptTemplate(template="...", input_variables=["role"])
  ↔ Java new PromptTemplate("...")（Spring AI 自动检测占位符，无需声明 input_variables）

Python template.format(role="...")   ↔  Java template.render(Map.of("role", "..."))  → String
Python template.invoke({...})        ↔  Java template.create(Map.of(...))             → Prompt 对象
Python result.to_string()            ↔  Java prompt.getContents()
Python model.invoke(result)          ↔  Java chatClient.prompt(prompt).call()

【format vs invoke 的差异（Python 侧）】
.format() → 纯字符串，适合打印查看
.invoke() → PromptValue 对象，适合接链式调用
Java 的 .render() 对应 .format()，.create() 对应 .invoke()
*/
