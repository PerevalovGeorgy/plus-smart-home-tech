package ru.yandex.practicum.telemetry.analyzer.processor;

import com.google.protobuf.Timestamp;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.entity.Scenario;
import ru.yandex.practicum.telemetry.analyzer.entity.ScenarioAction;
import ru.yandex.practicum.telemetry.analyzer.entity.ScenarioCondition;
import ru.yandex.practicum.telemetry.analyzer.entity.enums.ConditionOperation;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class SnapshotProcessor {

    private static final Logger log = LoggerFactory.getLogger(SnapshotProcessor.class);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(1000);

    private final Consumer<String, SensorsSnapshotAvro> consumer;
    private final ScenarioRepository scenarioRepository;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;
    private final String topic;

    public SnapshotProcessor(Consumer<String, SensorsSnapshotAvro> consumer,
                             ScenarioRepository scenarioRepository,
                             HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient,
                             @Value("${kafka.topics.snapshots}") String topic) {
        this.consumer = consumer;
        this.scenarioRepository = scenarioRepository;
        this.hubRouterClient = hubRouterClient;
        this.topic = topic;
    }

    public void start() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(List.of(topic));

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(POLL_TIMEOUT);
                records.forEach(record -> processSnapshot(record.value()));
                consumer.commitSync();
            }
        } catch (WakeupException ignored) {
            // штатное завершение
        } catch (Exception e) {
            log.error("Ошибка в SnapshotProcessor", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }

    @Transactional
    protected void processSnapshot(SensorsSnapshotAvro snapshot) {
        List<Scenario> scenarios = scenarioRepository.findByHubId(snapshot.getHubId());
        if (scenarios.isEmpty()) {
            return;
        }

        Map<String, SensorStateAvro> state = snapshot.getSensorsState();

        for (Scenario scenario : scenarios) {
            boolean allMatch = scenario.getConditions().stream()
                    .allMatch(c -> checkCondition(c, state));

            if (allMatch) {
                log.info("Сценарий {} выполнен для хаба {}",
                        scenario.getName(), snapshot.getHubId());
                executeActions(scenario, snapshot);
            }
        }
    }

    private boolean checkCondition(ScenarioCondition link, Map<String, SensorStateAvro> state) {
        String sensorId = link.getSensor().getId();
        SensorStateAvro sensorState = state.get(sensorId);
        if (sensorState == null) {
            return false;
        }

        Integer actualValue = extractValue(sensorState.getData(), link.getCondition().getType().name());
        if (actualValue == null) {
            return false;
        }

        int expected = link.getCondition().getValue();
        ConditionOperation operation = link.getCondition().getOperation();

        return switch (operation) {
            case EQUALS -> actualValue == expected;
            case GREATER_THAN -> actualValue > expected;
            case LOWER_THAN -> actualValue < expected;
        };
    }

    private Integer extractValue(Object data, String type) {
        return switch (data) {
            case MotionSensorAvro m -> "MOTION".equals(type) ? (m.getMotion() ? 1 : 0) : null;
            case TemperatureSensorAvro t -> "TEMPERATURE".equals(type) ? t.getTemperatureC() : null;
            case LightSensorAvro l -> "LUMINOSITY".equals(type) ? l.getLuminosity() : null;
            case ClimateSensorAvro c -> switch (type) {
                case "TEMPERATURE" -> c.getTemperatureC();
                case "HUMIDITY" -> c.getHumidity();
                case "CO2LEVEL" -> c.getCo2Level();
                default -> null;
            };
            case SwitchSensorAvro s -> "SWITCH".equals(type) ? (s.getState() ? 1 : 0) : null;
            default -> null;
        };
    }

    private void executeActions(Scenario scenario, SensorsSnapshotAvro snapshot) {
        Instant now = Instant.now();

        for (ScenarioAction link : scenario.getActions()) {
            DeviceActionProto actionProto = DeviceActionProto.newBuilder()
                    .setSensorId(link.getSensor().getId())
                    .setType(ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto
                            .valueOf(link.getAction().getType().name()))
                    .setValue(link.getAction().getValue() != null
                            ? link.getAction().getValue() : 0)
                    .build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(snapshot.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(actionProto)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(now.getEpochSecond())
                            .setNanos(now.getNano())
                            .build())
                    .build();

            try {
                hubRouterClient.handleDeviceAction(request);
                log.info("Отправлено действие для устройства {}",
                        link.getSensor().getId());
            } catch (Exception e) {
                log.error("Ошибка отправки действия на hub-router", e);
            }
        }
    }
}