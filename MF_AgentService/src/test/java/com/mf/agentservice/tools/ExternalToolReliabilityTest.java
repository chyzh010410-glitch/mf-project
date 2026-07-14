package com.mf.agentservice.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.mf.agentservice.client.MfEpClient;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class ExternalToolReliabilityTest {

    @Test
    void productSearchDistinguishesEmptyAndUnavailableResults() {
        var client = mock(MfEpClient.class);
        var tools = new ProductTools(client, new ToolExecutor());
        var context = new ToolExecutionContext();
        when(client.searchProducts("果树肥", null, 1, 5))
                .thenReturn(JsonNodeFactory.instance.objectNode().putArray("records"));

        var empty = tools.search(context, "果树肥", null);

        assertThat(empty.success()).isTrue();
        assertThat(empty.value().path("records")).isEmpty();

        when(client.searchProducts("果树肥", null, 1, 5))
                .thenThrow(new ResourceAccessException("connection refused"));
        var unavailable = tools.search(context, "果树肥", null);

        assertThat(unavailable.success()).isFalse();
        assertThat(unavailable.failureReason()).isEqualTo(ToolFailureReason.UPSTREAM_UNAVAILABLE);
    }

    @Test
    void encyclopediaSearchDistinguishesSuccessAndTimeout() {
        var client = mock(MfEpClient.class);
        var tools = new EncyclopediaTools(client, new ToolExecutor());
        var context = new ToolExecutionContext();
        when(client.searchEncyclopedia("苹果树", 1, 5))
                .thenReturn(JsonNodeFactory.instance.objectNode().putArray("records"));

        assertThat(tools.search(context, "苹果树").success()).isTrue();

        when(client.searchEncyclopedia("苹果树", 1, 5))
                .thenThrow(new ResourceAccessException("read timed out", new SocketTimeoutException("read timed out")));
        var timeout = tools.search(context, "苹果树");

        assertThat(timeout.success()).isFalse();
        assertThat(timeout.failureReason()).isEqualTo(ToolFailureReason.UPSTREAM_TIMEOUT);
    }

    @Test
    void orderStatusDistinguishesAuthorizedSuccessAndUpstreamFailure() {
        var client = mock(MfEpClient.class);
        var tools = new OrderTools(client, new ToolExecutor());
        var context = new ToolExecutionContext();
        when(client.orderStatus(12L, "Bearer token"))
                .thenReturn(JsonNodeFactory.instance.objectNode().put("status", "shipped"));

        assertThat(tools.status(context, 12L, "Bearer token").success()).isTrue();

        when(client.orderStatus(12L, "Bearer token"))
                .thenThrow(new ResourceAccessException("connection refused"));
        var unavailable = tools.status(context, 12L, "Bearer token");

        assertThat(unavailable.success()).isFalse();
        assertThat(unavailable.failureReason()).isEqualTo(ToolFailureReason.UPSTREAM_UNAVAILABLE);
    }
}
