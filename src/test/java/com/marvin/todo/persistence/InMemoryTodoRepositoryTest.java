package com.marvin.todo.persistence;

import com.marvin.todo.domain.NewTodo;
import com.marvin.todo.domain.Todo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryTodoRepositoryTest {

    private InMemoryTodoRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTodoRepository();
    }

    private Todo create(String title) {
        return repository.create(NewTodo.of(title));
    }

    @Test
    @DisplayName("AC-01-1 할 일이 하나도 없을 때 제목 \"장보기\"로 추가하면 제목 \"장보기\", 설명 없음, 마감일 없음, 진행 중인 할 일이 새로 만들어진다")
    void 만들면_설명_없음_마감일_없음_진행_중으로_저장된다() {
        Todo created = create("장보기");

        assertThat(created.title()).isEqualTo("장보기");
        assertThat(created.description()).isNull();
        assertThat(created.dueDate()).isNull();
        assertThat(created.completed()).isFalse();
    }

    @Test
    @DisplayName("저장소는 새 번호를 1부터 하나씩 늘려 발급한다 (DATA_MODEL 6)")
    void 번호는_1부터_하나씩_늘어난다() {
        assertThat(create("가").id()).isEqualTo(1L);
        assertThat(create("나").id()).isEqualTo(2L);
        assertThat(create("다").id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("AC-03-1 할 일 \"가\", \"나\", \"다\"가 이 순서로 등록되어 있을 때 목록을 보면 \"가\", \"나\", \"다\" 순서로 돌아온다")
    void 목록은_등록한_순서로_돌아온다() {
        create("가");
        create("나");
        create("다");

        assertThat(repository.findAll()).extracting(Todo::title).containsExactly("가", "나", "다");
    }

    @Test
    @DisplayName("목록은 변경할 수 없는 스냅샷이다 (DATA_MODEL 6)")
    void 돌려받은_목록은_변경할_수_없다() {
        create("가");

        assertThatThrownBy(() -> repository.findAll().add(new Todo(9L, "나", null, null, false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("할 일이 하나도 없을 때 목록은 빈 목록이다 (DATA_MODEL 6)")
    void 할_일이_없으면_빈_목록이다() {
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("번호로 한 건을 찾고, 없는 번호는 빈 값이다 (DATA_MODEL 6)")
    void 번호로_찾고_없는_번호는_빈_값이다() {
        Todo created = create("가");

        assertThat(repository.findById(created.id())).contains(created);
        assertThat(repository.findById(999L)).isEmpty();
    }

    @Test
    @DisplayName("AC-12-2 한 번도 만들어진 적 없는 할 일을 가리킬 때 수정을 시도하면 다른 할 일은 바뀌지 않는다")
    void 없는_번호를_수정하면_빈_값이고_다른_할_일은_그대로다() {
        Todo existing = create("가");

        assertThat(repository.update(999L, todo -> new Todo(todo.id(), "바뀜", null, null, true))).isEmpty();
        assertThat(repository.findAll()).containsExactly(existing);
    }

    @Test
    @DisplayName("수정은 대상을 새 값으로 교체하고 새 값을 돌려준다. 돌려준 값의 번호가 다르면 거부한다 (DATA_MODEL 6)")
    void 수정은_새_값으로_교체하고_번호가_다르면_거부한다() {
        Todo created = create("가");

        Optional<Todo> updated = repository.update(created.id(),
                todo -> new Todo(todo.id(), "나", "설명", null, true));

        assertThat(updated).contains(new Todo(created.id(), "나", "설명", null, true));
        assertThat(repository.findById(created.id())).isEqualTo(updated);
        assertThatThrownBy(() -> repository.update(created.id(), todo -> new Todo(todo.id() + 1, "다", null, null, false)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.findById(created.id())).isEqualTo(updated);
    }

    @Test
    @DisplayName("AC-11-2 방금 삭제한 할 일을 가리킬 때 다시 열면 찾을 수 없다")
    void 삭제하면_true이고_이후_찾을_수_없으며_없는_번호는_false이다() {
        Todo created = create("가");

        assertThat(repository.deleteById(created.id())).isTrue();
        assertThat(repository.findById(created.id())).isEmpty();
        assertThat(repository.deleteById(created.id())).isFalse();
    }

    @Test
    @DisplayName("AC-12-1 한 번도 만들어진 적 없는 할 일을 가리킬 때 열면 찾을 수 없다 (삭제된 번호는 다시 발급하지 않음)")
    void 삭제된_번호는_다시_발급되지_않는다() {
        Todo first = create("가");
        Todo last = create("나");
        repository.deleteById(last.id());

        Todo next = create("다");

        assertThat(next.id()).isGreaterThan(last.id());
        assertThat(repository.findById(last.id())).isEmpty();
        assertThat(repository.findById(first.id())).contains(first);
    }

    @Test
    @DisplayName("여러 스레드가 동시에 만들어도 번호가 겹치지 않는다 (TRD 3.1)")
    void 동시에_만들어도_번호가_겹치지_않는다() throws Exception {
        int threads = 16;
        int perThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<Long>>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                List<Long> ids = new ArrayList<>();
                for (int j = 0; j < perThread; j++) {
                    ids.add(create("동시").id());
                }
                return ids;
            }));
        }
        start.countDown();
        Set<Long> allIds = new HashSet<>();
        for (Future<List<Long>> future : futures) {
            allIds.addAll(future.get());
        }
        executor.shutdown();

        assertThat(allIds).hasSize(threads * perThread);
        assertThat(repository.findAll()).hasSize(threads * perThread);
    }

    @Test
    @DisplayName("여러 스레드가 같은 할 일을 동시에 수정해도 변경이 유실되지 않는다 (TRD 3.1)")
    void 동시에_수정해도_변경이_유실되지_않는다() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        Todo created = create("동시 수정");
        repository.update(created.id(), todo -> new Todo(todo.id(), todo.title(), null, startDate, false));
        int threads = 16;
        int perThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                for (int j = 0; j < perThread; j++) {
                    repository.update(created.id(),
                            todo -> new Todo(todo.id(), todo.title(), null, todo.dueDate().plusDays(1), false));
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        assertThat(repository.findById(created.id()).orElseThrow().dueDate())
                .isEqualTo(startDate.plusDays((long) threads * perThread));
    }
}
