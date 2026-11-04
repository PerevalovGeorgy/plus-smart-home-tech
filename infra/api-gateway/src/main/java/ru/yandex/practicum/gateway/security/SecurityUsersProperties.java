package ru.yandex.practicum.gateway.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security")
@Data
public class SecurityUsersProperties {

    private List<UserEntry> users = new ArrayList<>();

    @Data
    public static class UserEntry {
        private String username;
        private String password;
        private List<String> roles = new ArrayList<>();
    }
}