package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);

    private final ScenarioRepository scenarioRepository;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public SnapshotService(ScenarioRepository scenarioRepository,
                           HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient) {
        this.scenarioRepository = scenarioRepository;
        this.hubRouterClient = hubRouterClient;
    }

    @Transactional
    public void processSnapshot(SensorsSnapshotAvro snapshot) {
        log.info("Обрабатываю снапшот для хаба {}", snapshot.getHubId());
        List<Scenario> scenarios = scenarioRepository.findByHubId(snapshot.getHubId());
        log.info("Найдено {} сценариев для хаба {}", scenarios.size(), snapshot.getHubId());

        for (Scenario scenario : scenarios) {
            boolean allMatch = scenario.getConditions().stream()
                    .allMatch(c -> checkCondition(c, snapshot.getSensorsState()));
            log.info("Сценарий {} совпал: {}", scenario.getName(), allMatch);
            if (allMatch) {
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

        Integer expected = link.getCondition().getValue();
        if (expected == null) return false;

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

                throw new RuntimeException("Не удалось отправить действие на hub-router", e);
            }
        }
    }

}
