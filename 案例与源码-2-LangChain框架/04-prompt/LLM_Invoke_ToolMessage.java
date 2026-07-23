/**
 * 对应 Python：13.1-invoke/LLM_Invoke_ToolMessage.py
 * Tool Calling 两次调用流程：bind_tools → AIMessage(tool_calls) → 执行工具 → ToolMessage → 最终回答
 *
 * Python 流程（两次 invoke）：
 *   # 1. 定义工具
 *   def get_weather(city: str) -> str: ...
 *   tools = [get_weather, get_time]
 *
 *   # 2. 绑定工具
 *   llm_with_tools = model.bind_tools(tools)
 *
 *   # 3. 第一次 invoke：模型返回 tool_calls
 *   response = llm_with_tools.invoke(messages)
 *   tool_call = response.tool_calls[0]  # {"name": "get_weather", "args": {"city": "北京"}}
 *
 *   # 4. 执行工具
 *   tool_result = tool_registry[tool_call["name"]](**tool_call["args"])
 *
 *   # 5. 追加 ToolMessage，第二次 invoke
 *   messages.append(response)
 *   messages.append(ToolMessage(tool_result, tool_call_id=tool_call["id"]))
 *   final_response = llm_with_tools.invoke(messages)
 *
 * Java 对应：Spring AI 的 @Tool 注解 + ChatClient 自动处理工具调用循环。
 * 更底层的两次调用参见注释部分的手动写法。
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;

@SpringBootApplication
public class LLM_Invoke_ToolMessage {
    public static void main(String[] args) {
        SpringApplication.run(LLM_Invoke_ToolMessage.class, args);
    }

    // Python: def get_weather(city: str) -> str: return f"{city} 晴天 25°C"
    // Java: @Bean Function（Spring AI 的工具定义方式）
    @Bean
    @Description("获取指定城市的天气")
    public Function<GetWeatherRequest, String> get_weather() {
        return request -> request.city() + " 晴天 25°C";
    }

    @Bean
    @Description("获取当前时间")
    public Function<Map<String, Object>, String> get_time() {
        return params -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

record GetWeatherRequest(String city) {}

@Component
class ToolMessageRunner implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) {
        // Python: llm_with_tools = model.bind_tools([get_weather, get_time])
        // Java: ChatClient 在 prompt 时指定工具名（@Bean 名即工具名）
        ChatClient llmWithTools = chatClientBuilder.build();

        // Python 两次调用流程：
        //   第一次 → 模型决定调工具 → ToolMessage → 第二次 → 最终回答
        // Java：Spring AI 默认自动循环（等价 Python 的 AgentExecutor）
        //        toolCallingMode = MANUAL 可控制是否手动处理
        String result = llmWithTools.prompt()
                .user("北京今天天气怎么样？现在几点了？")
                .tools("get_weather", "get_time")   // 绑定工具（对应 bind_tools）
                .call()
                .content();   // 自动完成：调用工具 → 把结果追加 → 再次调用 → 返回最终文本

        System.out.println("最终回答：" + result);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("【注意：以上是 Spring AI 自动工具调用循环（推荐写法）】");
        System.out.println("Python 手动两次 invoke 的逻辑在 Spring AI 里由框架自动完成。");
        System.out.println("如需手动控制，可使用 toolCallingMode = ToolCallingMode.NONE");
        System.out.println("然后自己解析 response.toolCalls()，执行工具，追加 ToolMessage，再次 invoke。");
    }
}

/*
【Java vs Python 工具调用对照】
Python def get_weather(city: str) -> str
  ↔ Java @Bean @Description Function<GetWeatherRequest, String>

Python model.bind_tools([get_weather, get_time])
  ↔ Java chatClient.prompt().tools("get_weather", "get_time")

Python 两次 invoke 流程（手动）：
  response1 = llm_with_tools.invoke(messages)     # 返回 tool_calls
  result = tool_registry[name](**args)            # 执行工具
  messages.append(ToolMessage(result, ...))       # 追加结果
  final = llm_with_tools.invoke(messages)         # 再次调用

  ↔ Java 自动循环：chatClient.prompt().tools(...).call().content()（框架自动处理两次调用）

Python tool_call["id"]    ↔  Java 由框架管理，无需手动追踪
Python ToolMessage(...)   ↔  Java 由框架自动构建和追加

【Python vs Java 工具调用模式区别】
Python LangChain 展示了完整的手动两次调用过程，帮助理解底层机制。
Java Spring AI 默认自动完成整个循环，生产代码更简洁；需要理解底层时可设置 MANUAL 模式。
*/
