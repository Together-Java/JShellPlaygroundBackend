package org.togetherjava.jshellapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// TODO - write some integrations
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JShellApiTests {

    @Test
    public void test() {
        assertThat(true).isTrue();
    }
}
