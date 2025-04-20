package org.example.chat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.chat.advisors.ReasoningContentAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
@RestController
public class mcpclient {
    static AtomicReference<McpSyncClient> clientRef = new AtomicReference<>();

    private OllamaChatModel chatModel;
    static SyncMcpToolCallback[] tools ;

    ChatClient Ollamaclient;

    {
        chatModel= OllamaChatModel.builder()
                .ollamaApi(new OllamaApi("http://140.143.170.96:11434"))
                .defaultOptions(OllamaOptions.builder().model("qwen2.5:1.5b").build()).build();
        Ollamaclient= ChatClient.builder(chatModel).
                defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultTools(tools)
                .build();
    }


    @Resource
    private OllamaEmbeddingModel EmbeddingModel;
    public static void main(String[] args) {

        McpSyncClient client = client(transport,
                builder ->
                        builder.roots(new McpSchema.Root("custom://resource/fajsdfkla", "test-root")));
        McpSchema.InitializeResult initialize = client.initialize();
        log.info(initialize.toString());
        client.setLoggingLevel(McpSchema.LoggingLevel.DEBUG);
        tools = client.listTools(null)
                .tools()
                .stream()
                .map(tool -> {
                    return new SyncMcpToolCallback(client, tool);
                })
                .toArray(SyncMcpToolCallback[]::new);


        // List available resources and their names
        List<McpSchema.Resource> resources = client.listResources().resources();
        client.readResource(resources.get(0));


        // List available prompt templates
        McpSchema.ListPromptsResult prompts  = client.listPrompts();
        prompts.prompts().forEach(System.out::println);
        SpringApplication.run(mcpclient.class, args);
    }

    @RequestMapping(method = RequestMethod.GET,value = "/api")
    public String call(@RequestParam("message") String message){

        return  Ollamaclient.prompt()
                .user(message)
                .call().
                content();
    }





    static McpClientTransport transport;

    static {

        transport = HttpClientSseClientTransport.builder("http://localhost:8080//mcp/message").build();
    }

    static McpSyncClient client(McpClientTransport transport, Function<McpClient.SyncSpec, McpClient.SyncSpec> customizer) {
        McpClient.SyncSpec syncSpec = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(100))
                .loggingConsumer(notification -> {//日志消费者
                    System.out.println("Received log message: " + notification.data());
                })
                .capabilities(   //客户端功能配置，核心对象为： McpSchema.ClientCapabilities
                        McpSchema.ClientCapabilities.builder().
                                roots(true).//允许动态更新资源访问边界
                                sampling(). //允许mcpserver 使用llm，并采样
                                experimental(new HashMap<>()).//疑似自定义功能扩展定义
                                build())
                .clientInfo(new McpSchema.Implementation("client", "1.0"));//客户端版本信息，
        //        以上信息的定义主要用于和mcp服务端进行交互

                

        McpClient.SyncSpec apply = customizer.apply(syncSpec);
        clientRef.set(
                apply.build()
        );
        


        return clientRef.get();
    }
}
