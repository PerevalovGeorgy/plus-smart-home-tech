package ru.yandex.practicum.telemetry.dto.hub;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class DeviceRemovedEvent extends HubEvent{

    private String id;

    @Override
    public HubEventType getPayload() {
        return HubEventType.DEVICE_REMOVED;
    }
}
