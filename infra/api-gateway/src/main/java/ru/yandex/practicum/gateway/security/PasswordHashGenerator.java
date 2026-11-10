package ru.yandex.practicum.gateway.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        var encoder = new BCryptPasswordEncoder();
        System.out.println("ivan: " + encoder.encode("ivan"));
        System.out.println("anna: " + encoder.encode("anna"));
    }
}