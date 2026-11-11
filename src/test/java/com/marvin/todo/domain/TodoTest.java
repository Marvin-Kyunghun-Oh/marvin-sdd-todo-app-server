package com.marvin.todo.domain;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TodoTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private static Todo todo(LocalDate dueDate, boolean completed) {
        return new Todo(1L, "제목", null, dueDate, completed);
    }

    @Test
    @DisplayName("AC-10-1 오늘이 2026-11-10이고, 진행 중이며 마감일이 2026-11-06인 할 일이 있을 때 목록을 보면 그 할 일은 기한 지남이다")
    void 진행_중이고_마감일이_오늘보다_이전이면_기한_지남이다() {
        assertThat(todo(LocalDate.of(2026, 11, 6), false).isOverdue(TODAY)).isTrue();
    }

    @Test
    @DisplayName("AC-10-2 오늘이 2026-11-10이고, 진행 중이며 마감일이 2026-11-10인 할 일이 있을 때 목록을 보면 기한 지남이 아니다")
    void 마감일이_오늘이면_기한_지남이_아니다() {
        assertThat(todo(LocalDate.of(2026, 11, 10), false).isOverdue(TODAY)).isFalse();
    }

    @Test
    @DisplayName("AC-10-3 오늘이 2026-11-10이고, 진행 중이며 마감일이 2026-11-12인 할 일이 있을 때 목록을 보면 기한 지남이 아니다")
    void 마감일이_오늘보다_이후이면_기한_지남이_아니다() {
        assertThat(todo(LocalDate.of(2026, 11, 12), false).isOverdue(TODAY)).isFalse();
    }

    @Test
    @DisplayName("AC-10-4 오늘이 2026-11-10이고, 완료되었으며 마감일이 2026-11-09인 할 일이 있을 때 목록을 보면 기한 지남이 아니다")
    void 완료된_할_일은_기한_지남이_아니다() {
        assertThat(todo(LocalDate.of(2026, 11, 9), true).isOverdue(TODAY)).isFalse();
    }

    @Test
    @DisplayName("AC-09-2 마감일이 있는 할 일이 있을 때 마감일을 지우면 마감일 없음 상태가 되고, 기한 지남이 아니다")
    void 마감일이_없으면_기한_지남이_아니다() {
        assertThat(todo(null, false).isOverdue(TODAY)).isFalse();
    }

    static Stream<Arguments> 불변식을_어기는_값() {
        return Stream.of(
                Arguments.of(0L, "제목", null),
                Arguments.of(-1L, "제목", null),
                Arguments.of(1L, "", null),
                Arguments.of(1L, " 제목", null),
                Arguments.of(1L, "가".repeat(101), null),
                Arguments.of(1L, "제목", ""),
                Arguments.of(1L, "제목", " 설명"),
                Arguments.of(1L, "제목", "가".repeat(1001)));
    }

    @ParameterizedTest
    @MethodSource("불변식을_어기는_값")
    @DisplayName("정규화된 값이 불변식을 어기면 만들 수 없다 (DATA_MODEL 2)")
    void 불변식을_어기면_만들_수_없다(long id, String title, String description) {
        assertThatThrownBy(() -> new Todo(id, title, description, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
