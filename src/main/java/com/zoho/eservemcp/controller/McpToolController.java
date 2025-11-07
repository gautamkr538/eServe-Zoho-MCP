package com.zoho.eservemcp.controller;

import com.zoho.eservemcp.tools.ZohoPayrollMcpTools;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
public class McpToolController {

    private static final Logger log = LoggerFactory.getLogger(McpToolController.class);

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * List available tools
     */
    @GetMapping("/tools")
    public ResponseEntity<?> listTools() {
        try {
            ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
            
            List<Map<String, Object>> tools = new ArrayList<>();
            for (ToolCallback callback : callbacks) {
                var def = callback.getToolDefinition();
                tools.add(Map.of(
                    "name", def.name(),
                    "description", def.description(),
                    "inputSchema", def.inputSchema()
                ));
            }

            return ResponseEntity.ok(Map.of("tools", tools));
        } catch (Exception e) {
            log.error("Error listing tools", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Call tool directly
     */
    @PostMapping("/call")
    public ResponseEntity<?> callTool(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("name");
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");

        try {
            log.info("Calling tool: {} with args: {}", toolName, arguments);

            // Find tool
            ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
            ToolCallback targetTool = null;
            
            for (ToolCallback callback : callbacks) {
                if (callback.getToolDefinition().name().equals(toolName)) {
                    targetTool = callback;
                    break;
                }
            }

            if (targetTool == null) {
                return ResponseEntity.status(404)
                    .body(Map.of("error", "Tool not found: " + toolName));
            }

            // Execute tool
            String argsJson = objectMapper.writeValueAsString(arguments != null ? arguments : Map.of());
            String result = targetTool.call(argsJson);

            log.info("Tool {} executed successfully", toolName);

            return ResponseEntity.ok(Map.of(
                "result", parseJson(result),
                "success", true
            ));

        } catch (Exception e) {
            log.error("Error calling tool: {}", toolName, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "success", false
            ));
        }
    }

    private Object parseJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
