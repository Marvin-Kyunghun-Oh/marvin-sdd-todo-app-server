package com.marvin.todo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SampleProfileTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("local")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    class LocalProfile {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("`local` 프로파일에서는 샘플이 채워진다 (TRD 9장)")
        void 샘플이_채워진다() throws Exception {
            mockMvc.perform(get("/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.summary.total").value(5))
                    .andExpect(jsonPath("$.items[?(@.overdue == true)]").isNotEmpty())
                    .andExpect(jsonPath("$.items[?(@.completed == true)]").isNotEmpty());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    class NoProfile {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("프로파일이 없을 때는 할 일이 비어 있다 (TRD 9장)")
        void 비어_있다() throws Exception {
            mockMvc.perform(get("/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isEmpty())
                    .andExpect(jsonPath("$.summary.total").value(0));
        }
    }
}
