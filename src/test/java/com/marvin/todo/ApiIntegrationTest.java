package com.marvin.todo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiIntegrationTest {

    @TestConfiguration
    static class TestBeans {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-09T15:30:00Z"), ZoneId.of("Asia/Seoul"));
        }

        @Bean
        FailingController failingController() {
            return new FailingController();
        }
    }

    @RestController
    static class FailingController {

        @GetMapping("/test-only/fail")
        String fail() {
            throw new IllegalStateException("비밀 내부 사정");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("AC-10-6 한국 시간으로 2026-11-10 00:30(세계 표준시로는 11-09)일 때 마감일 2026-11-09인 진행 중 할 일의 기한 지남을 판정하면 오늘은 2026-11-10으로 계산되어 기한 지남이다")
    void 한국_날짜_기준으로_목록과_한_건_보기_모두_기한_지남이다() throws Exception {
        mockMvc.perform(post("/todos").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"장보기\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/todos/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"장보기\",\"dueDate\":\"2026-11-09\",\"completed\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].overdue").value(true));
        mockMvc.perform(get("/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue").value(true));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "POST|/todos|{\"title\":123}",
            "PUT|/todos/1|{\"title\":\"가\",\"completed\":\"true\"}",
            "PATCH|/todos/1|{\"completed\":\"true\"}",
            "POST|/todos|{\"title\":\"가\",\"extra\":1}"})
    @DisplayName("AC-14-3 요청 내용을 읽을 수 없을 때 어떤 요청이든 처리하면 거부되고 \"요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요.\"라는 안내가 돌아오며 저장된 내용은 바뀌지 않는다")
    void 실제_JSON_설정에서도_읽을_수_없는_요청은_거부된다(String method, String path, String body) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요."));
    }

    @Test
    @DisplayName("AC-14-5 서버가 제공하지 않는 주소로 요청할 때 처리하면 \"요청한 주소를 찾을 수 없어요.\"라는 안내가 같은 구성으로 돌아온다")
    void 실제_서버에서_없는_주소는_404다() throws Exception {
        mockMvc.perform(get("/nothing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ENDPOINT_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("요청한 주소를 찾을 수 없어요."));
    }

    @Test
    @DisplayName("AC-14-6 서버가 제공하는 주소에 허용되지 않는 요청 방식으로 요청할 때 처리하면 \"지원하지 않는 요청 방식이에요.\"라는 안내가 같은 구성으로 돌아온다")
    void 실제_서버에서_허용되지_않는_방식은_405다() throws Exception {
        mockMvc.perform(request(HttpMethod.DELETE, "/todos"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.error.message").value("지원하지 않는 요청 방식이에요."));
    }

    @Test
    @DisplayName("AC-14-4 서버 내부에서 예상하지 못한 오류가 났을 때 어떤 요청이든 처리하면 \"서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요.\"라는 안내가 같은 구성으로 돌아오고, 내부 정보는 담기지 않는다")
    void 실제_서버에서_예상_못한_오류는_500이고_내부_정보가_없다() throws Exception {
        String body = mockMvc.perform(get("/test-only/fail"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요."))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(body).doesNotContain("비밀 내부 사정", "IllegalStateException", "com.marvin");
    }
}
