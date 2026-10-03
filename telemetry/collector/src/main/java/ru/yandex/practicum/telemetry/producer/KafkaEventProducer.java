package ru.yandex.practicum.telemetry.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.mapper.EventMapper;

@Component
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventMapper mapper;
    private final String sensorsTopic;
    private final String hubsTopic;

    public KafkaEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            EventMapper mapper,
            @Value("${kafka.topics.sensors}") String sensorsTopic,
            @Value("${kafka.topics.hubs}") String hubsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
        this.sensorsTopic = sensorsTopic;
        this.hubsTopic = hubsTopic;
    }

    public void send(SensorEventProto event) {
        SensorEventAvro avro = mapper.toAvro(event);
        kafkaTemplate.send(sensorsTopic, event.getId(), avro);
    }

    public void send(HubEventProto event) {
        HubEventAvro avro = mapper.toAvro(event);
        kafkaTemplate.send(hubsTopic, event.getHubId(), avro);
    }
}