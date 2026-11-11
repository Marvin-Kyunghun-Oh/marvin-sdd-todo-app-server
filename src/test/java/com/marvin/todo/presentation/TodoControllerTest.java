package com.marvin.todo.presentation;

import java.util.stream.Stream;

import com.marvin.todo.application.CreateTodoCommand;
import com.marvin.todo.application.TodoService;
import com.marvin.todo.application.TodoView;
import com.marvin.todo.domain.Todo;
import com.marvin.todo.domain.TodoErrorCode;
import com.marvin.todo.domain.TodoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodoService service;

    private org.springframework.test.web.servlet.ResultActions postTodo(String body) throws Exception {
        return mockMvc.perform(post("/todos").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    @DisplayName("AC-01-1 할 일이 하나도 없을 때 제목 \"장보기\"로 추가하면 제목 \"장보기\", 설명 없음, 마감일 없음, 진행 중인 할 일이 새로 만들어진다")
    void 추가하면_201과_할_일_모양이_돌아온다() throws Exception {
        when(service.create(new CreateTodoCommand("장보기")))
                .thenReturn(new TodoView(new Todo(1L, "장보기", null, null, false), false));

        postTodo("{\"title\":\"장보기\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("장보기"))
                .andExpect(jsonPath("$.description").value((Object) null))
                .andExpect(jsonPath("$.dueDate").value((Object) null))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.overdue").value(false));
    }

    static Stream<Arguments> 비어_있는_제목_요청() {
        return Stream.of(
                Arguments.of("{}", null),
                Arguments.of("{\"title\":null}", null),
                Arguments.of("{\"title\":\"\"}", ""));
    }

    @ParameterizedTest
    @MethodSource("비어_있는_제목_요청")
    @DisplayName("AC-02-1 제목이 비어 있을 때 추가하면 할 일은 만들어지지 않고 \"제목을 입력해 주세요.\"라는 안내가 돌아온다")
    void 제목이_비어_있으면_400_제목_없음(String body, String expectedTitle) throws Exception {
        when(service.create(any())).thenThrow(new TodoException(TodoErrorCode.TITLE_REQUIRED));

        postTodo(body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TITLE_REQUIRED"))
                .andExpect(jsonPath("$.error.message").value("제목을 입력해 주세요."));

        verify(service).create(new CreateTodoCommand(expectedTitle));
    }

    @Test
    @DisplayName("AC-02-2 제목이 공백 문자로만 이루어져 있을 때 추가하면 비어 있는 경우와 똑같이 거부된다")
    void 제목이_공백뿐이면_400_제목_없음() throws Exception {
        when(service.create(any())).thenThrow(new TodoException(TodoErrorCode.TITLE_REQUIRED));

        postTodo("{\"title\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TITLE_REQUIRED"));

        verify(service).create(new CreateTodoCommand("   "));
    }

    @Test
    @DisplayName("AC-02-3 제목이 101자일 때 추가하면 할 일은 만들어지지 않고 \"제목은 100자 이하로 입력해 주세요.\"라는 안내가 돌아온다")
    void 제목이_101자이면_400_제목_너무_김() throws Exception {
        when(service.create(any())).thenThrow(new TodoException(TodoErrorCode.TITLE_TOO_LONG));

        postTodo("{\"title\":\"" + "가".repeat(101) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TITLE_TOO_LONG"))
                .andExpect(jsonPath("$.error.message").value("제목은 100자 이하로 입력해 주세요."));
    }

    @Test
    @DisplayName("AC-14-1 서로 다른 이유(제목 없음, 너무 긴 제목, 없는 할 일)로 실패했을 때 안내를 비교하면 모두 같은 구성(실패 종류, 사람이 읽을 문장)을 가진다")
    void 서로_다른_실패도_같은_구성의_안내다() throws Exception {
        for (TodoErrorCode code : new TodoErrorCode[] {
                TodoErrorCode.TITLE_REQUIRED, TodoErrorCode.TITLE_TOO_LONG, TodoErrorCode.TODO_NOT_FOUND}) {
            doThrow(new TodoException(code)).when(service).create(any());

            postTodo("{\"title\":\"x\"}")
                    .andExpect(jsonPath("$.error.code").value(code.name()))
                    .andExpect(jsonPath("$.error.message").isString())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$.error.length()").value(2));
        }
    }

    static Stream<Arguments> 오류_코드와_응답() {
        return Stream.of(
                Arguments.of(TodoErrorCode.TITLE_REQUIRED, 400, "제목을 입력해 주세요."),
                Arguments.of(TodoErrorCode.TITLE_TOO_LONG, 400, "제목은 100자 이하로 입력해 주세요."),
                Arguments.of(TodoErrorCode.DESCRIPTION_TOO_LONG, 400, "설명은 1,000자 이하로 입력해 주세요."),
                Arguments.of(TodoErrorCode.INVALID_DUE_DATE, 400, "마감일을 올바른 날짜로 입력해 주세요."),
                Arguments.of(TodoErrorCode.TODO_NOT_FOUND, 404, "할 일을 찾을 수 없어요. 이미 삭제되었을 수 있어요."));
    }

    @ParameterizedTest
    @MethodSource("오류_코드와_응답")
    @DisplayName("도메인 오류 코드마다 상태 코드와 확정 문구가 일치한다 (API_SPEC 1.8)")
    void 오류_코드마다_상태와_문구가_일치한다(TodoErrorCode code, int expectedStatus, String expectedMessage) throws Exception {
        when(service.create(any())).thenThrow(new TodoException(code));

        postTodo("{\"title\":\"x\"}")
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(code.name()))
                .andExpect(jsonPath("$.error.message").value(expectedMessage));
    }
}
