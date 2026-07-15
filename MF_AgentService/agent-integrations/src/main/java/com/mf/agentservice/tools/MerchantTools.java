package com.mf.agentservice.tools;

import org.springframework.ai.tool.annotation.Tool;
import com.mf.agentservice.agent.port.MerchantToolPort;
import org.springframework.stereotype.Component;

@Component
public class MerchantTools implements MerchantToolPort {
    private final ToolExecutor executor;

    public MerchantTools(ToolExecutor executor) {
        this.executor = executor;
    }

    @Tool(name = McpToolNames.MERCHANT_GUIDE, description = "Explain MF merchant onboarding conditions and audit flow.")
    public ToolResult<String> guide(ToolExecutionContext context) {
        return executor.execute(context, McpToolNames.MERCHANT_GUIDE, "merchant onboarding guide",
                () -> ToolResult.ok("""
                        商家入驻需要提交主体信息、联系方式、经营类目和资质材料。
                        平台审核通过后才可以发布商品；V1 Agent 只提供流程说明，不执行自动审核。
                        """.strip(), "merchant guide returned"));
    }
}
