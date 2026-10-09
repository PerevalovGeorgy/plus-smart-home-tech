package ru.yandex.practicum.telemetry.handler.hub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceRemovedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.telemetry.handler.HubEventHandler;

@Component
public class DeviceRemovedEventHandler implements HubEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceRemovedEventHandler.class);

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_REMOVED;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceRemovedEventProto removed = event.getDeviceRemoved();
        log.info("Хаб {}: удалено устройство id={}", event.getHubId(), removed.getId());
    }
}
