package com.example.todo;

/**
 * 一件のTODOを表す、不変のデータクラス。
 *
 * @param id TODOの識別子
 * @param title タイトル
 * @param completed 完了している場合はtrue
 */
public record Todo(long id, String title, boolean completed) {
    /**
     * IDとタイトルを保ち、完了状態を反転した新しいTODOを返す。
     *
     * @return 完了状態を反転したTODO
     */
    public Todo toggle() {
        return new Todo(id, title, !completed);
    }
}
