package ru.yandex.practicum.telemetry.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

@Component
public class KafkaEventProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final String sensorsTopic;
    private final String hubsTopic;

    public KafkaEventProducer(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            @Value("${kafka.topics.sensors}") String sensorsTopic,
            @Value("${kafka.topics.hubs}") String hubsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.sensorsTopic = sensorsTopic;
        this.hubsTopic = hubsTopic;
    }

    public void send(SensorEventProto event) {
        kafkaTemplate.send(sensorsTopic, event.getId(), event.toByteArray());
    }

    public void send(HubEventProto event) {
        kafkaTemplate.send(hubsTopic, event.getHubId(), event.toByteArray());
    }
}