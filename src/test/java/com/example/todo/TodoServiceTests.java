package com.example.todo;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TodoServiceTests {
    private final TodoService service = new TodoService();

    @Test
    void startsEmptyAndAddsTrimmedTitlesInInsertionOrder() {
        assertThat(service.findAll()).isEmpty();
        Todo first = service.add("  Javaを学ぶ　");
        Todo second = service.add("Springを学ぶ");

        assertThat(first.title()).isEqualTo("Javaを学ぶ");
        assertThat(first.completed()).isFalse();
        assertThat(second.id()).isGreaterThan(first.id());
        assertThat(service.findAll()).containsExactly(first, second);
    }

    @Test
    void togglesBothWaysWithoutChangingOrderOrOldSnapshot() {
        Todo first = service.add("Java");
        Todo second = service.add("Spring");
        var snapshot = service.findAll();

        service.toggle(first.id());
        assertThat(service.findAll()).containsExactly(new Todo(first.id(), "Java", true), second);
        assertThat(snapshot).containsExactly(first, second);
        service.toggle(first.id());
        assertThat(service.findAll()).containsExactly(first, second);
    }

    @Test
    void deletesOnlyTheSpecifiedTodoAndDoesNotReuseIds() {
        Todo first = service.add("Java");
        Todo second = service.add("Spring");
        service.delete(first.id());

        assertThat(service.findAll()).containsExactly(second);
        assertThat(service.add("Maven").id()).isGreaterThan(second.id());
    }

    @Test
    void rejectsNullEmptyAndWhitespaceTitles() {
        for (String title : new String[] { null, "", " \t\n", "　" }) {
            assertThatThrownBy(() -> service.add(title))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("タイトルを入力してください。");
        }
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void rejectsMissingIdsWithoutChangingExistingTodos() {
        Todo todo = service.add("Java");
        assertThatThrownBy(() -> service.toggle(999)).isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> service.delete(999)).isInstanceOf(NoSuchElementException.class);
        assertThat(service.findAll()).containsExactly(todo);
    }
}
