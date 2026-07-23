/**
 * 对应 Python：
 *   StructuredOutput_TypedDict.py  → with_structured_output(AnimalList)  → dict
 *   StructuredOutput_Pydantic.py   → with_structured_output(Product)      → Pydantic 实例
 *
 * Python TypedDict 版：
 *   class Animal(TypedDict):
 *       animal: Annotated[str, "动物"]
 *       emoji: Annotated[str, "表情"]
 *   class AnimalList(TypedDict):
 *       animals: Annotated[list[Animal], "动物与表情列表"]
 *
 *   llm_with_structured_output = llm.with_structured_output(AnimalList)
 *   resp = llm_with_structured_output.invoke(messages)
 *   # resp = {'animals': [{'animal': '狗', 'emoji': '🐶'}, ...]}
 *
 * Python Pydantic 版：
 *   class Product(BaseModel):
 *       name: str = Field(description="商品名称")
 *       category: str = Field(description="商品类别")
 *       description: str = Field(description="商品描述")
 *
 *   llm_with_structured_output = llm.with_structured_output(Product)
 *   response = llm_with_structured_output.invoke(messages)
 *   # response = Product(name="华为Mate X7", ...)  ← Pydantic 对象
 *
 * Java 对应：
 *   BeanOutputConverter<T> 同时覆盖两种场景：
 *   - 生成格式说明（等价 get_format_instructions / JSON Schema）
 *   - 解析输出（返回强类型 Java 对象，等价 Pydantic 实例）
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootApplication
public class StructuredOutputDemo {
    public static void main(String[] args) {
        SpringApplication.run(StructuredOutputDemo.class, args);
    }
}

// ─── TypedDict 版对应（对应 Python Animal + AnimalList TypedDict）──────────
// Python: class Animal(TypedDict): animal: Annotated[str,"动物"]; emoji: Annotated[str,"表情"]
// Java:   record Animal(String animal, String emoji)
record Animal(String animal, String emoji) {}

// Python: class AnimalList(TypedDict): animals: Annotated[list[Animal],"动物与表情列表"]
record AnimalList(List<Animal> animals) {}

// ─── Pydantic 版对应（对应 Python Product BaseModel）─────────────────────────
// Python: class Product(BaseModel):
//             name: str = Field(description="商品名称")
//             category: str = Field(description="商品类别")
//             description: str = Field(description="商品描述")
record Product(String name, String category, String description) {}

@Component
class StructuredOutputRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StructuredOutputRunner.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        ChatClient llm = chatClientBuilder.build();

        // ─── TypedDict 版（对应 StructuredOutput_TypedDict.py）────────────────
        System.out.println("【TypedDict 版：AnimalList 结构化输出】");

        // Python: llm.with_structured_output(AnimalList).invoke(messages)
        // Java:   BeanOutputConverter<AnimalList> 自动生成格式说明 + 解析
        BeanOutputConverter<AnimalList> animalConverter = new BeanOutputConverter<>(AnimalList.class);

        // Python: Annotated[str, "动物"] 里的描述 → JSON Schema 里的 description
        // Java:   BeanOutputConverter.getFormat() 生成类似的 JSON Schema 说明
        String animalJson = llm.prompt()
                .system("你只能输出 JSON，不要有任何多余文字。" + animalConverter.getFormat())
                .user("任意生成三种动物，以及他们的 emoji 表情")
                .call()
                .content();

        AnimalList animalList = animalConverter.convert(animalJson);

        // Python: print(resp)  → {'animals': [{'animal': '狗', 'emoji': '🐶'}, ...]}
        logger.info("TypedDict 版结果：{}", animalList);
        logger.info("结果类型：{}", animalList.getClass().getSimpleName());

        System.out.println("\n" + "=".repeat(50));

        // ─── Pydantic 版（对应 StructuredOutput_Pydantic.py）──────────────────
        System.out.println("【Pydantic 版：Product 结构化输出】");

        // Python: llm.with_structured_output(Product).invoke(messages)
        // Java:   BeanOutputConverter<Product>
        BeanOutputConverter<Product> productConverter = new BeanOutputConverter<>(Product.class);

        String productJson = llm.prompt()
                .system("你只能输出 JSON，不要有任何多余文字。" + productConverter.getFormat())
                .user("请介绍一款最新的华为旗舰手机，用 JSON 格式返回商品信息")
                .call()
                .content();

        Product product = productConverter.convert(productJson);

        // Python: print(response)        → Product(name='华为Mate X7', ...)
        // Python: print(response.name)   → 华为Mate X7
        logger.info("Pydantic 版结果：{}", product);
        logger.info("商品名称：{}", product.name());   // Python: response.name
        logger.info("商品类别：{}", product.category());
        logger.info("商品描述：{}", product.description());
        logger.info("结果类型：{}", product.getClass().getSimpleName());
    }
}

/*
【Java vs Python 结构化输出对照】

Python with_structured_output(TypedDict) → dict
  ↔ Java BeanOutputConverter<Record>.convert(json) → Record 实例（强类型，比 dict 更好）

Python with_structured_output(BaseModel) → Pydantic 实例（可 .name, .category 访问）
  ↔ Java BeanOutputConverter<Record>.convert(json) → Record 实例（可 .name(), .category()）

Python class Animal(TypedDict): animal: Annotated[str, "动物"]
  ↔ Java record Animal(String animal, String emoji)
  Annotated 描述 → 对应 Java 的 @JsonProperty / @JsonPropertyDescription（可选）

Python llm.with_structured_output(Schema)
  ↔ Java BeanOutputConverter<T>（getFormat() 生成说明 + convert() 解析结果）

【底层机制（两者完全一致）】
Python: Annotated 描述 → JSON Schema → system prompt 里的格式说明 → Function Calling / Tool Use
Java:   BeanOutputConverter.getFormat() → JSON Schema → system prompt 里的格式说明

两者都是把类型定义转成 JSON Schema，注入到 prompt 里告诉模型要输出什么格式。
模型返回 JSON 字符串，然后解析成目标类型（Python dict/Pydantic，Java record/class）。
*/
