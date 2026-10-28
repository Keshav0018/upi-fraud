package com.payguard.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void definesTheExpectedRoutes() {
        List<String> ids = routeDefinitionLocator.getRouteDefinitions()
                .map(route -> route.getId())
                .collectList()
                .block(Duration.ofSeconds(5));

        assertThat(ids).containsExactlyInAnyOrder("payments", "alerts", "stats", "stream", "analytics");
    }

    @Test
    void routesStatsPathToPaymentServiceEvenThoughItIsNotRunning() {
        // payment-service isn't up in this test; a route match should still attempt the proxy
        // and fail with a gateway/connection error, never Spring's default "no route" 404.
        webTestClient.get().uri("/api/stats")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void unknownPathHasNoMatchingRoute() {
        webTestClient.get().uri("/totally/unmapped")
                .exchange()
                .expectStatus().isNotFound();
    }
}
