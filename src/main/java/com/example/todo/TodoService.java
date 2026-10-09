package com.example.todo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

/** メモリ上でTODOを管理する。再起動すると保存内容は消える。 */
@Service
public class TodoService {
    // LinkedHashMapは追加順を保持する。複数リクエストの操作はsynchronizedで保護する。
    private final Map<Long, Todo> todos = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized List<Todo> findAll() {
        return List.copyOf(todos.values());
    }

    public synchronized Todo add(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("タイトルを入力してください。");
        }
        Todo todo = new Todo(nextId++, title.strip(), false);
        todos.put(todo.id(), todo);
        return todo;
    }

    public synchronized void toggle(long id) {
        Todo todo = requireTodo(id);
        todos.put(id, todo.toggle());
    }

    public synchronized void delete(long id) {
        requireTodo(id);
        todos.remove(id);
    }

    private Todo requireTodo(long id) {
        Todo todo = todos.get(id);
        if (todo == null) {
            throw new NoSuchElementException("TODOが見つかりません。");
        }
        return todo;
    }
}
