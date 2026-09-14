package ru.yandex.practicum.telemetry.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;

import ru.yandex.practicum.telemetry.dto.hub.ActionType;
import ru.yandex.practicum.telemetry.dto.hub.ConditionOperation;
import ru.yandex.practicum.telemetry.dto.hub.ConditionType;
import ru.yandex.practicum.telemetry.dto.hub.DeviceType;

@Component
public final class EnumsMapper {

    public EnumsMapper() {}

    public static DeviceTypeAvro toDeviceTypeAvro(DeviceType dto) {
        return dto == null ? null : DeviceTypeAvro.valueOf(dto.name());
    }

    public static ConditionTypeAvro toConditionTypeAvro(ConditionType dto) {
        return dto == null ? null : ConditionTypeAvro.valueOf(dto.name());
    }

    public static ConditionOperationAvro toConditionOperationAvro(ConditionOperation dto) {
        return dto == null ? null : ConditionOperationAvro.valueOf(dto.name());
    }

    public static ActionTypeAvro toActionTypeAvro(ActionType dto) {
        return dto == null ? null : ActionTypeAvro.valueOf(dto.name());
    }
}