package org.example;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
public class mcpclient {
    static AtomicReference<McpSyncClient> clientRef = new AtomicReference<>();

    public static void main(String[] args) {

        McpSyncClient client = client(transport, builder -> builder.roots(new McpSchema.Root("file:///test/path", "test-root")));

        McpSchema.InitializeResult initialize = client.initialize();
        log.info(initialize.toString());

        SyncMcpToolCallback[] array = client.listTools(null)
                .tools()
                .stream()
                .map(tool -> {
                    return new SyncMcpToolCallback(client, tool);
                })
                .toArray(SyncMcpToolCallback[]::new);


        ConfigurableApplicationContext run = SpringApplication.run(mcpclient.class, args);
    }


    static McpClientTransport transport;

    static {

        transport = HttpClientSseClientTransport.builder("http://localhost:8080/mcp-server").build();
    }

    static McpSyncClient client(McpClientTransport transport, Function<McpClient.SyncSpec, McpClient.SyncSpec> customizer) {
        McpClient.SyncSpec syncSpec = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
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
