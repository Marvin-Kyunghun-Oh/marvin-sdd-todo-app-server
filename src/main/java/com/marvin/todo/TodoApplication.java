package com.marvin.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;

// 스프링 기본 오류 처리기는 `/error` 주소를 직접 열어 두고 약속된 오류 모양과 다른 응답을 낸다 (API_SPEC 1.1, TD-53).
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class TodoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoApplication.class, args);
    }
}
