package ru.yandex.practicum.telemetry.analyzer.config;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

@Configuration
public class GrpcConfig {

    @Bean
    public HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient(
            @GrpcClient("hub-router") HubRouterControllerGrpc.HubRouterControllerBlockingStub stub) {
        return stub;
    }
}