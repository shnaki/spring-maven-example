package com.example.todo;

/** 一件のTODOを表す、不変のデータクラス。 */
public record Todo(long id, String title, boolean completed) {
    public Todo toggle() {
        return new Todo(id, title, !completed);
    }
}
