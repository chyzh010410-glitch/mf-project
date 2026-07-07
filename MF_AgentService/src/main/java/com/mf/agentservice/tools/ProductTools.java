package com.mf.agentservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.mf.agentservice.client.MfEpClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ProductTools {
    private final MfEpClient mfEpClient;
    private final ToolExecutor executor;

    public ProductTools(MfEpClient mfEpClient, ToolExecutor executor) {
        this.mfEpClient = mfEpClient;
        this.executor = executor;
    }

    @Tool(name = McpToolNames.PRODUCT_SEARCH, description = "Search MF products by keyword and product type.")
    public ToolResult<JsonNode> search(ToolExecutionContext context, String keyword, String productType) {
        return executor.execute(context, McpToolNames.PRODUCT_SEARCH, "keyword=" + keyword + ",type=" + productType,
                () -> {
                    var data = mfEpClient.searchProducts(keyword, productType, 1, 5);
                    return ToolResult.ok(data, "product search returned " + countItems(data) + " items");
                });
    }

    @Tool(name = McpToolNames.PRODUCT_DETAIL, description = "Get a public MF product detail by product id.")
    public ToolResult<JsonNode> detail(ToolExecutionContext context, Long productId) {
        return executor.execute(context, McpToolNames.PRODUCT_DETAIL, "productId=" + productId,
                () -> ToolResult.ok(mfEpClient.productDetail(productId), "product detail loaded"));
    }

    private int countItems(JsonNode data) {
        if (data == null) {
            return 0;
        }
        if (data.has("records") && data.get("records").isArray()) {
            return data.get("records").size();
        }
        if (data.has("list") && data.get("list").isArray()) {
            return data.get("list").size();
        }
        return data.isArray() ? data.size() : 1;
    }
}
