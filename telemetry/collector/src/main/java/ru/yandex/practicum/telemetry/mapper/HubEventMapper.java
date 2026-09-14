package ru.yandex.practicum.telemetry.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.dto.hub.*;

import java.time.Instant;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
@RequiredArgsConstructor
public class HubEventMapper {

    public HubEventAvro toAvro(HubEvent dto) {
        Instant ts = dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now();

        Object payload = switch (dto.getPayload()) {
            case DEVICE_ADDED      -> toDeviceAdded((DeviceAddedEvent) dto);
            case DEVICE_REMOVED    -> toDeviceRemoved((DeviceRemovedEvent) dto);
            case SCENARIO_ADDED    -> toScenarioAdded((ScenarioAddedEvent) dto);
            case SCENARIO_REMOVED  -> toScenarioRemoved((ScenarioRemovedEvent) dto);
        };

        return HubEventAvro.newBuilder()
                .setHubId(dto.getHubId())
                .setTimestamp(ts)
                .setPayload(payload)
                .build();
    }

    private DeviceAddedEventAvro toDeviceAdded(DeviceAddedEvent dto) {
        return DeviceAddedEventAvro.newBuilder()
                .setId(dto.getId())
                .setType(EnumsMapper.toDeviceTypeAvro(dto.getDeviceType()))
                .build();
    }

    private DeviceRemovedEventAvro toDeviceRemoved(DeviceRemovedEvent dto) {
        return DeviceRemovedEventAvro.newBuilder()
                .setId(dto.getId())
                .build();
    }

    private ScenarioAddedEventAvro toScenarioAdded(ScenarioAddedEvent dto) {
        return ScenarioAddedEventAvro.newBuilder()
                .setName(dto.getName())
                .setConditions(dto.getConditions().stream()
                        .map(this::toCondition)
                        .toList())
                .setActions(dto.getActions().stream()
                        .map(this::toAction)
                        .toList())
                .build();
    }

    private ScenarioRemovedEventAvro toScenarioRemoved(ScenarioRemovedEvent dto) {
        return ScenarioRemovedEventAvro.newBuilder()
                .setName(dto.getName())
                .build();
    }

    private ScenarioConditionAvro toCondition(ScenarioCondition dto) {
        return ScenarioConditionAvro.newBuilder()
                .setSensorId(dto.getSensorId())
                .setType(EnumsMapper.toConditionTypeAvro(dto.getType()))
                .setOperation(EnumsMapper.toConditionOperationAvro(dto.getOperation()))
                .setValue(dto.getValue())
                .build();
    }

    private DeviceActionAvro toAction(DeviceAction dto) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(dto.getSensorId())
                .setType(EnumsMapper.toActionTypeAvro(dto.getType()))
                .setValue(dto.getValue())
                .build();
    }
}