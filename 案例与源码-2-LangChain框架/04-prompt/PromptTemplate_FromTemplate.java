/**
 * 对应 Python：13.2-prompt_templates/PromptTemplate_FromTemplate.py
 * PromptTemplate.from_template()：自动检测 {变量名}
 *
 * Python:
 *   from langchain_core.prompts import PromptTemplate
 *   template = PromptTemplate.from_template("你好，{name}！请介绍一下{topic}。")
 *   prompt_str = template.format(name="小明", topic="AI")
 *   print(prompt_str)  # 你好，小明！请介绍一下AI。
 *
 * Java 对应：
 *   Spring AI 用 PromptTemplate 类，API 几乎相同。
 *   核心概念：占位符 {name} 被替换为实际值，最终得到字符串或 Prompt 对象。
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Map;

@SpringBootApplication
public class PromptTemplate_FromTemplate {
    public static void main(String[] args) {
        SpringApplication.run(PromptTemplate_FromTemplate.class, args);
    }
}

@Component
class FromTemplateRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        // Python: template = PromptTemplate.from_template("你好，{name}！请介绍一下{topic}。")
        // Java: new PromptTemplate("你好，{name}！请介绍一下{topic}。")
        PromptTemplate template = new PromptTemplate("你好，{name}！请简短介绍一下{topic}。");

        // Python: prompt_str = template.format(name="小明", topic="AI")
        // Java:   template.render(Map.of(...)) → 返回字符串
        String promptStr = template.render(Map.of("name", "小明", "topic", "AI"));

        // Python: print(prompt_str)
        System.out.println("格式化后的字符串：" + promptStr);

        // Python: type(prompt_str) → <class 'str'>
        System.out.println("类型：" + promptStr.getClass().getSimpleName());

        System.out.println("\n" + "=".repeat(50));

        // 进一步：把 PromptTemplate 结果发给模型
        // Python: model.invoke(template.format(...))  ← 传字符串也可以
        // Java:   template.create(vars).getContents() 得到字符串，直接 .user() 传入
        ChatClient model = chatClientBuilder.build();
        String response = model.prompt()
                .user(promptStr)
                .call()
                .content();
        System.out.println("模型回答：" + response);
    }
}

/*
【Java vs Python 对照】
Python PromptTemplate.from_template("你好，{name}！")
  ↔ Java new PromptTemplate("你好，{name}！")

Python template.format(name="小明", topic="AI")   ↔  Java template.render(Map.of("name","小明","topic","AI"))
Python 结果类型 str                                ↔  Java String（都是纯字符串）
Python model.invoke(prompt_str)                   ↔  Java chatClient.prompt().user(promptStr).call()

【注意】
Python PromptTemplate 用 {variable} 占位符（单花括号）。
Java Spring AI PromptTemplate 同样用 {variable}（单花括号），格式一致。
*/
