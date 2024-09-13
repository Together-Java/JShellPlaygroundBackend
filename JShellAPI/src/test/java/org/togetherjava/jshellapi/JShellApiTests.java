package org.togetherjava.jshellapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * This class holds integration tests for JShellAPI. It depends on gradle building image task, fore
 * more information check "test" section in gradle.build file.
 *
 * @author Firas Regaieg
 */
@ContextConfiguration(classes = Main.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JShellApiTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("When posting code snippet, evaluate it then returns successfully result")
    public void evaluateCodeSnippetTest() {

        final String testEvalId = "test";

        // -- performing a first code snippet execution

        final String firstCodeExpression = "int a = 2+2;";

        final JShellSnippetResult firstCodeSnippet = new JShellSnippetResult(SnippetStatus.VALID,
                SnippetType.ADDITION, 1, firstCodeExpression, "4");
        final JShellResult firstCodeExpectedResult =
                getJShellResultDefaultInstance(firstCodeSnippet);

        assertThat(testEval(testEvalId, firstCodeExpression)).isEqualTo(firstCodeExpectedResult);

        // -- performing a second code snippet execution

        final String secondCodeExpression = "a * 2";

        final JShellSnippetResult secondCodeSnippet = new JShellSnippetResult(SnippetStatus.VALID,
                SnippetType.ADDITION, 2, secondCodeExpression, "8");

        final JShellResult secondCodeExpectedResult =
                getJShellResultDefaultInstance(secondCodeSnippet);

        assertThat(testEval(testEvalId, secondCodeExpression)).isEqualTo(secondCodeExpectedResult);
    }

    private JShellResult testEval(String testEvalId, String codeInput) {
        final String endpoint =
                String.join("/", ApiEndpoints.BASE, ApiEndpoints.EVALUATE, testEvalId);

        JShellResult result = this.webTestClient.mutate()
            .responseTimeout(Duration.ofSeconds(6))
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

        assertThat(result).isNotNull();

        return result;
    }

    private static JShellResult getJShellResultDefaultInstance(JShellSnippetResult snippetResult) {
        return new JShellResult(List.of(snippetResult), null, false, "");
    }
}
