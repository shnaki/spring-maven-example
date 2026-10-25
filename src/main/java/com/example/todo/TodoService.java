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

    /**
     * TODOを追加順で取得する。取得後の追加・更新・削除は返した一覧に反映されない。
     *
     * @return 変更できないTODO一覧。未登録の場合は空の一覧
     */
    public synchronized List<Todo> findAll() {
        return List.copyOf(todos.values());
    }

    /**
     * タイトルの前後の空白を除去し、新しいIDを持つ未完了のTODOを追加する。
     *
     * @param title 登録するタイトル
     * @return 登録したTODO
     * @throws IllegalArgumentException タイトルがnull、空文字、または空白のみの場合
     */
    public synchronized Todo add(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("タイトルを入力してください。");
        }
        Todo todo = new Todo(nextId++, title.strip(), false);
        todos.put(todo.id(), todo);
        return todo;
    }

    /**
     * 指定したTODOの完了状態を反転する。一覧の追加順は変わらない。
     *
     * @param id 対象のTODOの識別子
     * @throws NoSuchElementException 指定したIDのTODOが存在しない場合
     */
    public synchronized void toggle(long id) {
        Todo todo = requireTodo(id);
        todos.put(id, todo.toggle());
    }

    /**
     * 指定したTODOを削除する。削除したIDは再利用しない。
     *
     * @param id 削除するTODOの識別子
     * @throws NoSuchElementException 指定したIDのTODOが存在しない場合
     */
    public synchronized void delete(long id) {
        requireTodo(id);
        todos.remove(id);
    }

    /**
     * 指定したIDのTODOを取得し、存在しない場合は例外を送出する。
     * 呼び出し元でこのインスタンスのロックを取得しておく必要がある。
     *
     * @param id 取得するTODOの識別子
     * @return 対象のTODO
     * @throws NoSuchElementException 指定したIDのTODOが存在しない場合
     */
    private Todo requireTodo(long id) {
        Todo todo = todos.get(id);
        if (todo == null) {
            throw new NoSuchElementException("TODOが見つかりません。");
        }
        return todo;
    }
}
