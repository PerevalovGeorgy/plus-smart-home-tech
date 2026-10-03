package ru.yandex.practicum.telemetry.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.telemetry.handler.HubEventHandler;
import ru.yandex.practicum.telemetry.handler.SensorEventHandler;
import ru.yandex.practicum.telemetry.producer.KafkaEventProducer;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@GrpcService
public class EventController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);
    private final KafkaEventProducer kafkaEventProducer;

    private final Map<SensorEventProto.PayloadCase, SensorEventHandler> sensorEventHandlers;
    private final Map<HubEventProto.PayloadCase, HubEventHandler> hubEventHandlers;

    public EventController(Set<SensorEventHandler> sensorEventHandlers,
                           Set<HubEventHandler> hubEventHandlers,
                           KafkaEventProducer kafkaEventProducer) {
        this.sensorEventHandlers = sensorEventHandlers.stream()
                .collect(Collectors.toMap(
                        SensorEventHandler::getMessageType,
                        Function.identity()
                ));
        this.hubEventHandlers = hubEventHandlers.stream()
                .collect(Collectors.toMap(
                        HubEventHandler::getMessageType,
                        Function.identity()
                ));
        this.kafkaEventProducer = kafkaEventProducer;
    }

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Получено событие датчика: id={}, hubId={}, payload={}",
                    request.getId(), request.getHubId(), request.getPayloadCase());

            SensorEventHandler handler = sensorEventHandlers.get(request.getPayloadCase());
            if (handler == null) {
                throw new IllegalArgumentException(
                        "Не найден обработчик для события датчика: " + request.getPayloadCase());
            }
            handler.handle(request);
            kafkaEventProducer.send(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при обработке события датчика", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Получено событие хаба: hubId={}, payload={}",
                    request.getHubId(), request.getPayloadCase());

            HubEventHandler handler = hubEventHandlers.get(request.getPayloadCase());
            if (handler == null) {
                throw new IllegalArgumentException(
                        "Не найден обработчик для события хаба: " + request.getPayloadCase());
            }
            handler.handle(request);
            kafkaEventProducer.send(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при обработке события хаба", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}