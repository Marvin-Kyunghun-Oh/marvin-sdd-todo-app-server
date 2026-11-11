package com.marvin.todo;

import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

// MockMvc는 톰캣을 거치지 않아 쿼리 문자열의 `%` 인코딩 오류를 재현하지 못하고, URI 클래스는 깨진 주소를 보내지 못한다.
// 그래서 실제 포트에 원시 HTTP 요청을 직접 보낸다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealServerTest {

    @LocalServerPort
    private int port;

    private String get(String pathAndQuery) throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            String request = "GET " + pathAndQuery + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            try (InputStream in = socket.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    @Test
    @DisplayName("AC-04-5 할 일이 있을 때 전체, 진행 중, 완료 외의 보기 구분(`%` 인코딩이 깨진 값)으로 목록을 요청하면 거부되고 \"보기 구분이 올바르지 않아요. 전체, 진행 중, 완료 중에서 골라 주세요.\"라는 안내가 돌아온다 (TD-53)")
    void 인코딩이_깨진_status는_보기_구분_오류다() throws Exception {
        String response = get("/todos?status=%zz");

        assertThat(response).startsWith("HTTP/1.1 400");
        assertThat(response).contains("INVALID_STATUS_FILTER", "보기 구분이 올바르지 않아요. 전체, 진행 중, 완료 중에서 골라 주세요.");
    }

    @Test
    @DisplayName("`status=all`을 명시하면 전체 목록이 돌아온다 (API_SPEC 2.2)")
    void status_all을_명시하면_전체_목록이다() throws Exception {
        String response = get("/todos?status=all");

        assertThat(response).startsWith("HTTP/1.1 200");
        assertThat(response).contains("\"items\":[]", "\"total\":0");
    }
}
