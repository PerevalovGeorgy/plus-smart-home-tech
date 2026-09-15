package ru.yandex.practicum.telemetry.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.telemetry.dto.hub.HubEvent;
import ru.yandex.practicum.telemetry.dto.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.service.EventService;

@RestController
@RequestMapping(value = "/events", consumes = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/sensors")
    @ResponseStatus(HttpStatus.CREATED)
    public void collectSensorEvent(@Valid @RequestBody SensorEvent dto) {
        eventService.collectSensorEvent(dto);
    }

    @PostMapping("/hubs")
    @ResponseStatus(HttpStatus.CREATED)
    public void collectHubEvent(@Valid @RequestBody HubEvent dto) {
        eventService.collectHubEvent(dto);
    }
}
