package com.marvin.todo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewTodoTest {

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("AC-02-1 제목이 비어 있을 때 추가하면 할 일은 만들어지지 않고 \"제목을 입력해 주세요.\"라는 안내가 돌아온다")
    void 제목이_비어_있으면_제목_없음_오류(String rawTitle) {
        assertThatThrownBy(() -> NewTodo.of(rawTitle))
                .isInstanceOfSatisfying(TodoException.class,
                        e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TITLE_REQUIRED));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "   ", "\t", "\n", "　"})
    @DisplayName("AC-02-2 제목이 공백 문자로만 이루어져 있을 때 추가하면 비어 있는 경우와 똑같이 거부된다")
    void 제목이_공백뿐이면_제목_없음_오류(String rawTitle) {
        assertThatThrownBy(() -> NewTodo.of(rawTitle))
                .isInstanceOfSatisfying(TodoException.class,
                        e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TITLE_REQUIRED));
    }

    @Test
    @DisplayName("AC-01-3 제목 앞뒤에 공백이 있을 때 추가하면 공백이 제거된 제목으로 저장된다")
    void 제목_앞뒤_공백은_제거되고_가운데_공백은_그대로다() {
        assertThat(NewTodo.of("  \t장보기 목록　 ").title()).isEqualTo("장보기 목록");
    }

    @Test
    @DisplayName("AC-02-4 제목이 정확히 100자일 때 추가하면 정상적으로 만들어진다")
    void 제목이_100자이면_만들어진다() {
        String title = "가".repeat(100);

        assertThat(NewTodo.of(title).title()).isEqualTo(title);
    }

    @Test
    @DisplayName("AC-02-3 제목이 101자일 때 추가하면 할 일은 만들어지지 않고 \"제목은 100자 이하로 입력해 주세요.\"라는 안내가 돌아온다")
    void 제목이_101자이면_제목_너무_김_오류() {
        assertThatThrownBy(() -> NewTodo.of("가".repeat(101)))
                .isInstanceOfSatisfying(TodoException.class,
                        e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TITLE_TOO_LONG));
    }

    @Test
    @DisplayName("앞뒤 공백을 제거한 뒤에 글자 수를 센다 (입력 규칙)")
    void 공백_제거_후에_글자_수를_센다() {
        String title = "가".repeat(100);

        assertThat(NewTodo.of("  " + title + " ").title()).isEqualTo(title);
    }

    @Test
    @DisplayName("글자 수는 한글, 영문, 숫자, 공백, 이모지 모두 1자로 센다 (입력 규칙)")
    void 이모지도_한_글자로_센다() {
        String hundredEmojis = "😀".repeat(100);

        assertThat(NewTodo.of(hundredEmojis).title()).isEqualTo(hundredEmojis);
        assertThatThrownBy(() -> NewTodo.of("😀".repeat(101)))
                .isInstanceOfSatisfying(TodoException.class,
                        e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TITLE_TOO_LONG));
    }
}
