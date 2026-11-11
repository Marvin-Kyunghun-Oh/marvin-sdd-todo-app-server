package com.marvin.todo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class TodoApplicationTest {

    @Test
    @DisplayName("애플리케이션이 뜬다")
    void 애플리케이션이_뜬다() {
    }

    @Test
    @DisplayName("애플리케이션 시작 지점을 실행하면 정상적으로 시작된다")
    void 애플리케이션_시작_지점을_실행하면_정상적으로_시작된다() {
        assertThatCode(() -> TodoApplication.main(new String[]{"--spring.main.web-application-type=none"}))
                .doesNotThrowAnyException();
    }
}
