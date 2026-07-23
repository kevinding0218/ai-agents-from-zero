/**
 * 对应 Python：LangChain_Ollama_Memorial.py
 * 向量记忆：Query Rewriting → ChromaDB 检索 → 拼接历史 → 调模型 → 存向量
 *
 * Python 实现的 4 步循环：
 *   1. rewrite_query(user_input)       → 优化查询词
 *   2. retrieve_relevant(rewritten)    → OllamaEmbeddings + Chroma 向量检索相关历史
 *   3. messages = [system, *relevant, HumanMessage(input)]  → 拼接上下文
 *   4. response = model.invoke(messages)
 *   5. store_exchange(input, response.content)  → 存新的对话到 Chroma
 *
 * Python 关键组件：
 *   from langchain_ollama import OllamaEmbeddings
 *   from langchain_chroma import Chroma
 *   embeddings = OllamaEmbeddings(model="nomic-embed-text", base_url="...")
 *   vector_store = Chroma(collection_name="chat_memory", embedding_function=embeddings)
 *
 * Java 对应：
 *   Spring AI 有 EmbeddingModel + VectorStore 体系，整体架构与 Python 完全对应。
 *   Chroma 有 Spring AI Starter：spring-ai-chroma-store-spring-boot-starter。
 *
 * application.properties：
 *   spring.ai.ollama.base-url=http://localhost:11434
 *   spring.ai.ollama.chat.options.model=qwen3:4b
 *   spring.ai.ollama.embedding.options.model=nomic-embed-text
 *   spring.ai.vectorstore.chroma.collection-name=chat_memory
 *   spring.ai.vectorstore.chroma.host=localhost
 *   spring.ai.vectorstore.chroma.port=8000
 */

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

@SpringBootApplication
public class LangChain_Ollama_Memorial {
    public static void main(String[] args) {
        SpringApplication.run(LangChain_Ollama_Memorial.class, args);
    }
}

@Component
class MemorialRunner implements CommandLineRunner {

    @Autowired
    private OllamaChatModel chatModel;

    // Spring AI 自动配置（需要 chroma starter + ollama embedding）
    // Python: vector_store = Chroma(collection_name="chat_memory", embedding_function=embeddings)
    @Autowired
    private VectorStore vectorStore;

    @Override
    public void run(String... args) {
        ChatClient model = ChatClient.create(chatModel);

        Scanner scanner = new Scanner(System.in);
        System.out.println("向量记忆对话已开始，输入 quit 退出");

        while (scanner.hasNextLine()) {
            System.out.print("你：");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("quit")) break;
            if (userInput.isEmpty()) continue;

            // ── 步骤 1：Query Rewriting ────────────────────────────────────
            // Python: rewritten = rewrite_query(user_input)
            // 用模型优化查询词，让向量检索更准确
            String rewritten = model.prompt()
                    .user("请将以下问题改写成更适合向量检索的查询词（保持语义，简洁精确）：\n" + userInput)
                    .call()
                    .content();
            System.out.println("[改写后查询词] " + rewritten);

            // ── 步骤 2：向量检索相关历史 ──────────────────────────────────
            // Python: docs = vector_store.similarity_search(rewritten, k=3)
            List<Document> relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(rewritten).topK(3).build()
            );

            // ── 步骤 3：拼接历史消息 ──────────────────────────────────────
            // Python: messages = [SystemMessage("..."), *relevant_messages, HumanMessage(userInput)]
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个AI助手，请利用下面的对话历史来回答问题"));

            // 把检索到的历史文档还原为消息对
            for (Document doc : relevantDocs) {
                String role = (String) doc.getMetadata().getOrDefault("role", "human");
                if ("human".equals(role)) {
                    messages.add(new UserMessage(doc.getText()));
                } else {
                    messages.add(new AssistantMessage(doc.getText()));
                }
            }
            messages.add(new UserMessage(userInput));

            // ── 步骤 4：调用模型 ──────────────────────────────────────────
            // Python: response = model.invoke(messages)
            String reply = model.prompt()
                    .messages(messages)
                    .call()
                    .content();

            System.out.println("AI：" + reply);

            // ── 步骤 5：存新对话到向量库 ──────────────────────────────────
            // Python: store_exchange(user_input, response.content)
            // 用户消息 + AI 回复分别存为两个 Document
            vectorStore.add(List.of(
                    new Document(userInput, Map.of("role", "human")),
                    new Document(reply, Map.of("role", "ai"))
            ));

            System.out.println("-".repeat(50));
        }
    }
}

/*
【Java vs Python 对照】
Python OllamaEmbeddings(model="nomic-embed-text")
  ↔ Java spring.ai.ollama.embedding.options.model=nomic-embed-text（自动配置）

Python Chroma(collection_name="chat_memory", embedding_function=embeddings)
  ↔ Java @Autowired VectorStore（spring-ai-chroma-store-spring-boot-starter）

Python vector_store.similarity_search(query, k=3)
  ↔ Java vectorStore.similaritySearch(SearchRequest.builder().query(q).topK(3).build())

Python vector_store.add_texts([text], metadatas=[{...}])
  ↔ Java vectorStore.add(List.of(new Document(text, metadata)))

【架构说明】
4 步向量记忆循环（Python 与 Java 架构完全一致）：
  Query Rewriting → ChromaDB 检索 → 拼接上下文 → 调模型 → 存向量
不同的只是语言语法，底层 Embedding + VectorStore 逻辑完全相同。
*/
