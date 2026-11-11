package com.marvin.todo.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.marvin.todo.domain.NewTodo;
import com.marvin.todo.domain.Todo;
import com.marvin.todo.domain.TodoException;
import com.marvin.todo.domain.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), SEOUL);

    @Mock
    private TodoRepository repository;

    private TodoService service;

    @BeforeEach
    void setUp() {
        service = new TodoService(repository, CLOCK);
    }

    @Test
    @DisplayName("AC-01-1 할 일이 하나도 없을 때 제목 \"장보기\"로 추가하면 제목 \"장보기\", 설명 없음, 마감일 없음, 진행 중인 할 일이 새로 만들어진다")
    void 제목만으로_추가하면_설명_없음_마감일_없음_진행_중이다() {
        when(repository.create(new NewTodo("장보기"))).thenReturn(new Todo(1L, "장보기", null, null, false));

        TodoView view = service.create(new CreateTodoCommand("장보기"));

        assertThat(view.todo()).isEqualTo(new Todo(1L, "장보기", null, null, false));
        assertThat(view.overdue()).isFalse();
    }

    @Test
    @DisplayName("AC-01-2 같은 제목의 할 일이 이미 있을 때 같은 제목으로 추가하면 새 할 일이 별도로 만들어진다")
    void 같은_제목으로_추가해도_별도로_만들어진다() {
        when(repository.create(new NewTodo("장보기")))
                .thenReturn(new Todo(1L, "장보기", null, null, false))
                .thenReturn(new Todo(2L, "장보기", null, null, false));

        TodoView first = service.create(new CreateTodoCommand("장보기"));
        TodoView second = service.create(new CreateTodoCommand("장보기"));

        assertThat(second.todo().id()).isNotEqualTo(first.todo().id());
        verify(repository, times(2)).create(new NewTodo("장보기"));
    }

    @Test
    @DisplayName("AC-01-3 제목 앞뒤에 공백이 있을 때 추가하면 공백이 제거된 제목으로 저장된다")
    void 제목_앞뒤_공백이_제거된_제목이_저장소에_넘어간다() {
        when(repository.create(new NewTodo("장보기"))).thenReturn(new Todo(1L, "장보기", null, null, false));

        service.create(new CreateTodoCommand("  장보기\t"));

        verify(repository).create(new NewTodo("장보기"));
    }

    static Stream<String> 제목_오류가_나는_입력() {
        return Stream.of(null, "", "   ", "가".repeat(101));
    }

    @ParameterizedTest
    @MethodSource("제목_오류가_나는_입력")
    @DisplayName("AC-14-2 추가가 거부되었을 때 목록과 요약을 다시 보면 개수와 내용이 거부 이전과 같다")
    void 제목_오류로_거부되면_저장소는_호출되지_않는다(String title) {
        assertThatThrownBy(() -> service.create(new CreateTodoCommand(title)))
                .isInstanceOf(TodoException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("AC-03-1 할 일 \"가\", \"나\", \"다\"가 이 순서로 등록되어 있을 때 목록을 보면 \"가\", \"나\", \"다\" 순서로 돌아온다")
    void 목록은_저장소가_준_순서를_유지한다() {
        when(repository.findAll()).thenReturn(List.of(
                new Todo(1L, "가", null, null, false),
                new Todo(2L, "나", null, null, false),
                new Todo(3L, "다", null, null, false)));

        ListResult result = service.list(StatusFilter.ALL);

        assertThat(result.items()).extracting(view -> view.todo().title()).containsExactly("가", "나", "다");
    }

    @Test
    @DisplayName("AC-03-2 마감일이나 완료 여부가 서로 다른 할 일들이 있을 때 목록을 보면 마감일이나 완료 여부와 관계없이 등록한 순서를 유지한다")
    void 마감일이나_완료_여부와_관계없이_순서를_바꾸지_않는다() {
        when(repository.findAll()).thenReturn(List.of(
                new Todo(1L, "가", null, LocalDate.of(2026, 12, 1), true),
                new Todo(2L, "나", null, LocalDate.of(2026, 11, 1), false),
                new Todo(3L, "다", null, null, true)));

        ListResult result = service.list(StatusFilter.ALL);

        assertThat(result.items()).extracting(view -> view.todo().id()).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("AC-03-3 목록을 보면 각 할 일에 대해 제목, 설명, 마감일, 완료 여부, 기한 지남 여부가 함께 돌아온다")
    void 각_항목에_다섯_값이_담긴다() {
        Todo todo = new Todo(1L, "가", "설명", LocalDate.of(2026, 11, 12), false);
        when(repository.findAll()).thenReturn(List.of(todo));

        TodoView view = service.list(StatusFilter.ALL).items().get(0);

        assertThat(view.todo()).isEqualTo(todo);
        assertThat(view.overdue()).isFalse();
    }

    @Test
    @DisplayName("AC-09-3 마감일이 없는 할 일이 있을 때 목록을 보면 마감일 없음으로 돌아온다")
    void 마감일이_없으면_null로_돌아온다() {
        when(repository.findAll()).thenReturn(List.of(new Todo(1L, "가", null, null, false)));

        assertThat(service.list(StatusFilter.ALL).items().get(0).todo().dueDate()).isNull();
    }

    private static List<Todo> 진행_중_3개_완료_1개() {
        return List.of(
                new Todo(1L, "가", null, null, false),
                new Todo(2L, "나", null, null, false),
                new Todo(3L, "다", null, null, true),
                new Todo(4L, "라", null, null, false));
    }

    @Test
    @DisplayName("AC-04-3 진행 중 3개, 완료 1개가 있을 때 전체를 보면 4개가 모두 돌아온다")
    void 전체_보기는_모두_돌려준다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        assertThat(service.list(StatusFilter.ALL).items()).hasSize(4);
    }

    @Test
    @DisplayName("AC-04-1 진행 중 3개, 완료 1개가 있을 때 진행 중만 보면 진행 중 3개만 등록 순서대로 돌아온다")
    void 진행_중_보기는_진행_중만_순서대로_돌려준다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        assertThat(service.list(StatusFilter.ACTIVE).items())
                .extracting(view -> view.todo().title()).containsExactly("가", "나", "라");
    }

    @Test
    @DisplayName("AC-04-2 진행 중 3개, 완료 1개가 있을 때 완료만 보면 완료 1개만 돌아온다")
    void 완료_보기는_완료만_돌려준다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        assertThat(service.list(StatusFilter.DONE).items())
                .extracting(view -> view.todo().title()).containsExactly("다");
    }

    @Test
    @DisplayName("AC-04-4 완료된 할 일이 하나도 없을 때 완료만 보면 오류 없이 빈 목록이 돌아온다")
    void 완료가_없으면_완료_보기는_빈_목록이다() {
        when(repository.findAll()).thenReturn(List.of(new Todo(1L, "가", null, null, false)));

        assertThat(service.list(StatusFilter.DONE).items()).isEmpty();
    }

    @Test
    @DisplayName("AC-05-1 진행 중 3개, 완료 1개가 있을 때 요약을 보면 전체 4, 진행 중 3, 완료 1, 남은 개수 3이 돌아온다")
    void 요약은_전체_진행_중_완료_남은_개수를_센다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        assertThat(service.list(StatusFilter.ALL).summary()).isEqualTo(new Summary(4, 3, 1, 3));
    }

    @Test
    @DisplayName("AC-05-2 진행 중 3개, 완료 1개가 있을 때 완료만 보면서 요약을 함께 받으면 요약 값은 위와 같고, 보기 선택에 영향을 받지 않는다")
    void 요약은_보기_선택에_영향을_받지_않는다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        assertThat(service.list(StatusFilter.DONE).summary()).isEqualTo(new Summary(4, 3, 1, 3));
        assertThat(service.list(StatusFilter.ACTIVE).summary()).isEqualTo(new Summary(4, 3, 1, 3));
    }

    @Test
    @DisplayName("AC-05-3 할 일이 하나도 없을 때 요약을 보면 전체 0, 진행 중 0, 완료 0, 남은 개수 0이 돌아온다")
    void 할_일이_없으면_요약은_모두_0이다() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.list(StatusFilter.ALL).summary()).isEqualTo(new Summary(0, 0, 0, 0));
    }

    @Test
    @DisplayName("AC-13-1 할 일이 하나도 없을 때 목록을 보면 오류 없이 빈 목록과 모든 개수 0이 돌아온다")
    void 할_일이_없으면_빈_목록과_0_요약이_돌아온다() {
        when(repository.findAll()).thenReturn(List.of());

        ListResult result = service.list(StatusFilter.ALL);

        assertThat(result.items()).isEmpty();
        assertThat(result.summary()).isEqualTo(new Summary(0, 0, 0, 0));
    }

    @Test
    @DisplayName("목록과 요약은 한 번 조회한 결과에서 함께 계산한다 (TRD 3.1)")
    void 목록과_요약은_저장소를_한_번만_조회한다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        service.list(StatusFilter.DONE);

        verify(repository, times(1)).findAll();
    }

    private boolean 목록에서_기한_지남인가(boolean completed, LocalDate dueDate) {
        when(repository.findAll()).thenReturn(List.of(new Todo(1L, "가", null, dueDate, completed)));

        return service.list(StatusFilter.ALL).items().get(0).overdue();
    }

    @Test
    @DisplayName("AC-10-1 오늘이 2026-11-10이고, 진행 중이며 마감일이 2026-11-06인 할 일이 있을 때 목록을 보면 그 할 일은 기한 지남이다")
    void 마감일이_오늘보다_이전이면_기한_지남이다() {
        assertThat(목록에서_기한_지남인가(false, LocalDate.of(2026, 11, 6))).isTrue();
    }

    @Test
    @DisplayName("AC-10-2 오늘이 2026-11-10이고, 진행 중이며 마감일이 2026-11-10인 할 일이 있을 때 목록을 보면 기한 지남이 아니다")
    void 마감일이_오늘이면_기한_지남이_아니다() {
        assertThat(목록에서_기한_지남인가(false, LocalDate.of(2026, 11, 10))).isFalse();
    }

    @Test
    @DisplayName("AC-10-3 오늘이 2026-11-10이고, 진행 중이며 마감일이 2026-11-12인 할 일이 있을 때 목록을 보면 기한 지남이 아니다")
    void 마감일이_오늘보다_이후면_기한_지남이_아니다() {
        assertThat(목록에서_기한_지남인가(false, LocalDate.of(2026, 11, 12))).isFalse();
    }

    @Test
    @DisplayName("AC-10-4 오늘이 2026-11-10이고, 완료되었으며 마감일이 2026-11-09인 할 일이 있을 때 목록을 보면 기한 지남이 아니다")
    void 완료된_할_일은_기한_지남이_아니다() {
        assertThat(목록에서_기한_지남인가(true, LocalDate.of(2026, 11, 9))).isFalse();
    }

    @Test
    @DisplayName("AC-10-6 한국 시간으로 2026-11-10 00:30(세계 표준시로는 11-09)일 때 마감일 2026-11-09인 진행 중 할 일의 기한 지남을 판정하면 오늘은 2026-11-10으로 계산되어 기한 지남이다")
    void 한국_날짜_기준으로_오늘을_계산한다() {
        Clock lateNightUtc = Clock.fixed(Instant.parse("2026-11-09T15:30:00Z"), SEOUL);
        TodoService seoulService = new TodoService(repository, lateNightUtc);
        when(repository.findAll()).thenReturn(List.of(new Todo(1L, "가", null, LocalDate.of(2026, 11, 9), false)));

        assertThat(seoulService.list(StatusFilter.ALL).items().get(0).overdue()).isTrue();
    }

    @Test
    @DisplayName("AC-03-3 목록을 보면 각 할 일에 대해 제목, 설명, 마감일, 완료 여부, 기한 지남 여부가 함께 돌아온다")
    void 섞인_목록에서_항목마다_기한_지남_여부가_맞다() {
        when(repository.findAll()).thenReturn(List.of(
                new Todo(1L, "지남", null, LocalDate.of(2026, 11, 6), false),
                new Todo(2L, "오늘", null, LocalDate.of(2026, 11, 10), false),
                new Todo(3L, "완료", null, LocalDate.of(2026, 11, 9), true),
                new Todo(4L, "없음", null, null, false)));

        assertThat(service.list(StatusFilter.ALL).items())
                .extracting(TodoView::overdue).containsExactly(true, false, false, false);
    }

    @Test
    @DisplayName("목록의 모든 항목은 같은 \"오늘\"로 판정한다 (DATA_MODEL 3.1)")
    void 오늘은_요청당_한_번만_계산한다() {
        AtomicInteger instantCalls = new AtomicInteger();
        Clock countingClock = new Clock() {
            @Override
            public ZoneId getZone() {
                return SEOUL;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                instantCalls.incrementAndGet();
                return CLOCK.instant();
            }
        };
        TodoService countingService = new TodoService(repository, countingClock);
        when(repository.findAll()).thenReturn(List.of(
                new Todo(1L, "가", null, LocalDate.of(2026, 11, 6), false),
                new Todo(2L, "나", null, LocalDate.of(2026, 11, 7), false),
                new Todo(3L, "다", null, LocalDate.of(2026, 11, 8), false)));

        countingService.list(StatusFilter.ALL);

        assertThat(instantCalls).hasValue(1);
    }
}
