package com.mf.agentservice.agent;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
public class CustomerServiceContractService {
    private static final String CONTRACT_RESOURCE = "classpath:customer-service-contract.yml";

    private final List<ContractFlow> flows;

    public CustomerServiceContractService(ResourceLoader resourceLoader) {
        this.flows = load(resourceLoader);
    }

    public Optional<ContractFlow> match(String message) {
        String normalizedMessage = normalize(message);
        return flows.stream()
                .filter(flow -> flow.examples().stream().anyMatch(example -> normalizedMessage.contains(normalize(example))))
                .max(Comparator.comparingInt(flow -> flow.examples().stream()
                        .filter(example -> normalizedMessage.contains(normalize(example)))
                        .mapToInt(String::length)
                        .max()
                        .orElse(0)));
    }

    private List<ContractFlow> load(ResourceLoader resourceLoader) {
        try (InputStream input = resourceLoader.getResource(CONTRACT_RESOURCE).getInputStream()) {
            Map<String, Object> document = new Yaml().load(input);
            Object rawFlows = document.get("flows");
            if (!(rawFlows instanceof List<?> items)) {
                throw new IllegalStateException("Customer service contract has no flows");
            }
            return items.stream().map(this::toFlow).toList();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load customer service contract", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private ContractFlow toFlow(Object item) {
        Map<String, Object> values = (Map<String, Object>) item;
        Map<String, Object> response = (Map<String, Object>) values.get("response");
        List<String> examples = ((List<Object>) values.get("examples")).stream().map(String::valueOf).toList();
        return new ContractFlow(
                String.valueOf(values.get("id")),
                String.valueOf(values.get("status")),
                String.valueOf(values.get("intent")),
                examples,
                new ContractResponse(
                        String.valueOf(response.get("success")),
                        String.valueOf(response.get("missing_slot")),
                        String.valueOf(response.get("empty"))));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[?？!！,，。\\s]", "");
    }

    public record ContractFlow(String id, String status, String intent, List<String> examples, ContractResponse response) {
        public boolean hasStatus(String expected) {
            return expected.equals(status);
        }
    }

    public record ContractResponse(String success, String missingSlot, String empty) {
    }
}
