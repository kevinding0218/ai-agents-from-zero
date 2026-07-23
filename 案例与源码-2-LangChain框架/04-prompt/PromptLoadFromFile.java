/**
 * 对应 Python：
 *   13.4-load_external/PromptLoadFromJson.py  → load_prompt("prompt.json")
 *   13.4-load_external/PromptLoadFromYaml.py  → load_prompt("prompt.yaml")
 *
 * Python:
 *   from langchain_core.prompts import load_prompt
 *   template = load_prompt("prompt.json", encoding="utf-8")
 *   print(template.format(name="张三", what="搞笑的"))   # 请张三讲一个搞笑的故事
 *
 *   template2 = load_prompt("prompt.yaml", encoding="utf-8")
 *   print(template2.format(name="年轻人", what="滑稽"))  # 请年轻人讲一个滑稽的故事
 *
 * Java 对应：
 *   Spring AI 没有内置 load_prompt，但可以从 classpath 读取 JSON/YAML 文件，
 *   用 Spring 的 @Value 或 ResourceLoader 加载文本，再用 PromptTemplate 渲染。
 *
 * prompt.json（放在 src/main/resources/）：
 *   {"template": "请{name}讲一个{what}的故事", "_type": "prompt"}
 *
 * prompt.yaml（放在 src/main/resources/）：
 *   template: "请{name}讲一个{what}的故事"
 *   _type: prompt
 */

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@SpringBootApplication
public class PromptLoadFromFile {
    public static void main(String[] args) {
        SpringApplication.run(PromptLoadFromFile.class, args);
    }
}

@Component
class LoadFromFileRunner implements CommandLineRunner {

    // 方式 A：Spring AI 原生支持从 classpath 加载 PromptTemplate（.st 格式）
    // Python: template = load_prompt("prompt.json")
    // Java:  @Value("classpath:/prompts/story.st") Resource promptResource;
    @Value("classpath:/prompts/story.st")
    private Resource promptResource;

    private final ResourceLoader resourceLoader;

    public LoadFromFileRunner(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) throws Exception {
        // ─── 方式 A：用 Spring AI 内置资源加载 .st 文件 ──────────────────
        System.out.println("【方式 A：从 classpath .st 文件加载（Spring AI 内置）】");
        try {
            // Spring AI PromptTemplate(Resource) 直接从文件读取模板文本
            PromptTemplate templateFromFile = new PromptTemplate(promptResource);
            String result = templateFromFile.render(Map.of("name", "张三", "what", "搞笑的"));
            System.out.println(result);
        } catch (Exception e) {
            System.out.println("（需要在 src/main/resources/prompts/story.st 放模板文件才能运行）");
            System.out.println("story.st 内容示例：请{name}讲一个{what}的故事");
        }

        System.out.println("\n" + "=".repeat(50));

        // ─── 方式 B：手动读取 JSON/YAML 文件，提取 template 字段 ─────────
        // Python: load_prompt("prompt.json") 读取 JSON 里的 template 字段
        // Java: 手动读文件，提取 template 字段
        System.out.println("【方式 B：手动读取 JSON/YAML 提取模板字符串（等价 load_prompt）】");

        // 模拟从文件里读到的模板字符串
        // 实际项目用 Jackson 解析 JSON: objectMapper.readTree(json).get("template").asText()
        // 或 SnakeYAML 解析 YAML: yaml.load(reader).get("template")
        String templateFromJson = "请{name}讲一个{what}的故事";   // JSON 里 "template" 字段的值
        PromptTemplate template = new PromptTemplate(templateFromJson);

        // Python: template.format(name="张三", what="搞笑的")
        System.out.println("JSON 版：" + template.render(Map.of("name", "张三", "what", "搞笑的")));
        System.out.println("YAML 版：" + template.render(Map.of("name", "年轻人", "what", "滑稽")));
    }
}

/*
【Java vs Python 对照】
Python load_prompt("prompt.json", encoding="utf-8")
  ↔ Java 方式 A：new PromptTemplate(Resource)（Spring AI 内置，支持 .st 文件）
  ↔ Java 方式 B：读 JSON/YAML → 提取 template 字段 → new PromptTemplate(str)

Python template.format(name="张三", what="搞笑的")
  ↔ Java template.render(Map.of("name","张三","what","搞笑的"))

【文件格式差异】
Python load_prompt 支持 JSON 和 YAML，两者都认 _type 字段。
Java Spring AI 原生 PromptTemplate(Resource) 支持 .st（StringTemplate）格式。
如果要加载 JSON/YAML，最实用的方式是用 Jackson/SnakeYAML 解析后提取模板字符串。

【配置建议】
实际项目里把提示词放文件的目的：版本管理、A/B 测试、多人协作。
Java Spring Boot 用 @Value("classpath:/prompts/xxx.st") 配合 PromptTemplate(Resource) 最简洁。
*/
