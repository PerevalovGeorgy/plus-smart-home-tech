package ru.yandex.practicum.telemetry.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DeviceAction {
    private String sensorId;
    private ActionType type;
    private int value;
}
