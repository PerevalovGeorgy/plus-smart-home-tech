package ru.yandex.practicum.telemetry.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaProducerProperties {
    private String bootstrapServers;
    private String schemaRegistryUrl;
}
