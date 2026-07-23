/**
 * 对应 Python：GetEnvInfo.py
 * 环境检查：打印 Spring AI 版本与 Java 运行时信息
 *
 * Python 靠 langchain.__version__ 取版本号。
 * Java 没有同等的运行时属性，但可以从 Maven 打包信息（MANIFEST.MF）里读，
 * 或者直接用 Spring Boot Actuator 的 /actuator/info 端点暴露出去。
 *
 * 依赖（pom.xml）：
 *   <dependency>
 *       <groupId>org.springframework.ai</groupId>
 *       <artifactId>spring-ai-bom</artifactId>           ← BOM 统一管版本
 *       <version>1.0.0</version>
 *       <type>pom</type>
 *       <scope>import</scope>
 *   </dependency>
 *   <dependency>
 *       <groupId>org.springframework.ai</groupId>
 *       <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
 *   </dependency>
 */
public class GetEnvInfo {

    public static void main(String[] args) {
        // Java 版本（对应 Python 的 sys.version）
        System.out.println("Java version:  " + System.getProperty("java.version"));

        // JVM 可执行文件路径（对应 Python 的 sys.executable）
        System.out.println("Java home:     " + System.getProperty("java.home"));

        // Spring AI 版本：从 JAR 的 MANIFEST.MF 里读
        // 如果是 Spring Boot fat-jar，可以用 /META-INF/MANIFEST.MF 里的 Implementation-Version
        Package springAiPkg = org.springframework.ai.chat.model.ChatModel.class.getPackage();
        String springAiVersion = springAiPkg != null ? springAiPkg.getImplementationVersion() : "unknown";
        System.out.println("Spring AI version: " + springAiVersion);

        // 操作系统信息
        System.out.println("OS:            " + System.getProperty("os.name")
                + " " + System.getProperty("os.version"));
    }
}

/*
【输出示例】
Java version:  21.0.3
Java home:     /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
Spring AI version: 1.0.0
OS:            Mac OS X 14.5
*/
