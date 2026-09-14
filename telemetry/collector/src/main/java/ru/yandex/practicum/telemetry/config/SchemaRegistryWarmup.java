package ru.yandex.practicum.telemetry.config;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaRegistryWarmup implements ApplicationRunner {
    private final SchemaRegistryClient schemaRegistryClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                schemaRegistryClient.getAllSubjects();
                log.info("Schema Registry reachable");
                return;
            } catch (Exception e) {
                log.warn("Schema Registry not ready ({}/30): {}", attempt, e.getMessage());
                Thread.sleep(2000);
            }
        }
        throw new IllegalStateException("Schema Registry not reachable in 60s");
    }
}
