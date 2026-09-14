package ru.yandex.practicum.telemetry.dto.hub;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "добавление нового устройства")
@NotNull
public class DeviceAddedEvent extends HubEvent{

    @NotBlank
    private String id;

    @NotNull
    private DeviceType deviceType;

    @Override
    public HubEventType getPayload() {
        return HubEventType.DEVICE_ADDED;
    }
}
