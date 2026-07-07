package com.mf.fertilizer.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    private static final long BIG_LONG_THRESHOLD = 1_000_000_000_000L;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longCustomizer() {
        return builder -> {
            builder.postConfigurer(mapper -> {
                SimpleModule module = new SimpleModule();

                // Serialize: big Long (>10^12, i.e. snowflake IDs) → String; small Long → number
                module.addSerializer(Long.class, new JsonSerializer<Long>() {
                    @Override
                    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                        if (value != null && Math.abs(value) >= BIG_LONG_THRESHOLD) {
                            gen.writeString(value.toString());
                        } else {
                            gen.writeNumber(value);
                        }
                    }
                });

                // Deserialize: accept String OR number → Long
                module.addDeserializer(Long.class, new JsonDeserializer<Long>() {
                    @Override
                    public Long deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                        if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
                            String s = p.getText().trim();
                            return s.isEmpty() ? null : Long.valueOf(s);
                        }
                        return p.getLongValue();
                    }
                });

                mapper.registerModule(module);
            });
        };
    }
}
