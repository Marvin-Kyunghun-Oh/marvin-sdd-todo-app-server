package com.marvin.todo;

import java.nio.charset.StandardCharsets;
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
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ScenarioIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private ResultActions create(String title) throws Exception {
        return mockMvc.perform(post("/todos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\"}"));
    }

    private ResultActions replace(long id, String json) throws Exception {
        return mockMvc.perform(put("/todos/{id}", id).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions complete(long id, boolean completed) throws Exception {
        return mockMvc.perform(patch("/todos/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":" + completed + "}"));
    }

    private String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String listBody() throws Exception {
        return body(mockMvc.perform(get("/todos")));
    }

    private void 네_개를_만든다_하나는_완료() throws Exception {
        create("가").andExpect(status().isCreated());
        create("나").andExpect(status().isCreated());
        create("다").andExpect(status().isCreated());
        create("라").andExpect(status().isCreated());
        complete(3, true).andExpect(status().isOk());
    }

    @Test
    @DisplayName("AC-13-1 할 일이 하나도 없을 때 목록을 보면 오류 없이 빈 목록과 모든 개수 0이 돌아온다")
    void 비어_있으면_빈_목록과_0_요약이다() throws Exception {
        mockMvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.summary.total").value(0))
                .andExpect(jsonPath("$.summary.active").value(0))
                .andExpect(jsonPath("$.summary.done").value(0))
                .andExpect(jsonPath("$.summary.remaining").value(0));
    }

    @Test
    @DisplayName("AC-01-1 할 일이 하나도 없을 때 제목 \"장보기\"로 추가하면 제목 \"장보기\", 설명 없음, 마감일 없음, 진행 중인 할 일이 새로 만들어진다")
    void 추가한_뒤_한_건_보기로_내용이_일치한다() throws Exception {
        create("장보기").andExpect(status().isCreated());

        mockMvc.perform(get("/todos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("장보기"))
                .andExpect(jsonPath("$.description").value((Object) null))
                .andExpect(jsonPath("$.dueDate").value((Object) null))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    @DisplayName("AC-01-2 같은 제목의 할 일이 이미 있을 때 같은 제목으로 추가하면 새 할 일이 별도로 만들어진다")
    void 같은_제목이어도_번호가_다른_두_항목이다() throws Exception {
        create("장보기");
        create("장보기");

        mockMvc.perform(get("/todos"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[1].id").value(2));
    }

    @Test
    @DisplayName("AC-03-1 할 일 \"가\", \"나\", \"다\"가 이 순서로 등록되어 있을 때 목록을 보면 \"가\", \"나\", \"다\" 순서로 돌아온다")
    void 목록은_등록한_순서다() throws Exception {
        create("가");
        create("나");
        create("다");

        mockMvc.perform(get("/todos"))
                .andExpect(jsonPath("$.items[0].title").value("가"))
                .andExpect(jsonPath("$.items[1].title").value("나"))
                .andExpect(jsonPath("$.items[2].title").value("다"));
    }

    @Test
    @DisplayName("AC-03-2 마감일이나 완료 여부가 서로 다른 할 일들이 있을 때 목록을 보면 마감일이나 완료 여부와 관계없이 등록한 순서를 유지한다")
    void 마감일과_완료_여부를_달리해도_순서가_같다() throws Exception {
        create("가");
        create("나");
        create("다");
        replace(1, "{\"title\":\"가\",\"dueDate\":\"2026-12-31\",\"completed\":true}");
        replace(2, "{\"title\":\"나\",\"dueDate\":\"2026-11-01\",\"completed\":false}");

        mockMvc.perform(get("/todos"))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.items[2].id").value(3));
    }

    @Test
    @DisplayName("AC-08-1 제목 \"PRD 초안\", 마감일 없음인 할 일이 있을 때 제목을 \"PRD 초안 작성하기\", 설명을 \"목적과 범위 정리\", 마감일을 2026-11-12로 저장하면 세 값이 모두 반영되어 돌아온다")
    void 수정이_한_건_보기와_목록에_바로_반영된다() throws Exception {
        create("PRD 초안");

        replace(1, "{\"title\":\"PRD 초안 작성하기\",\"description\":\"목적과 범위 정리\","
                + "\"dueDate\":\"2026-11-12\",\"completed\":false}")
                .andExpect(status().isOk());

        mockMvc.perform(get("/todos/1"))
                .andExpect(jsonPath("$.title").value("PRD 초안 작성하기"))
                .andExpect(jsonPath("$.description").value("목적과 범위 정리"))
                .andExpect(jsonPath("$.dueDate").value("2026-11-12"));
        mockMvc.perform(get("/todos"))
                .andExpect(jsonPath("$.items[0].title").value("PRD 초안 작성하기"))
                .andExpect(jsonPath("$.items[0].description").value("목적과 범위 정리"))
                .andExpect(jsonPath("$.items[0].dueDate").value("2026-11-12"));
    }

    @Test
    @DisplayName("AC-06-1 진행 중인 할 일이 있을 때 완료로 표시하면 그 할 일은 완료가 되고 요약의 진행 중은 1 줄고 완료는 1 는다")
    void 완료로_표시하면_요약이_바뀐다() throws Exception {
        create("가");
        create("나");
        mockMvc.perform(get("/todos"))
                .andExpect(jsonPath("$.summary.active").value(2))
                .andExpect(jsonPath("$.summary.done").value(0));

        complete(1, true).andExpect(jsonPath("$.completed").value(true));

        mockMvc.perform(get("/todos"))
                .andExpect(jsonPath("$.summary.total").value(2))
                .andExpect(jsonPath("$.summary.active").value(1))
                .andExpect(jsonPath("$.summary.done").value(1));
    }

    @Test
    @DisplayName("AC-06-2 완료된 할 일이 있을 때 진행 중으로 되돌리면 그 할 일은 진행 중이 되고 요약이 그에 맞게 바뀐다")
    void 되돌리면_요약이_바뀐다() throws Exception {
        create("가");
        create("나");
        complete(1, true);
        mockMvc.perform(get("/todos")).andExpect(jsonPath("$.summary.done").value(1));

        complete(1, false).andExpect(jsonPath("$.completed").value(false));

        mockMvc.perform(get("/todos"))
                .andExpect(jsonPath("$.summary.active").value(2))
                .andExpect(jsonPath("$.summary.done").value(0));
    }

    @Test
    @DisplayName("AC-05-1 진행 중 3개, 완료 1개가 있을 때 요약을 보면 전체 4, 진행 중 3, 완료 1, 남은 개수 3이 돌아온다")
    void 요약은_실제_개수다() throws Exception {
        네_개를_만든다_하나는_완료();

        mockMvc.perform(get("/todos"))
                .andExpect(jsonPath("$.summary.total").value(4))
                .andExpect(jsonPath("$.summary.active").value(3))
                .andExpect(jsonPath("$.summary.done").value(1))
                .andExpect(jsonPath("$.summary.remaining").value(3));
    }

    @Test
    @DisplayName("AC-05-2 진행 중 3개, 완료 1개가 있을 때 완료만 보면서 요약을 함께 받으면 요약 값은 위와 같고, 보기 선택에 영향을 받지 않는다")
    void 완료만_봐도_요약은_같다() throws Exception {
        네_개를_만든다_하나는_완료();

        mockMvc.perform(get("/todos").param("status", "done"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.summary.total").value(4))
                .andExpect(jsonPath("$.summary.active").value(3))
                .andExpect(jsonPath("$.summary.done").value(1))
                .andExpect(jsonPath("$.summary.remaining").value(3));
    }

    @Test
    @DisplayName("AC-11-1 할 일이 있을 때 삭제하면 그 할 일은 이후 목록과 요약에서 사라진다")
    void 삭제하면_목록과_요약에서_사라진다() throws Exception {
        create("가");
        create("나");

        mockMvc.perform(delete("/todos/1")).andExpect(status().isNoContent());

        mockMvc.perform(get("/todos"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("나"))
                .andExpect(jsonPath("$.summary.total").value(1));
    }

    @Test
    @DisplayName("AC-11-2 방금 삭제한 할 일을 가리킬 때 다시 열면 \"할 일을 찾을 수 없어요. 이미 삭제되었을 수 있어요.\"라는 안내가 돌아온다")
    void 삭제한_뒤_열면_404다() throws Exception {
        create("가");
        mockMvc.perform(delete("/todos/1")).andExpect(status().isNoContent());

        mockMvc.perform(get("/todos/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TODO_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("할 일을 찾을 수 없어요. 이미 삭제되었을 수 있어요."));
    }

    @Test
    @DisplayName("AC-14-2 추가가 거부되었을 때 목록과 요약을 다시 보면 개수와 내용이 거부 이전과 같다")
    void 추가가_거부되어도_목록과_요약은_그대로다() throws Exception {
        create("가");
        String before = listBody();

        create("").andExpect(status().isBadRequest());
        create("가".repeat(101)).andExpect(status().isBadRequest());

        assertThat(listBody()).isEqualTo(before);
    }

    @Test
    @DisplayName("AC-08-3 수정 내용이 규칙에 어긋날 때 저장하면 거부되고 기존 내용은 하나도 바뀌지 않는다")
    void 수정이_거부되어도_기존_내용은_그대로다() throws Exception {
        create("PRD 초안");
        String before = body(mockMvc.perform(get("/todos/1")));

        replace(1, "{\"title\":\"새 제목\",\"description\":\"" + "가".repeat(1001) + "\",\"completed\":true}")
                .andExpect(status().isBadRequest());
        replace(1, "{\"title\":\"새 제목\",\"dueDate\":\"abc\",\"completed\":true}")
                .andExpect(status().isBadRequest());

        assertThat(body(mockMvc.perform(get("/todos/1")))).isEqualTo(before);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "PUT|{\"title\":\"새 제목\",\"completed\":true}",
            "PATCH|{\"completed\":true}",
            "DELETE|"})
    @DisplayName("AC-12-2 한 번도 만들어진 적 없는 할 일을 가리킬 때 수정, 완료 표시, 삭제를 시도하면 각각 같은 안내가 돌아오고, 다른 할 일은 바뀌지 않는다")
    void 없는_할_일에_시도해도_다른_할_일은_그대로다(String method, String json) throws Exception {
        create("가");
        String before = listBody();

        var builder = request(HttpMethod.valueOf(method), "/todos/99");
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        mockMvc.perform(builder)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TODO_NOT_FOUND"));

        assertThat(listBody()).isEqualTo(before);
    }

    @Test
    @DisplayName("AC-14-3 요청 내용을 읽을 수 없을 때 어떤 요청이든 처리하면 거부되고 안내가 돌아오며 저장된 내용은 바뀌지 않는다")
    void 깨진_요청이_거부된_뒤에도_목록과_요약은_그대로다() throws Exception {
        create("가");
        String before = listBody();

        mockMvc.perform(post("/todos").contentType(MediaType.APPLICATION_JSON).content("{\"title\":"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/todos/1").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/todos/1").contentType(MediaType.APPLICATION_JSON).content("{\"completed\":\"true\"}"))
                .andExpect(status().isBadRequest());

        assertThat(listBody()).isEqualTo(before);
    }
}
