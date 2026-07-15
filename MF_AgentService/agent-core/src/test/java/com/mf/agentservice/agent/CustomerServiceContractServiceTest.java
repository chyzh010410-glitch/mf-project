package com.mf.agentservice.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class CustomerServiceContractServiceTest {
    private final CustomerServiceContractService service = new CustomerServiceContractService(new DefaultResourceLoader());

    @Test
    void matchesTheMostSpecificKnownExample() {
        var flow = service.match("我的订单发货了吗");

        assertThat(flow).isPresent();
        assertThat(flow.orElseThrow().id()).isEqualTo("24");
        assertThat(flow.orElseThrow().status()).isEqualTo("pending_data");
    }
}
