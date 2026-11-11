package com.marvin.todo.domain;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    @Nested
    @DisplayName("제목·설명 검증 순서")
    class TitleBeforeDescription {

        @Test
        @DisplayName("오류가 여러 개여도 첫 오류 하나만 알려 준다 (검증 순서)")
        void 제목과_설명이_모두_잘못되면_제목_오류만_난다() {
            assertThatThrownBy(() -> update("", "가".repeat(1001)))
                    .isInstanceOfSatisfying(TodoException.class,
                            e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TITLE_REQUIRED));
        }
    }

    static Stream<Arguments> 불변식을_어기는_값() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of(" 제목", null),
                Arguments.of("가".repeat(101), null),
                Arguments.of("제목", ""),
                Arguments.of("제목", "설명 "),
                Arguments.of("제목", "가".repeat(1001)));
    }

    @ParameterizedTest
    @MethodSource("불변식을_어기는_값")
    @DisplayName("정규화된 값이 불변식을 어기면 `TodoUpdate`를 만들 수 없다 (DATA_MODEL 2)")
    void 불변식을_어기면_만들_수_없다(String title, String description) {
        assertThatThrownBy(() -> new TodoUpdate(title, description, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static TodoUpdate updateWithDueDate(String rawDueDateText) {
        return TodoUpdate.of("제목", null, rawDueDateText, false);
    }

    @Test
    @DisplayName("AC-09-1 마감일이 없는 할 일이 있을 때 마감일 2026-11-12를 지정하면 마감일이 2026-11-12로 저장된다")
    void 마감일을_지정하면_그_날짜가_된다() {
        assertThat(updateWithDueDate("2026-11-12").dueDate()).isEqualTo(LocalDate.of(2026, 11, 12));
    }

    @Test
    @DisplayName("마감일 앞뒤에 공백이 있어도 공백을 제거한 뒤 읽는다 (설계)")
    void 마감일_앞뒤_공백은_제거하고_읽는다() {
        assertThat(updateWithDueDate(" 2026-11-12\u3000").dueDate()).isEqualTo(LocalDate.of(2026, 11, 12));
    }

    @Test
    @DisplayName("AC-09-2 마감일이 있는 할 일이 있을 때 마감일을 지우면 마감일 없음 상태가 된다")
    void 마감일이_null이면_마감일_없음() {
        assertThat(updateWithDueDate(null).dueDate()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t\n", "\u3000"})
    @DisplayName("AC-09-6 마감일이 있는 할 일이 있을 때 마감일을 빈 문자열이나 공백 문자로만 이루어진 값으로 저장하면 마감일 없음 상태가 된다")
    void 마감일이_빈_문자열이나_공백뿐이면_마감일_없음(String rawDueDateText) {
        assertThat(updateWithDueDate(rawDueDateText).dueDate()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "2026-02-30", "2026-1-5", "2026-11-12T00:00", "+12345-01-01", "123", "true"})
    @DisplayName("AC-09-4 날짜가 아닌 값을 마감일로 보낼 때 저장하면 \"마감일을 올바른 날짜로 입력해 주세요.\"라는 안내가 돌아온다")
    void 날짜가_아닌_마감일은_마감일_오류(String rawDueDateText) {
        assertThatThrownBy(() -> updateWithDueDate(rawDueDateText))
                .isInstanceOfSatisfying(TodoException.class,
                        e -> assertThat(e.code()).isEqualTo(TodoErrorCode.INVALID_DUE_DATE));
    }

    @Test
    @DisplayName("AC-09-5 오늘이 2026-11-10일 때 마감일을 2026-11-01로 지정하면 저장된다 (도메인은 오늘을 모르므로 과거 날짜를 거부하지 않음)")
    void 과거_날짜도_마감일로_거부되지_않는다() {
        assertThat(updateWithDueDate("2026-11-01").dueDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Nested
    @DisplayName("설명·마감일 검증 순서")
    class DescriptionBeforeDueDate {

        @Test
        @DisplayName("오류가 여러 개여도 첫 오류 하나만 알려 준다 (검증 순서)")
        void 설명과_마감일이_모두_잘못되면_설명_오류만_난다() {
            assertThatThrownBy(() -> TodoUpdate.of("제목", "가".repeat(1001), "abc", false))
                    .isInstanceOfSatisfying(TodoException.class,
                            e -> assertThat(e.code()).isEqualTo(TodoErrorCode.DESCRIPTION_TOO_LONG));
        }
    }
}
