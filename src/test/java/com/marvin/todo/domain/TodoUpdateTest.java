package com.marvin.todo.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TodoUpdateTest {

    private static TodoUpdate update(String rawTitle, String rawDescription) {
        return TodoUpdate.of(rawTitle, rawDescription, null, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("AC-02-5 기존 할 일이 있을 때 제목을 비우거나 공백만 넣어 수정하면 수정은 거부되고 \"제목을 입력해 주세요.\"라는 안내가 돌아오며 기존 내용은 그대로다")
    void 제목을_비우거나_공백만_넣어_수정하면_제목_없음_오류(String rawTitle) {
        assertThatThrownBy(() -> update(rawTitle, null))
                .isInstanceOfSatisfying(TodoException.class,
                        e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TITLE_REQUIRED));
    }

    @Test
    @DisplayName("AC-02-6 설명이 1,001자일 때 추가 또는 수정하면 거부되고 \"설명은 1,000자 이하로 입력해 주세요.\"라는 안내가 돌아온다")
    void 설명이_1001자이면_설명_너무_김_오류() {
        assertThatThrownBy(() -> update("제목", "가".repeat(1001)))
                .isInstanceOfSatisfying(TodoException.class,
                        e -> assertThat(e.code()).isEqualTo(TodoErrorCode.DESCRIPTION_TOO_LONG));
    }

    @Test
    @DisplayName("설명이 정확히 1,000자이면 만들어진다 (입력 규칙)")
    void 설명이_1000자이면_만들어진다() {
        String description = "가".repeat(1000);

        assertThat(update("제목", description).description()).isEqualTo(description);
    }

    @Test
    @DisplayName("설명은 앞뒤 공백을 제거한 값이 저장된다 (입력 규칙)")
    void 설명_앞뒤_공백은_제거된다() {
        assertThat(update("제목", "  \t목적과 범위 정리\u3000 ").description()).isEqualTo("목적과 범위 정리");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n", "\u3000"})
    @DisplayName("설명이 없거나 공백뿐이면 설명 없음으로 본다 (입력 규칙, AC-08-5·AC-08-6의 도메인 규칙)")
    void 설명이_없거나_공백뿐이면_설명_없음(String rawDescription) {
        assertThat(update("제목", rawDescription).description()).isNull();
    }

    @Test
    @DisplayName("오류가 여러 개여도 첫 오류 하나만 알려 준다 (검증 순서: 제목이 설명보다 먼저)")
    void 제목과_설명이_모두_잘못되면_제목_오류만_난다() {
        assertThatThrownBy(() -> update("", "가".repeat(1001)))
                .isInstanceOfSatisfying(TodoException.class,
                        e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TITLE_REQUIRED));
    }
}
