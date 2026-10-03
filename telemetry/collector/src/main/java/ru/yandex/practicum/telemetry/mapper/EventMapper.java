package ru.yandex.practicum.telemetry.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Component
public class EventMapper {

    private static Instant toInstant(com.google.protobuf.Timestamp ts) {
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }

    // ==================== Sensor ====================

    public SensorEventAvro toAvro(SensorEventProto proto) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(proto.getId())
                .setHubId(proto.getHubId())
                .setTimestamp(toInstant(proto.getTimestamp()));

        Object payload = switch (proto.getPayloadCase()) {
            case MOTION -> MotionSensorAvro.newBuilder()
                    .setLinkQuality(proto.getMotion().getLinkQuality())
                    .setMotion(proto.getMotion().getMotion())
                    .setVoltage(proto.getMotion().getVoltage())
                    .build();
            case TEMPERATURE -> TemperatureSensorAvro.newBuilder()
                    .setTemperatureC(proto.getTemperature().getTemperatureC())
                    .setTemperatureF(proto.getTemperature().getTemperatureF())
                    .build();
            case LIGHT -> LightSensorAvro.newBuilder()
                    .setLinkQuality(proto.getLight().getLinkQuality())
                    .setLuminosity(proto.getLight().getLuminosity())
                    .build();
            case CLIMATE -> ClimateSensorAvro.newBuilder()
                    .setTemperatureC(proto.getClimate().getTemperatureC())
                    .setHumidity(proto.getClimate().getHumidity())
                    .setCo2Level(proto.getClimate().getCo2Level())
                    .build();
            case SWITCH -> SwitchSensorAvro.newBuilder()
                    .setState(proto.getSwitch().getState())
                    .build();
            default -> throw new IllegalArgumentException(
                    "Unknown sensor payload: " + proto.getPayloadCase());
        };

        return builder.setPayload(payload).build();
    }

    // ==================== Hub ====================

    public HubEventAvro toAvro(HubEventProto proto) {
        HubEventAvro.Builder builder = HubEventAvro.newBuilder()
                .setHubId(proto.getHubId())
                .setTimestamp(toInstant(proto.getTimestamp()));

        Object payload = switch (proto.getPayloadCase()) {
            case DEVICE_ADDED -> DeviceAddedEventAvro.newBuilder()
                    .setId(proto.getDeviceAdded().getId())
                    .setType(DeviceTypeAvro.valueOf(proto.getDeviceAdded().getType().name()))
                    .build();
            case DEVICE_REMOVED -> DeviceRemovedEventAvro.newBuilder()
                    .setId(proto.getDeviceRemoved().getId())
                    .build();
            case SCENARIO_ADDED -> {
                ScenarioAddedEventProto s = proto.getScenarioAdded();
                var conditions = s.getConditionList().stream()
                        .map(c -> ScenarioConditionAvro.newBuilder()
                                .setSensorId(c.getSensorId())
                                .setType(ConditionTypeAvro.valueOf(c.getType().name()))
                                .setOperation(ConditionOperationAvro.valueOf(c.getOperation().name()))
                                .setValue(switch (c.getValueCase()) {
                                    case INT_VALUE -> c.getIntValue();
                                    case BOOL_VALUE -> c.getBoolValue();
                                    default -> null;
                                })
                                .build())
                        .toList();
                var actions = s.getActionList().stream()
                        .map(a -> DeviceActionAvro.newBuilder()
                                .setSensorId(a.getSensorId())
                                .setType(ActionTypeAvro.valueOf(a.getType().name()))
                                .setValue(a.hasValue() ? a.getValue() : null)
                                .build())
                        .toList();
                yield ScenarioAddedEventAvro.newBuilder()
                        .setName(s.getId())
                        .setConditions(conditions)
                        .setActions(actions)
                        .build();
            }
            case SCENARIO_REMOVED -> ScenarioRemovedEventAvro.newBuilder()
                    .setName(proto.getScenarioRemoved().getId())
                    .build();
            default -> throw new IllegalArgumentException(
                    "Unknown hub payload: " + proto.getPayloadCase());
        };

        return builder.setPayload(payload).build();
    }
}