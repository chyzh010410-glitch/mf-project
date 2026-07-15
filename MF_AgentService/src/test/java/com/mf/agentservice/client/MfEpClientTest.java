package com.mf.agentservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mf.agentservice.config.MfAgentProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MfEpClientTest {
    @Test
    void productSearchOmitsBlankFilters() {
        var builder = RestClient.builder().baseUrl("http://mf-ep.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), request -> {
                    assertThat(request.getURI().getPath()).isEqualTo("/client/products");
                    assertThat(request.getURI().getQuery()).contains("keyword=apple", "page=1", "size=5")
                            .doesNotContain("productType");
                })
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"code\":200,\"msg\":\"success\",\"data\":{\"records\":[{\"id\":35}]}}", MediaType.APPLICATION_JSON));

        var client = new MfEpClient(builder.build(), properties());
        var products = client.searchProducts("apple", null, 1, 5);

        assertThat(products.path("records")).hasSize(1);
        server.verify();
    }

    private MfAgentProperties properties() {
        return new MfAgentProperties(
                new MfAgentProperties.Endpoint("http://mf-ep.test", ""),
                null,
                new MfAgentProperties.Agent(false, Duration.ofSeconds(5), Duration.ofSeconds(10), 20,
                        Duration.ofMinutes(20), 4, Duration.ofMinutes(10), "", Duration.ofSeconds(30), 5, Duration.ofMinutes(5)));
    }
}
