package ru.yandex.practicum.telemetry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventDataProducerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventDataProducerApplication.class, args);
    }
}