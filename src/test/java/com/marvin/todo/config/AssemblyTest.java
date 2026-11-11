package com.marvin.todo.config;

import java.time.Clock;
import java.time.ZoneId;

import com.marvin.todo.application.TodoService;
import com.marvin.todo.domain.TodoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AssemblyTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("서비스, 저장소, `Clock`이 조립된 채로 뜬다 (TRD 2)")
    void 세_빈이_모두_있다() {
        assertThat(context.getBeansOfType(TodoService.class)).hasSize(1);
        assertThat(context.getBeansOfType(TodoRepository.class)).hasSize(1);
        assertThat(context.getBeansOfType(Clock.class)).hasSize(1);
    }

    @Test
    @DisplayName("`Clock`은 `Asia/Seoul` 기준이다 (TRD 2, D-10)")
    void 시계는_서울_시간대다() {
        assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }
}
