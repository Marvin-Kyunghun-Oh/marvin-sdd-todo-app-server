package com.marvin.todo.presentation;

import com.marvin.todo.application.StatusFilter;
import com.marvin.todo.application.TodoService;
import com.marvin.todo.config.JacksonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoController.class)
@Import(JacksonConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class FrameworkErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TodoService service;

    @ParameterizedTest
    @ValueSource(strings = {"/nothing", "/todos/", "/todos/1/extra"})
    @DisplayName("AC-14-5 서버가 제공하지 않는 주소로 요청할 때 처리하면 \"요청한 주소를 찾을 수 없어요.\"라는 안내가 같은 구성으로 돌아온다")
    void 없는_주소는_404_주소_없음(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.code").value("ENDPOINT_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("요청한 주소를 찾을 수 없어요."));
    }

    @ParameterizedTest
    @CsvSource({"DELETE,/todos", "PUT,/todos", "POST,/todos/1"})
    @DisplayName("AC-14-6 서버가 제공하는 주소에 허용되지 않는 요청 방식으로 요청할 때 처리하면 \"지원하지 않는 요청 방식이에요.\"라는 안내가 같은 구성으로 돌아온다")
    void 허용되지_않는_방식은_405(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.error.message").value("지원하지 않는 요청 방식이에요."));
    }

    @Test
    @DisplayName("AC-14-4 서버 내부에서 예상하지 못한 오류가 났을 때 어떤 요청이든 처리하면 \"서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요.\"라는 안내가 같은 구성으로 돌아오고, 내부 정보는 담기지 않는다")
    void 예상_못한_오류는_500이고_내부_정보가_없다() throws Exception {
        when(service.list(StatusFilter.ALL)).thenThrow(new IllegalStateException("비밀 내부 사정"));

        String body = mockMvc.perform(get("/todos"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요."))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(body).doesNotContain("비밀 내부 사정", "IllegalStateException", "com.marvin", "at ");
    }

    @Test
    @DisplayName("예상 못한 오류는 삼키지 않고 서버 로그에 남긴다 (CLAUDE.md 2.4.3)")
    void 예상_못한_오류는_로그에_남는다(CapturedOutput output) throws Exception {
        when(service.list(StatusFilter.ALL)).thenThrow(new IllegalStateException("비밀 내부 사정"));

        mockMvc.perform(get("/todos")).andExpect(status().isInternalServerError());

        assertThat(output.getAll()).contains("IllegalStateException", "비밀 내부 사정");
    }
}
