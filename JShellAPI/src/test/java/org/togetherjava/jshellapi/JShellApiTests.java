package org.togetherjava.jshellapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.togetherjava.jshellapi.dto.JShellResult;
import org.togetherjava.jshellapi.rest.ApiEndpoints;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class holds integration tests for JShellAPI. It depends on gradle building image task, fore
 * more information check "test" section in gradle.build file.
 *
 * @author Firas Regaieg
 */
@ContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JShellApiTests {

    @Autowired
    private WebTestClient webTestClient;

    private static final String TEST_EVALUATION_ID = "test";
    private static final String TEST_CODE_INPUT = "2+2";
    private static final String TEST_CODE_EXPECTED_OUTPUT = "4";

    @Test
    @DisplayName("When posting code snippet, evaluate it then returns successfully result")
    public void evaluateCodeSnippetTest() {

        JShellResult result = this.webTestClient.mutate()
            .responseTimeout(Duration.ofSeconds(6))
            .build()
            .post()
            .uri(ApiEndpoints.EVALUATE_CODE_SNIPPET + "/" + TEST_EVALUATION_ID)
            .bodyValue(TEST_CODE_INPUT)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(JShellResult.class)
            .value(task -> assertThat(task).isNotNull())
            .returnResult()
            .getResponseBody();

        assertThat(result).isNotNull();

        boolean isValidResult = result.snippetsResults()
            .stream()
            .filter(res -> res.result() != null)
            .anyMatch(res -> res.result().equals(TEST_CODE_EXPECTED_OUTPUT));

        assertThat(isValidResult).isTrue();

    }
}
