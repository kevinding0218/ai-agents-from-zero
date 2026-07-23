/**
 * 对应 Python：ModelIO_OpenAI.py
 * 原始 OpenAI SDK（不经过 LangChain），直接调用 completions.create
 *
 * Python:
 *   from openai import OpenAI
 *   client = OpenAI(api_key=os.getenv("openai-api"))
 *   response = client.chat.completions.create(
 *       model="gpt-4o-mini",
 *       messages=[{"role": "user", "content": "你是谁？"}],
 *   )
 *   print(response.choices[0].message.content)
 *
 * Python 与 LangChain 的关键区别：
 *   原始 SDK: response.choices[0].message.content
 *   LangChain:  response.content
 *
 * Java 对应：
 *   方案 A（推荐）：Spring AI OpenAiChatModel（Spring 封装，= LangChain 路线）
 *   方案 B（等价原始 SDK）：直接用 OpenAI 官方 Java 客户端 openai-java
 */

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionUserMessageParam;

// ─── 方案 A：Spring AI（推荐，与 LangChain 路线对齐）──────────────────────
// 参见 ModelIO_ChatOpenAI.java，使用 ChatClient 注入即可，代码略。

// ─── 方案 B：原始 OpenAI Java SDK（等价 Python 原始 OpenAI SDK）───────────
// Maven 依赖：
//   <dependency>
//     <groupId>com.openai</groupId>
//     <artifactId>openai-java</artifactId>
//     <version>0.x.x</version>   <!-- 查最新版本 -->
//   </dependency>
public class ModelIO_OpenAI {

    public static void main(String[] args) {
        // Python: client = OpenAI(api_key=os.getenv("openai-api"))
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .build();

        // Python:
        //   response = client.chat.completions.create(
        //       model="gpt-4o-mini",
        //       messages=[{"role": "user", "content": "你是谁？"}],
        //   )
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model("gpt-4o-mini")
                .addUserMessage("你是谁？")
                .build();

        ChatCompletion response = client.chat().completions().create(params);

        // Python: print(response.choices[0].message.content)  ← 原始 SDK 路径
        String content = response.choices().get(0).message().content().orElse("");
        System.out.println("原始 SDK 回答：" + content);

        // 对比：LangChain / Spring AI 路径
        // Python:  response.content      ← LangChain AIMessage
        // Java:    chatResponse.getResult().getOutput().getText()  ← Spring AI
        System.out.println("（LangChain / Spring AI 会直接给你 .content 而非 .choices[0].message.content）");
    }
}

/*
【Java vs Python 对照】
Python from openai import OpenAI                  ↔  Java import com.openai.client.OpenAIClient
Python OpenAI(api_key=...)                        ↔  Java OpenAIOkHttpClient.builder().apiKey(...).build()
Python client.chat.completions.create(...)        ↔  Java client.chat().completions().create(params)
Python response.choices[0].message.content        ↔  Java response.choices().get(0).message().content()

【LangChain vs 原始 SDK 路径对比】
原始 SDK 路径（Python）:  response.choices[0].message.content
LangChain 路径（Python）:  response.content
Spring AI 路径（Java）:    chatResponse.getResult().getOutput().getText()
                           或 chatClient.prompt().call().content()（更简洁）
*/
