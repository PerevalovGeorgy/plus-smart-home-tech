package ru.yandex.practicum.gateway.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
class GatewaySecurityConfigTest {

    private static final String IVAN_AUTH = basic("ivan", "ivan");
    private static final String ANNA_AUTH = basic("anna", "anna");
    private static final String ORIGIN = "http://localhost:5173";

    @Autowired
    private WebTestClient client;

    @ParameterizedTest(name = "GET {0} is public")
    @MethodSource("publicGetUris")
    void publicGet_isPublic(String uri) {
        client.get().uri(uri)
                .exchange()
                .expectStatus().isOk();
    }

    static Stream<String> publicGetUris() {
        return Stream.of(
                "/api/products",
                "/api/products/1",
                "/api/categories",
                "/api/categories/1",
                "/api/inventory",
                "/api/inventory/1"
        );
    }

    @Test
    void corsPreflight_isPublicAndReturnsCorsHeaders() {
        client.options().uri("/api/orders")
                .header(HttpHeaders.ORIGIN, ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN)
                .expectHeader().valueMatches(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ".*POST.*")
                .expectHeader().valueMatches(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "(?i).*authorization.*")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    void orderCreate_withoutCredentials_isUnauthorized() {
        client.post().uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void orderCreate_withWrongPassword_isUnauthorized() {
        client.post().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void orderCreate_withUserCredentials_passesSecurity() {
        client.post().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, IVAN_AUTH)
                .exchange()
                .expectStatus().isOk();
    }

    @ParameterizedTest(name = "GET {0} with USER is forbidden")
    @MethodSource("userForbiddenGetUris")
    void orderRead_withUser_isForbidden(String uri) {
        client.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, IVAN_AUTH)
                .exchange()
                .expectStatus().isForbidden();
    }

    static Stream<String> userForbiddenGetUris() {
        return Stream.of("/api/orders");
    }

    @ParameterizedTest(name = "GET {0} with ADMIN passes")
    @MethodSource("adminAllowedOrderUris")
    void orderRead_withAdmin_passesSecurity(String uri) {
        client.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, ANNA_AUTH)
                .exchange()
                .expectStatus().isOk();
    }

    static Stream<String> adminAllowedOrderUris() {
        return Stream.of(
                "/api/orders",
                "/api/orders/1",
                "/api/orders/by-email?email=test@example.com"
        );
    }

    @ParameterizedTest(name = "{0} {1} with USER is forbidden")
    @MethodSource("writeOperations")
    void productWrite_withUser_isForbidden(HttpMethod method, String uri) {
        client.method(method).uri(uri)
                .header(HttpHeaders.AUTHORIZATION, IVAN_AUTH)
                .exchange()
                .expectStatus().isForbidden();
    }

    @ParameterizedTest(name = "{0} {1} with ADMIN passes")
    @MethodSource("writeOperations")
    void productWrite_withAdmin_passesSecurity(HttpMethod method, String uri) {
        client.method(method).uri(uri)
                .header(HttpHeaders.AUTHORIZATION, ANNA_AUTH)
                .exchange()
                .expectStatus().isOk();
    }

    static Stream<Arguments> writeOperations() {
        return Stream.of(
                Arguments.of(HttpMethod.POST,   "/api/products"),
                Arguments.of(HttpMethod.PUT,    "/api/products/1"),
                Arguments.of(HttpMethod.PATCH,  "/api/products/1"),
                Arguments.of(HttpMethod.DELETE, "/api/products/1"),

                Arguments.of(HttpMethod.POST,   "/api/categories"),
                Arguments.of(HttpMethod.PUT,    "/api/categories/1"),
                Arguments.of(HttpMethod.PATCH,  "/api/categories/1"),
                Arguments.of(HttpMethod.DELETE, "/api/categories/1"),

                Arguments.of(HttpMethod.POST,   "/api/inventory"),
                Arguments.of(HttpMethod.PUT,    "/api/inventory/1"),
                Arguments.of(HttpMethod.PATCH,  "/api/inventory/1"),
                Arguments.of(HttpMethod.DELETE, "/api/inventory/1")
        );
    }

    @ParameterizedTest(name = "GET {0} with ADMIN is forbidden")
    @MethodSource("unknownUris")
    void unknownRoute_withAdmin_isForbidden(String uri) {
        client.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, ANNA_AUTH)
                .exchange()
                .expectStatus().isForbidden();
    }

    static Stream<String> unknownUris() {
        return Stream.of(
                "/api/unknown-route",
                "/api/internal/secret",
                "/actuator/env"
        );
    }

    private static String basic(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class TestBackendConfig {

        @Bean
        RouterFunction<ServerResponse> testBackendRoutes() {
            return route()
                    .GET("/api/products",       req -> ServerResponse.ok().build())
                    .GET("/api/products/**",    req -> ServerResponse.ok().build())
                    .GET("/api/categories",     req -> ServerResponse.ok().build())
                    .GET("/api/categories/**",  req -> ServerResponse.ok().build())
                    .GET("/api/inventory",      req -> ServerResponse.ok().build())
                    .GET("/api/inventory/**",   req -> ServerResponse.ok().build())

                    .GET("/api/orders",         req -> ServerResponse.ok().build())
                    .GET("/api/orders/**",      req -> ServerResponse.ok().build())
                    .POST("/api/orders",        req -> ServerResponse.ok().build())
                    .POST("/api/orders/**",     req -> ServerResponse.ok().build())

                    .POST("/api/products",      req -> ServerResponse.ok().build())
                    .POST("/api/products/**",   req -> ServerResponse.ok().build())
                    .PUT("/api/products/**",    req -> ServerResponse.ok().build())
                    .PATCH("/api/products/**",  req -> ServerResponse.ok().build())
                    .DELETE("/api/products/**", req -> ServerResponse.ok().build())

                    .POST("/api/categories",      req -> ServerResponse.ok().build())
                    .POST("/api/categories/**",   req -> ServerResponse.ok().build())
                    .PUT("/api/categories/**",    req -> ServerResponse.ok().build())
                    .PATCH("/api/categories/**",  req -> ServerResponse.ok().build())
                    .DELETE("/api/categories/**", req -> ServerResponse.ok().build())

                    .POST("/api/inventory",      req -> ServerResponse.ok().build())
                    .POST("/api/inventory/**",   req -> ServerResponse.ok().build())
                    .PUT("/api/inventory/**",    req -> ServerResponse.ok().build())
                    .PATCH("/api/inventory/**",  req -> ServerResponse.ok().build())
                    .DELETE("/api/inventory/**", req -> ServerResponse.ok().build())
                    .build();
        }
    }
}