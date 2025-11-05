package com.zoho.eservemcp;

import com.zoho.eservemcp.tools.ZohoPayrollMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EServeZohoMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(EServeZohoMcpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider payrollTools(ZohoPayrollMcpTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}