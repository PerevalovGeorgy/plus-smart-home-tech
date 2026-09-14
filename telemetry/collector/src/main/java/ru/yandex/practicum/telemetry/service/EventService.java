package ru.yandex.practicum.telemetry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.dto.hub.HubEvent;
import ru.yandex.practicum.telemetry.dto.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.mapper.HubEventMapper;
import ru.yandex.practicum.telemetry.mapper.SensorEventMapper;
import ru.yandex.practicum.telemetry.producer.KafkaEventProducer;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;
    private final KafkaEventProducer producer;

    public void collectSensorEvent(SensorEvent dto) {
        log.debug("Получено событие датчика: id={}, hubId={}, type={}",
                dto.getId(), dto.getHubId(), dto.getType());

        SensorEventAvro avro = sensorEventMapper.toAvro(dto);
        producer.sendSensorEvent(avro);
    }

    public void collectHubEvent(HubEvent dto) {
        log.debug("Получено событие хаба: hubId={}, type={}",
                dto.getHubId(), dto.getPayload());

        HubEventAvro avro = hubEventMapper.toAvro(dto);
        producer.sendHubEvent(avro);
    }
}