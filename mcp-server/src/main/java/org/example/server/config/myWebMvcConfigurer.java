package org.example.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
@EnableWebMvc
public class myWebMvcConfigurer implements WebMvcConfigurer {
    @Bean
    public HttpServletSseServerTransportProvider servletSseServerTransportProvider() {
        return new HttpServletSseServerTransportProvider(new ObjectMapper(), "/mcp/message");
    }



//    spring.web提供的servlet3.0之后的注册钩子，默认映射路径是"/"
    @Bean
    public ServletRegistrationBean customServletBean(HttpServletSseServerTransportProvider transportProvider) {
        return new ServletRegistrationBean(transportProvider);
    }
}
