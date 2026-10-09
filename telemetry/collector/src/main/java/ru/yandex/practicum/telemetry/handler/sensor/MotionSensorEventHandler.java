package ru.yandex.practicum.telemetry.handler.sensor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.MotionSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.handler.SensorEventHandler;

@Component
public class MotionSensorEventHandler implements SensorEventHandler {

    private static final Logger log = LoggerFactory.getLogger(MotionSensorEventHandler.class);

    @Override
    public SensorEventProto.PayloadCase getMessageType() {
        return SensorEventProto.PayloadCase.MOTION;
    }

    @Override
    public void handle(SensorEventProto event) {
        MotionSensorProto motion = event.getMotion();
        log.info("Датчик движения {}: motion={}, linkQuality={}, voltage={}",
                event.getId(), motion.getMotion(), motion.getLinkQuality(), motion.getVoltage());
    }
}
