package org.togetherjava.jshellapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.togetherjava.jshellapi.dto.JShellResult;
import org.togetherjava.jshellapi.dto.JShellSnippetResult;
import org.togetherjava.jshellapi.dto.SnippetStatus;
import org.togetherjava.jshellapi.dto.SnippetType;
import org.togetherjava.jshellapi.rest.ApiEndpoints;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integrates tests for JShellAPI.
 */
@ActiveProfiles("testing")
@ContextConfiguration(classes = Main.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JShellApiTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private Config testsConfig;

    @Test
    @DisplayName("When posting code snippet, evaluate it then return successfully result")
    public void evaluateCodeSnippetTest() {

        final String testEvalId = "test";

        // -- first code snippet eval
        executeCodeEvalTest(testEvalId, "int a = 2+2;", 1, "4");

        // -- second code snippet eval
        executeCodeEvalTest(testEvalId, "a * 2", 2, "8");
    }

    private void executeCodeEvalTest(String evalId, String codeSnippet, int expectedId,
            String expectedResult) {
        final JShellSnippetResult jshellCodeSnippet = new JShellSnippetResult(SnippetStatus.VALID,
                SnippetType.ADDITION, expectedId, codeSnippet, expectedResult);

        assertThat(testEval(evalId, codeSnippet))
            .isEqualTo(new JShellResult(List.of(jshellCodeSnippet), null, false, ""));
    }

    private JShellResult testEval(String testEvalId, String codeInput) {
        final String endpoint =
                String.join("/", ApiEndpoints.BASE, ApiEndpoints.EVALUATE, testEvalId);

        return this.webTestClient.mutate()
            .responseTimeout(Duration.ofSeconds(testsConfig.evalTimeoutSeconds()))
            .build()
            .post()
            .uri(endpoint)
            .bodyValue(codeInput)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(JShellResult.class)
            .value((JShellResult evalResult) -> assertThat(evalResult).isNotNull())
            .returnResult()
            .getResponseBody();
    }
}
