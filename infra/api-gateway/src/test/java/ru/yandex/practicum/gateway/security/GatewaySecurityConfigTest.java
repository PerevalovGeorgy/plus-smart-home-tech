package ru.yandex.practicum.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient client;


    @Test
    void catalogGet_isPublic() {
        client.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void corsPreflight_isPublic() {
        client.options().uri("/api/orders")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
    }


    @Test
    void orderCreate_withoutCredentials_isUnauthorized() {
        client.post().uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void orderCreate_withUserCredentials_passesSecurity() {
        client.post().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void ordersList_withUser_isForbidden() {
        client.get().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void ordersList_withAdmin_passesSecurity() {
        client.get().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna"))
                .exchange()
                .expectStatus().isOk();
    }


    @Test
    void productWrite_withUser_isForbidden() {
        client.post().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void productWrite_withAdmin_passesSecurity() {
        client.post().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void unknownRoute_withAdmin_isForbidden() {
        client.get().uri("/api/unknown-route")
                .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna"))
                .exchange()
                .expectStatus().isForbidden();
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
                    .GET("/api/products", req -> ServerResponse.ok().build())
                    .GET("/api/products/**", req -> ServerResponse.ok().build())
                    .GET("/api/categories/**", req -> ServerResponse.ok().build())
                    .GET("/api/inventory/**", req -> ServerResponse.ok().build())
                    .GET("/api/orders", req -> ServerResponse.ok().build())
                    .GET("/api/orders/**", req -> ServerResponse.ok().build())
                    .POST("/api/orders", req -> ServerResponse.ok().build())
                    .POST("/api/orders/**", req -> ServerResponse.ok().build())
                    .POST("/api/products", req -> ServerResponse.ok().build())
                    .POST("/api/products/**", req -> ServerResponse.ok().build())
                    .PUT("/api/products/**", req -> ServerResponse.ok().build())
                    .PATCH("/api/products/**", req -> ServerResponse.ok().build())
                    .DELETE("/api/products/**", req -> ServerResponse.ok().build())
                    .build();
        }
    }
}