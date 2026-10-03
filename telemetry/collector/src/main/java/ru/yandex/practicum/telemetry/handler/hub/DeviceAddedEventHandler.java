package ru.yandex.practicum.telemetry.handler.hub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.telemetry.handler.HubEventHandler;

@Component
public class DeviceAddedEventHandler implements HubEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceAddedEventHandler.class);

    @Override
    public HubEventProto.PayloadCase getMessageType() {
        return HubEventProto.PayloadCase.DEVICE_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        DeviceAddedEventProto added = event.getDeviceAdded();
        log.info("Хаб {}: добавлено устройство id={}, type={}",
                event.getHubId(), added.getId(), added.getType());
    }
}
