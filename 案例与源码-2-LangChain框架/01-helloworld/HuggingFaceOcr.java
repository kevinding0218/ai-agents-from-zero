/**
 * 对应 Python：LangChain_huggingface_ocr_model.py
 * 非聊天模型：图像分类（ViT）+ 多行 OCR（GOT-OCR-2.0）
 *
 * Python 做法：
 *   # 示例 A：pipeline("image-classification", model="google/vit-base-patch16-224")
 *   classifier = pipeline("image-classification", model="google/vit-base-patch16-224")
 *   results = classifier(image, top_k=3)
 *
 *   # 示例 B：AutoProcessor + AutoModel 多行 OCR
 *   processor = AutoProcessor.from_pretrained("stepfun-ai/GOT-OCR-2.0-hf")
 *   model = AutoModelForImageTextToText.from_pretrained(...)
 *   inputs = processor(image, return_tensors="pt")
 *   generated_ids = model.generate(**inputs)
 *
 * ============================================================
 * Java / Spring AI 没有直接等价写法。
 *
 * 原因：
 *   这类模型（图像分类、OCR）属于"非聊天模型"，Spring AI 主要面向 LLM 聊天场景。
 *   Python 的 transformers 库对 CV/OCR 任务有原生支持。
 *
 * Java 里的两种替代方案：
 * ============================================================
 *
 * 方案 A（推荐）：直接调用 HuggingFace Inference API（HTTP 调用，无需本地 GPU）
 * 方案 B：多模态聊天模型（如 GPT-4o / Qwen-VL），把图片连同问题一起发给模型
 * ============================================================
 *
 * 下面演示两种方案：
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class HuggingFaceOcr {
    public static void main(String[] args) {
        SpringApplication.run(HuggingFaceOcr.class, args);
    }
}

@Component
class OcrRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) throws Exception {
        demonstrateHuggingFaceApi();   // 方案 A
        demonstrateMultimodalChat();   // 方案 B
    }

    // ========== 方案 A：调用 HuggingFace Inference API（HTTP）==========
    // 对应 Python: pipeline("image-classification", model="google/vit-base-patch16-224")
    // Java 直接用 RestTemplate / WebClient 调 HuggingFace 的 REST 接口
    void demonstrateHuggingFaceApi() throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("方案 A：调用 HuggingFace Inference API（HTTP REST）");
        System.out.println("=".repeat(60));

        // 准备图片（读本地文件，转 byte[]）
        // Python: test_img = Image.new("RGB", (224, 224), color=(255, 165, 0))
        byte[] imageBytes = Files.readAllBytes(Path.of("/path/to/your/image.jpg"));

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + System.getenv("HF_TOKEN"));
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);  // 直接发图片字节

        HttpEntity<byte[]> request = new HttpEntity<>(imageBytes, headers);

        // Python: results = classifier(test_img, top_k=3)
        // Java: POST https://api-inference.huggingface.co/models/google/vit-base-patch16-224
        ResponseEntity<List> response = restTemplate.exchange(
                "https://api-inference.huggingface.co/models/google/vit-base-patch16-224",
                HttpMethod.POST,
                request,
                List.class
        );

        // Python: for r in results: print(f"{r['label']}  {r['score']:.4f}")
        System.out.println("分类结果：");
        if (response.getBody() != null) {
            response.getBody().forEach(item -> {
                Map<?, ?> result = (Map<?, ?>) item;
                System.out.printf("  %-30s  %.4f%n", result.get("label"), result.get("score"));
            });
        }
    }

    // ========== 方案 B：用多模态聊天模型做 OCR（Spring AI 原生支持）==========
    // 对应 Python: processor + model.generate() 做 OCR
    // Java: 把图片作为消息附件发给支持视觉的聊天模型（Qwen-VL / GPT-4o 等）
    void demonstrateMultimodalChat() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("方案 B：多模态聊天模型（把图片发给 LLM 做 OCR）");
        System.out.println("=".repeat(60));

        // 读取图片（Base64 编码后放进消息）
        byte[] imageBytes = Files.readAllBytes(Path.of("/path/to/your/image.jpg"));

        // Spring AI 的 Media 类：把图片嵌入消息（对应 Python 里直接把 PIL.Image 传给模型）
        // 需要支持视觉的模型：qwen-vl-plus / gpt-4o 等
        // application.properties 里把 model 改成支持视觉的版本
        ChatClient chatClient = chatClientBuilder.build();

        // Python: inputs = processor(image, return_tensors="pt"); model.generate(**inputs)
        // Java:   把图片字节 + 问题文本一起发给多模态聊天模型
        String reply = chatClient.prompt()
                .messages(new UserMessage(
                        "请识别图中所有文字，按原始布局输出",
                        List.of(new Media(MimeTypeUtils.IMAGE_JPEG, imageBytes))
                ))
                .call()
                .content();

        // Python: print("【OCR 提取结果】"); print(extracted_text)
        System.out.println("【OCR 提取结果（多模态模型）】");
        System.out.println(reply);
    }
}

/*
【Java vs Python 非聊天模型对照】

Python transformers.pipeline("image-classification")
  ↔ Java RestTemplate POST HuggingFace Inference API（方案 A）
  ↔ 或 Java ChatClient + 多模态模型（方案 B，如果只需要理解图片内容）

Python AutoProcessor + model.generate()（OCR）
  ↔ Java 多模态聊天模型（Qwen-VL / GPT-4o）+ Spring AI Media 附件（方案 B）
  ↔ 或 Python FastAPI 暴露 OCR 接口，Java 调用（最灵活）

【核心差异】
CV/OCR 任务是 Python + PyTorch 生态的强项。
Java 在这个方向上最实用的做法是：
  - 简单图片理解 → 多模态 LLM（方案 B）
  - 精确 OCR / 分类 → HuggingFace Inference API（方案 A）
  - 必须本地推理 → Python 微服务 + Java 调用
*/
