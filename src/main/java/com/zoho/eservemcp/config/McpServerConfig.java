package com.zoho.eservemcp.config;

import com.zoho.eservemcp.tools.ZohoPayrollMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {
    
    @Bean
    public ToolCallbackProvider toolCallbackProvider(ZohoPayrollMcpTools payrollTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(payrollTools)
                .build();
    }
}
