package com.mf.agentservice.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mf.agentservice.client.MfEpClient;
import org.junit.jupiter.api.Test;

class OrderToolsTest {

    @Test
    void refusesOrderLookupWithoutAuthToken() {
        var client = mock(MfEpClient.class);
        var tools = new OrderTools(client, new ToolExecutor());
        var context = new ToolExecutionContext();

        var result = tools.status(context, 123L, "");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("ORDER_AUTH_REQUIRED");
        assertThat(context.records()).hasSize(1);
        assertThat(context.records().get(0).name()).isEqualTo(McpToolNames.ORDER_STATUS);
        verify(client, never()).orderStatus(123L, "");
    }
}
