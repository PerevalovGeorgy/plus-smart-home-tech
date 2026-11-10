package ru.yandex.practicum.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .cors(cors -> {})
                .httpBasic(httpBasic -> {})
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/inventory/**").permitAll()

                        .pathMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")

                        .pathMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole("USER", "ADMIN")

                        .pathMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        .pathMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/categories/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")

                        .pathMatchers(HttpMethod.POST, "/api/inventory/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/inventory/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/inventory/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/inventory/**").hasRole("ADMIN")

                        .anyExchange().denyAll()
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(SecurityUsersProperties properties) {
        List<UserDetails> users = properties.getUsers().stream()
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRoles().toArray(String[]::new))
                        .build())
                .toList();
        return new MapReactiveUserDetailsService(users);
    }
}