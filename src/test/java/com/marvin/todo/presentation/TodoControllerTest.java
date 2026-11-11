package com.marvin.todo.presentation;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import com.marvin.todo.application.CreateTodoCommand;
import com.marvin.todo.application.ListResult;
import com.marvin.todo.application.StatusFilter;
import com.marvin.todo.application.Summary;
import com.marvin.todo.application.TodoService;
import com.marvin.todo.application.TodoView;
import com.marvin.todo.config.JacksonConfig;
import com.marvin.todo.domain.Todo;
import com.marvin.todo.domain.TodoErrorCode;
import com.marvin.todo.domain.TodoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoController.class)
@Import(JacksonConfig.class)
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

    @Nested
    @DisplayName("요청 본문 읽기 정책")
    class RequestBodyPolicy {

        private static final String MALFORMED_MESSAGE = "요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요.";

        private void 거부되고_서비스는_호출되지_않는다(String body) throws Exception {
            postTodo(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"))
                    .andExpect(jsonPath("$.error.message").value(MALFORMED_MESSAGE));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("AC-14-3 요청 내용을 읽을 수 없을 때(값의 종류가 맞지 않음) 처리하면 거부되고 \"요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요.\"라는 안내가 돌아오며 저장된 내용은 바뀌지 않는다")
        void 값의_종류가_맞지_않으면_400_읽을_수_없음() throws Exception {
            거부되고_서비스는_호출되지_않는다("{\"title\":123}");
        }

        @Test
        @DisplayName("AC-14-3 요청 내용을 읽을 수 없을 때(깨진 형식) 처리하면 거부되고 \"요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요.\"라는 안내가 돌아오며 저장된 내용은 바뀌지 않는다")
        void 깨진_JSON이면_400_읽을_수_없음() throws Exception {
            거부되고_서비스는_호출되지_않는다("{\"title\":");
        }

        @Test
        @DisplayName("AC-14-3 요청 내용을 읽을 수 없을 때(알 수 없는 항목) 처리하면 거부되고 \"요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요.\"라는 안내가 돌아오며 저장된 내용은 바뀌지 않는다")
        void 알_수_없는_필드가_있으면_400_읽을_수_없음() throws Exception {
            거부되고_서비스는_호출되지_않는다("{\"title\":\"장보기\",\"description\":\"설명\"}");
        }

        @Test
        @DisplayName("AC-14-3 요청 내용을 읽을 수 없을 때(본문 없음) 처리하면 거부되고 \"요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요.\"라는 안내가 돌아오며 저장된 내용은 바뀌지 않는다")
        void 본문이_없으면_400_읽을_수_없음() throws Exception {
            거부되고_서비스는_호출되지_않는다("");
        }
    }

    @Nested
    @DisplayName("목록과 요약")
    class ListTodos {

        private static final Summary FOUR_ONE = new Summary(4, 3, 1, 3);
        private static final Summary EMPTY = new Summary(0, 0, 0, 0);

        private ListResult 가나다() {
            return new ListResult(List.of(
                    new TodoView(new Todo(1L, "가", null, LocalDate.of(2026, 11, 6), false), true),
                    new TodoView(new Todo(2L, "나", "목적과 범위 정리", LocalDate.of(2026, 11, 12), false), false),
                    new TodoView(new Todo(3L, "다", null, null, true), false)), FOUR_ONE);
        }

        @Test
        @DisplayName("AC-03-1 할 일 \"가\", \"나\", \"다\"가 이 순서로 등록되어 있을 때 목록을 보면 \"가\", \"나\", \"다\" 순서로 돌아온다")
        void 서비스가_준_순서대로_items가_나온다() throws Exception {
            when(service.list(StatusFilter.ALL)).thenReturn(가나다());

            mockMvc.perform(get("/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].title").value("가"))
                    .andExpect(jsonPath("$.items[1].title").value("나"))
                    .andExpect(jsonPath("$.items[2].title").value("다"));
        }

        @Test
        @DisplayName("AC-03-3 목록을 보면 각 할 일에 대해 제목, 설명, 마감일, 완료 여부, 기한 지남 여부가 함께 돌아온다")
        void 각_항목에_여섯_필드가_있다() throws Exception {
            when(service.list(StatusFilter.ALL)).thenReturn(가나다());

            mockMvc.perform(get("/todos"))
                    .andExpect(jsonPath("$.items[1].id").value(2))
                    .andExpect(jsonPath("$.items[1].title").value("나"))
                    .andExpect(jsonPath("$.items[1].description").value("목적과 범위 정리"))
                    .andExpect(jsonPath("$.items[1].dueDate").value("2026-11-12"))
                    .andExpect(jsonPath("$.items[1].completed").value(false))
                    .andExpect(jsonPath("$.items[1].overdue").value(false))
                    .andExpect(jsonPath("$.items[0].overdue").value(true));
        }

        @Test
        @DisplayName("AC-09-3 마감일이 없는 할 일이 있을 때 목록을 보면 마감일 없음으로 돌아온다")
        void 마감일과_설명이_없으면_null로_나온다() throws Exception {
            when(service.list(StatusFilter.ALL)).thenReturn(가나다());

            mockMvc.perform(get("/todos"))
                    .andExpect(jsonPath("$.items[2].dueDate").value((Object) null))
                    .andExpect(jsonPath("$.items[2].description").value((Object) null))
                    .andExpect(jsonPath("$.items[2].length()").value(6));
        }

        @Test
        @DisplayName("AC-04-3 진행 중 3개, 완료 1개가 있을 때 전체를 보면 4개가 모두 돌아온다")
        void status가_없으면_전체로_요청한다() throws Exception {
            when(service.list(StatusFilter.ALL)).thenReturn(가나다());

            mockMvc.perform(get("/todos")).andExpect(status().isOk());

            verify(service).list(StatusFilter.ALL);
        }

        @Test
        @DisplayName("AC-04-1 진행 중 3개, 완료 1개가 있을 때 진행 중만 보면 진행 중 3개만 등록 순서대로 돌아온다")
        void status가_active이면_진행_중으로_요청한다() throws Exception {
            when(service.list(StatusFilter.ACTIVE)).thenReturn(가나다());

            mockMvc.perform(get("/todos").param("status", "active")).andExpect(status().isOk());

            verify(service).list(StatusFilter.ACTIVE);
        }

        @Test
        @DisplayName("AC-04-2 진행 중 3개, 완료 1개가 있을 때 완료만 보면 완료 1개만 돌아온다")
        void status가_done이면_완료로_요청한다() throws Exception {
            when(service.list(StatusFilter.DONE)).thenReturn(가나다());

            mockMvc.perform(get("/todos").param("status", "done")).andExpect(status().isOk());

            verify(service).list(StatusFilter.DONE);
        }

        @Test
        @DisplayName("AC-04-4 완료된 할 일이 하나도 없을 때 완료만 보면 오류 없이 빈 목록이 돌아온다")
        void 결과가_비면_200과_빈_items다() throws Exception {
            when(service.list(StatusFilter.DONE)).thenReturn(new ListResult(List.of(), FOUR_ONE));

            mockMvc.perform(get("/todos").param("status", "done"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isEmpty());
        }

        @Test
        @DisplayName("AC-13-1 할 일이 하나도 없을 때 목록을 보면 오류 없이 빈 목록과 모든 개수 0이 돌아온다")
        void 할_일이_없으면_빈_items와_0_요약이다() throws Exception {
            when(service.list(StatusFilter.ALL)).thenReturn(new ListResult(List.of(), EMPTY));

            mockMvc.perform(get("/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isEmpty())
                    .andExpect(jsonPath("$.summary.total").value(0))
                    .andExpect(jsonPath("$.summary.active").value(0))
                    .andExpect(jsonPath("$.summary.done").value(0))
                    .andExpect(jsonPath("$.summary.remaining").value(0));
        }

        @Test
        @DisplayName("AC-05-2 완료만 보면서 요약을 함께 받으면 요약 값은 위와 같고, 보기 선택에 영향을 받지 않는다")
        void 요약은_서비스가_준_값_그대로다() throws Exception {
            when(service.list(StatusFilter.DONE)).thenReturn(new ListResult(List.of(), FOUR_ONE));

            mockMvc.perform(get("/todos").param("status", "done"))
                    .andExpect(jsonPath("$.summary.total").value(4))
                    .andExpect(jsonPath("$.summary.active").value(3))
                    .andExpect(jsonPath("$.summary.done").value(1))
                    .andExpect(jsonPath("$.summary.remaining").value(3));
        }

        @Test
        @DisplayName("AC-05-3 할 일이 하나도 없을 때 요약을 보면 전체 0, 진행 중 0, 완료 0, 남은 개수 0이 돌아온다")
        void 요약_네_값이_0이다() throws Exception {
            when(service.list(StatusFilter.ALL)).thenReturn(new ListResult(List.of(), EMPTY));

            mockMvc.perform(get("/todos"))
                    .andExpect(jsonPath("$.summary.length()").value(4))
                    .andExpect(jsonPath("$.summary.total").value(0));
        }

        static Stream<Arguments> 허용되지_않는_status() {
            return Stream.of(
                    Arguments.of((Object) new String[] {"foo"}),
                    Arguments.of((Object) new String[] {"ALL"}),
                    Arguments.of((Object) new String[] {""}),
                    Arguments.of((Object) new String[] {"active", "done"}));
        }

        @ParameterizedTest
        @MethodSource("허용되지_않는_status")
        @DisplayName("AC-04-5 할 일이 있을 때 전체, 진행 중, 완료 외의 보기 구분으로 목록을 요청하면 거부되고 \"보기 구분이 올바르지 않아요. 전체, 진행 중, 완료 중에서 골라 주세요.\"라는 안내가 돌아온다")
        void 허용되지_않는_status면_400_보기_구분_오류(String[] values) throws Exception {
            mockMvc.perform(get("/todos").param("status", values))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_FILTER"))
                    .andExpect(jsonPath("$.error.message")
                            .value("보기 구분이 올바르지 않아요. 전체, 진행 중, 완료 중에서 골라 주세요."));

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("표에 없는 쿼리 파라미터는 무시한다 (API_SPEC 2.2)")
        void 알_수_없는_파라미터는_무시한다() throws Exception {
            when(service.list(StatusFilter.ALL)).thenReturn(new ListResult(List.of(), EMPTY));

            mockMvc.perform(get("/todos").param("sort", "dueDate")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("한 건 보기와 삭제")
    class GetAndDelete {

        private static final String NOT_FOUND_MESSAGE = "할 일을 찾을 수 없어요. 이미 삭제되었을 수 있어요.";

        @Test
        @DisplayName("AC-07-1 할 일이 있을 때 그 할 일을 열면 제목, 설명, 마감일, 완료 여부, 기한 지남 여부가 돌아온다")
        void 있는_할_일을_열면_200과_할_일_모양이다() throws Exception {
            when(service.get(2L)).thenReturn(
                    new TodoView(new Todo(2L, "PRD 초안 작성하기", "목적과 범위 정리", LocalDate.of(2026, 11, 12), false), false));

            mockMvc.perform(get("/todos/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.title").value("PRD 초안 작성하기"))
                    .andExpect(jsonPath("$.description").value("목적과 범위 정리"))
                    .andExpect(jsonPath("$.dueDate").value("2026-11-12"))
                    .andExpect(jsonPath("$.completed").value(false))
                    .andExpect(jsonPath("$.overdue").value(false));
        }

        @Test
        @DisplayName("AC-12-1 한 번도 만들어진 적 없는 할 일을 가리킬 때 열면 \"할 일을 찾을 수 없어요. 이미 삭제되었을 수 있어요.\"라는 안내가 돌아온다")
        void 없는_할_일을_열면_404_할_일_없음() throws Exception {
            when(service.get(99L)).thenThrow(new TodoException(TodoErrorCode.TODO_NOT_FOUND));

            mockMvc.perform(get("/todos/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("TODO_NOT_FOUND"))
                    .andExpect(jsonPath("$.error.message").value(NOT_FOUND_MESSAGE));
        }

        @Test
        @DisplayName("AC-11-2 방금 삭제한 할 일을 가리킬 때 다시 열면 \"할 일을 찾을 수 없어요. 이미 삭제되었을 수 있어요.\"라는 안내가 돌아온다")
        void 서비스가_할_일_없음을_주면_404다() throws Exception {
            when(service.get(1L)).thenThrow(new TodoException(TodoErrorCode.TODO_NOT_FOUND));

            mockMvc.perform(get("/todos/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("TODO_NOT_FOUND"));
        }

        @Test
        @DisplayName("AC-11-1 할 일이 있을 때 삭제하면 그 할 일은 이후 목록과 요약에서 사라진다")
        void 삭제하면_204이고_본문이_없다() throws Exception {
            mockMvc.perform(delete("/todos/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(service).delete(1L);
        }

        @Test
        @DisplayName("AC-12-2 한 번도 만들어진 적 없는 할 일을 가리킬 때 삭제를 시도하면 같은 안내가 돌아온다")
        void 없는_할_일을_삭제하면_404_할_일_없음() throws Exception {
            doThrow(new TodoException(TodoErrorCode.TODO_NOT_FOUND)).when(service).delete(99L);

            mockMvc.perform(delete("/todos/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("TODO_NOT_FOUND"))
                    .andExpect(jsonPath("$.error.message").value(NOT_FOUND_MESSAGE));
        }

        static Stream<Arguments> 번호로_읽을_수_없는_id() {
            return Stream.of("abc", "0", "-1", "01", "+1", "1.5", "99999999999999999999")
                    .flatMap(id -> Stream.of(Arguments.of("GET", id), Arguments.of("DELETE", id)));
        }

        @ParameterizedTest
        @MethodSource("번호로_읽을_수_없는_id")
        @DisplayName("AC-12-3 번호로 읽을 수 없는 값(예: \"abc\", 0, 음수)으로 할 일을 가리킬 때 열기 또는 삭제를 시도하면 존재하지 않는 할 일과 같은 안내가 돌아온다")
        void 번호로_읽을_수_없으면_404이고_서비스는_호출되지_않는다(String method, String id) throws Exception {
            mockMvc.perform(request(HttpMethod.valueOf(method), "/todos/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("TODO_NOT_FOUND"))
                    .andExpect(jsonPath("$.error.message").value(NOT_FOUND_MESSAGE));

            verifyNoInteractions(service);
        }
    }
}
