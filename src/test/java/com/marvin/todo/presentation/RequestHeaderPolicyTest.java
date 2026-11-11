package com.marvin.todo.presentation;

import java.util.stream.Stream;

import com.marvin.todo.application.ListResult;
import com.marvin.todo.application.StatusFilter;
import com.marvin.todo.application.Summary;
import com.marvin.todo.application.TodoService;
import com.marvin.todo.application.TodoView;
import com.marvin.todo.config.JacksonConfig;
import com.marvin.todo.domain.Todo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoController.class)
@Import(JacksonConfig.class)
class RequestHeaderPolicyTest {

    private static final String MALFORMED_MESSAGE = "요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodoService service;

    private static final Todo TODO = new Todo(1L, "가", null, null, false);

    private void 서비스가_정상_응답을_준다() {
        TodoView view = new TodoView(TODO, false);
        when(service.create(any())).thenReturn(view);
        when(service.get(anyLong())).thenReturn(view);
        when(service.update(anyLong(), any())).thenReturn(view);
        when(service.markCompleted(anyLong(), anyBoolean())).thenReturn(view);
        when(service.list(any(StatusFilter.class))).thenReturn(new ListResult(java.util.List.of(), new Summary(0, 0, 0, 0)));
    }

    static Stream<Arguments> 본문이_있는_요청과_잘못된_Content_Type() {
        Stream<Arguments> requests = Stream.of(
                Arguments.of(HttpMethod.POST, "/todos", "{\"title\":\"가\"}"),
                Arguments.of(HttpMethod.PUT, "/todos/1", "{\"title\":\"가\",\"completed\":false}"),
                Arguments.of(HttpMethod.PATCH, "/todos/1", "{\"completed\":true}"),
                Arguments.of(HttpMethod.POST, "/todos", ""));
        return requests.flatMap(r -> Stream.of(null, "text/plain", "application/xml")
                .map(contentType -> Arguments.of(r.get()[0], r.get()[1], r.get()[2], contentType)));
    }

    @ParameterizedTest
    @MethodSource("본문이_있는_요청과_잘못된_Content_Type")
    @DisplayName("AC-14-3 요청 내용을 읽을 수 없을 때(지원하지 않는 Content-Type) 어떤 요청이든 처리하면 거부되고 \"요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요.\"라는 안내가 돌아오며 저장된 내용은 바뀌지 않는다")
    void Content_Type이_없거나_JSON이_아니면_400_읽을_수_없음(HttpMethod method, String path, String body, String contentType) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, path).content(body);
        if (contentType != null) {
            builder.contentType(contentType);
        }

        mockMvc.perform(builder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.error.message").value(MALFORMED_MESSAGE));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("`Content-Type: application/json; charset=UTF-8`는 통과한다 (API_SPEC 1.2)")
    void charset이_붙은_JSON은_통과한다() throws Exception {
        서비스가_정상_응답을_준다();

        mockMvc.perform(request(HttpMethod.POST, "/todos")
                        .contentType("application/json; charset=UTF-8")
                        .content("{\"title\":\"가\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("`GET`, `DELETE`는 `Content-Type`이 없어도 통과하고 본문이 와도 무시한다 (API_SPEC 1.2)")
    void GET과_DELETE는_Content_Type_없이도_통과하고_본문을_무시한다() throws Exception {
        서비스가_정상_응답을_준다();

        mockMvc.perform(request(HttpMethod.GET, "/todos").content("garbage")).andExpect(status().isOk());
        mockMvc.perform(request(HttpMethod.DELETE, "/todos/1").content("garbage")).andExpect(status().isNoContent());
    }

    static Stream<Arguments> 여섯_엔드포인트() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/todos", "{\"title\":\"가\"}"),
                Arguments.of(HttpMethod.GET, "/todos", null),
                Arguments.of(HttpMethod.GET, "/todos/1", null),
                Arguments.of(HttpMethod.PUT, "/todos/1", "{\"title\":\"가\",\"completed\":false}"),
                Arguments.of(HttpMethod.PATCH, "/todos/1", "{\"completed\":true}"),
                Arguments.of(HttpMethod.DELETE, "/todos/1", null));
    }

    private MockHttpServletRequestBuilder 요청(HttpMethod method, String path, String body) {
        MockHttpServletRequestBuilder builder = request(method, path);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return builder;
    }

    @ParameterizedTest
    @MethodSource("여섯_엔드포인트")
    @DisplayName("AC-14-3 요청 내용을 읽을 수 없을 때(지원하지 않는 Accept) 어떤 요청이든 처리하면 거부되고 \"요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요.\"라는 안내가 돌아오며 저장된 내용은 바뀌지 않는다")
    void JSON을_받을_수_없는_Accept면_400이고_서비스는_호출되지_않는다(HttpMethod method, String path, String body) throws Exception {
        mockMvc.perform(요청(method, path, body).accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.error.message").value(MALFORMED_MESSAGE));

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "*/*", "application/json"})
    @DisplayName("`Accept`가 없거나 `*/*`, `application/json`이면 통과한다 (API_SPEC 1.2)")
    void Accept가_없거나_JSON을_받을_수_있으면_통과한다(String accept) throws Exception {
        서비스가_정상_응답을_준다();
        MockHttpServletRequestBuilder builder = request(HttpMethod.GET, "/todos");
        if (!accept.isEmpty()) {
            builder.accept(accept);
        }

        mockMvc.perform(builder).andExpect(status().isOk());
    }

    @Test
    @DisplayName("`Accept: text/html`일 때도 오류 응답은 JSON이다 (API_SPEC 1.3, TD-47, TD-51)")
    void Accept가_text_html이어도_오류_응답은_JSON이다() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/todos").accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }
}
