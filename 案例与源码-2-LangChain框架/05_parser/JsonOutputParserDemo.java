/**
 * 对应 Python：JsonOutputParserDemo.py + JsonOutputParser_GetFormatInstructions.py
 *
 * 文件一：JsonOutputParserDemo.py
 *   在 prompt 里直接说"返回 json 格式，q字段表示问题，a字段表示答案"
 *   用 JsonOutputParser() 解析模型返回的 JSON 文本 → dict
 *
 * 文件二：JsonOutputParser_GetFormatInstructions.py
 *   用 Pydantic 模型定义结构（Person: time/person/event）
 *   parser.get_format_instructions() → 生成格式说明文本，拼进 prompt
 *   模型按 schema 输出 → JsonOutputParser.invoke() → dict
 *
 * Java 对应：
 *   方式 A（类比文件一）：在 system 消息里手写 JSON 格式要求，用 Jackson 解析
 *   方式 B（类比文件二）：用 BeanOutputConverter（等价 get_format_instructions + 解析）
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.Map;

@SpringBootApplication
public class JsonOutputParserDemo {
    public static void main(String[] args) {
        SpringApplication.run(JsonOutputParserDemo.class, args);
    }
}

// 对应 Python: class Person(BaseModel): time: str; person: str; event: str
record Person(String time, String person, String event) {}

// 对应 Python: class QA: q: str; a: str (用于文件一的简单结构)
record QA(String q, String a) {}

@Component
class JsonParserRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(JsonParserRunner.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) throws Exception {
        ChatClient model = chatClientBuilder.build();
        ObjectMapper mapper = new ObjectMapper();

        // ─── 方式 A：手写 JSON 格式要求（对应 JsonOutputParserDemo.py）───────────
        // Python: system = "你是一个{role}，结果返回json格式，q字段表示问题，a字段表示答案"
        //         parser = JsonOutputParser()
        //         response = parser.invoke(result)  → dict
        System.out.println("【方式 A：手写格式要求 + Jackson 解析（等价 JsonOutputParser）】");

        String rawJson = model.prompt()
                .system("你是一个AI助手，你只能输出结构化JSON数据。结果返回json格式，q字段表示问题，a字段表示答案。")
                .user("什么是LangChain，简洁回答100字以内")
                .call()
                .content();
        logger.info("模型原始输出：{}", rawJson);

        // Python: response = parser.invoke(result)  → dict
        // Java: Jackson 解析 JSON 字符串 → Map
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = mapper.readValue(rawJson, Map.class);
        logger.info("解析后的结构化结果：{}", parsed);
        logger.info("结果类型：{}", parsed.getClass().getSimpleName());  // HashMap

        System.out.println("\n" + "=".repeat(50));

        // ─── 方式 B：BeanOutputConverter（对应 JsonOutputParser_GetFormatInstructions.py）──
        // Python: class Person(BaseModel): time: str; person: str; event: str
        //         parser = JsonOutputParser(pydantic_object=Person)
        //         format_instructions = parser.get_format_instructions()
        //         prompt = chat_prompt.format_messages(topic=..., format_instructions=...)
        //         response = parser.invoke(result)  → dict

        System.out.println("【方式 B：BeanOutputConverter（等价 get_format_instructions + 解析）】");

        // Python: JsonOutputParser(pydantic_object=Person)
        // Java:   BeanOutputConverter<Person>（自动生成格式说明 + 解析结果）
        BeanOutputConverter<Person> converter = new BeanOutputConverter<>(Person.class);

        // Python: format_instructions = parser.get_format_instructions()
        //         在 prompt 里加 {format_instructions}
        // Java:   converter.getFormat() 返回格式说明字符串，手动拼进 prompt
        String formatInstructions = converter.getFormat();
        logger.info("格式说明（等价 get_format_instructions）：\n{}", formatInstructions);

        // Python: prompt = chat_prompt.format_messages(topic="小米su7", format_instructions=...)
        String jsonOutput = model.prompt()
                .system("你是一个AI助手，你只能输出结构化JSON数据。" + formatInstructions)
                .user("请生成一个关于小米su7跑车的新闻")
                .call()
                .content();
        logger.info("模型原始输出：{}", jsonOutput);

        // Python: response = parser.invoke(result)  → dict（含 time/person/event）
        // Java:   converter.convert(text) → Person 对象（强类型，类似 Pydantic 实例）
        Person personResult = converter.convert(jsonOutput);
        logger.info("解析后的结构化结果：{}", personResult);
        logger.info("结果类型：{}", personResult.getClass().getSimpleName());  // Person
    }
}

/*
【Java vs Python 对照】

文件一（JsonOutputParserDemo.py）：
Python JsonOutputParser()                    ↔  Java Jackson ObjectMapper（手动解析）
Python parser.invoke(result)  → dict        ↔  Java mapper.readValue(json, Map.class)
Python {"q": "...", "a": "..."}             ↔  Java Map<String, Object>

文件二（JsonOutputParser_GetFormatInstructions.py）：
Python class Person(BaseModel)               ↔  Java record Person(String time, ...)
Python JsonOutputParser(pydantic_object=Person) ↔  Java BeanOutputConverter<Person>
Python parser.get_format_instructions()      ↔  Java converter.getFormat()
Python parser.invoke(result)  → dict        ↔  Java converter.convert(text) → Person 对象

【关键差异】
Python 的 JsonOutputParser.invoke() 结果是 dict（不是 Pydantic 实例），
  若要 Pydantic 实例需用 PydanticOutputParser。
Java 的 BeanOutputConverter.convert() 直接返回强类型 Java 对象（record / class），
  等价于 Python 的 PydanticOutputParser（更强类型的那个）。
*/
