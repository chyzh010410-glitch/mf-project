package com.mf.agentservice.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntentResolverTest {
    private final IntentResolver resolver = new IntentResolver();

    @Test
    void resolvesUnsafeActionBeforeOrderIntent() {
        assertThat(resolver.resolve("帮我把订单 123 退款")).isEqualTo(AgentIntent.UNSAFE_ACTION);
    }

    @Test
    void extractsFirstNumberAsOrderId() {
        assertThat(resolver.extractFirstNumber("我的订单 12345 发货了吗")).contains(12345L);
    }

    @Test
    void resolvesProductAndKnowledgeIntents() {
        assertThat(resolver.resolve("推荐几款适合果树的肥料")).isEqualTo(AgentIntent.PRODUCT);
        assertThat(resolver.resolve("苹果树腐烂病怎么处理")).isEqualTo(AgentIntent.ENCYCLOPEDIA);
        assertThat(resolver.resolve("果树施肥要注意什么")).isEqualTo(AgentIntent.ENCYCLOPEDIA);
        assertThat(resolver.resolve("果树施肥推荐哪款肥料")).isEqualTo(AgentIntent.PRODUCT);
    }

    @Test
    void resolvesMerchantOnboardingIntent() {
        assertThat(resolver.resolve("我想申请商家入驻，需要哪些资质")).isEqualTo(AgentIntent.MERCHANT);
    }

    @Test
    void resolvesGreetingAndHelpBeforeUnknownFallback() {
        assertThat(resolver.resolve("你好")).isEqualTo(AgentIntent.GREETING);
        assertThat(resolver.resolve("你会什么")).isEqualTo(AgentIntent.HELP);
        assertThat(resolver.resolve("你会做什么")).isEqualTo(AgentIntent.HELP);
        assertThat(resolver.resolve("你是哪个公司的客服")).isEqualTo(AgentIntent.COMPANY);
    }
}
