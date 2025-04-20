package org.example.server;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.List;

@SpringBootApplication
public class mcpServer {
    public static void main(String[] args) {
//        1,启动web服务： mcp的server服务的传输本质上是servlet定义，此处使用类springboot的web容器来对接外部
        SpringApplication springApplication = new SpringApplication(mcpServer.class);
        ConfigurableApplicationContext context = SpringApplication.run(mcpServer.class, args);


//        2
        McpSyncServer server = server(context.getBean(HttpServletSseServerTransportProvider.class));
        server.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                .level(McpSchema.LoggingLevel.INFO)
                .logger("custom-logger")
                .data("Custom log message")
                .build());

//        "id" : "urn:jsonschema:Operation",
                // Sync tool specification
        var schema = """
                {
                  "type" : "object",
                 "id" : "urn:jsonschema:Operation",
                  "properties" : {
                    "city" : {
                      "type" : "string"
                    }
                  }
                }
                """;
        McpServerFeatures.SyncToolSpecification syncToolSpecification = new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("天气查询", "根据城市查询天气", schema),
                (exchange, arguments) -> {
                    exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                            .level(McpSchema.LoggingLevel.ERROR)
                            .logger("custom-logger")
                            .data("Custom log message")
                            .build());
                    exchange.createMessage(new McpSchema.CreateMessageRequest())
                    return new McpSchema.CallToolResult("晴朗", false);
                }
        );
        server.addTool(syncToolSpecification);
        McpSchema.ServerCapabilities serverCapabilities = server.getServerCapabilities();

    }


    public static McpSyncServer server(HttpServletSseServerTransportProvider transportProvider) {
        McpServerFeatures.SyncResourceSpecification syncResourceSpecification = new McpServerFeatures.SyncResourceSpecification(
                new McpSchema.Resource("custom://resource", "test-root", "全局资源", "mime-type", null),
                (exchange, request) -> {
                    McpSchema.ClientCapabilities.RootCapabilities roots = exchange.getClientCapabilities().roots();
                    exchange.listRoots();// 客户端所提供的根列表，目前似乎需要根据这个列表手动约束，


                    // Resource read implementation
                    McpSchema.TextResourceContents textResourceContents = new McpSchema.TextResourceContents("jflksadjklf", "fas", "fasdf");
                    return new McpSchema.ReadResourceResult(List.of(textResourceContents));
                }
        );
        McpSyncServer build = McpServer.sync(transportProvider)
                .serverInfo("test-server", "1.0.0")
                .capabilities(new McpSchema.ServerCapabilities(
                        new HashMap<>()
                        , new McpSchema.ServerCapabilities.LoggingCapabilities()
                        , new McpSchema.ServerCapabilities.PromptCapabilities(true)
                        , new McpSchema.ServerCapabilities.ResourceCapabilities(true, true)
                        , new McpSchema.ServerCapabilities.ToolCapabilities(true)
                ))
                .build();


        build.addResource(syncResourceSpecification);

        return build;

    }

}
