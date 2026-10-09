package com.example.todo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 実際のThymeleafテンプレートも描画してHTTP処理を確認する。
@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerTests {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private TodoService service;

    @BeforeEach
    void clearTodos() {
        service.findAll().forEach(todo -> service.delete(todo.id()));
    }

    @Test
    void redirectsHomeAndRendersEmptyJapanesePageAndStylesheet() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos"));
        mvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(view().name("todos"))
                .andExpect(content().string(containsString("まだTODOがありません")))
                .andExpect(content().string(containsString("action=\"/todos\"")));
        mvc.perform(get("/style.css")).andExpect(status().isOk());
    }

    @Test
    void addsTogglesBackAndDeletesThroughForms() throws Exception {
        mvc.perform(post("/todos").param("title", "  Mavenを学ぶ  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/todos"));
        Todo todo = service.findAll().getFirst();
        assertThat(todo.title()).isEqualTo("Mavenを学ぶ");
        mvc.perform(get("/todos"))
                .andExpect(content().string(containsString("Mavenを学ぶ")))
                .andExpect(content().string(containsString("/todos/" + todo.id() + "/toggle")))
                .andExpect(content().string(containsString("/todos/" + todo.id() + "/delete")));

        mvc.perform(post("/todos/{id}/toggle", todo.id()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/todos"));
        assertThat(service.findAll().getFirst().completed()).isTrue();
        mvc.perform(get("/todos"))
                .andExpect(content().string(containsString("未完了に戻す")));
        mvc.perform(post("/todos/{id}/toggle", todo.id()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/todos"));
        assertThat(service.findAll().getFirst().completed()).isFalse();

        mvc.perform(post("/todos/{id}/delete", todo.id()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/todos"));
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void showsValidationErrorForBlankOrMissingTitleWithoutLosingExistingTodos() throws Exception {
        service.add("既存のTODO");
        mvc.perform(post("/todos").param("title", "　 "))
                .andExpect(status().isOk()).andExpect(view().name("todos"))
                .andExpect(content().string(containsString("タイトルを入力してください。")))
                .andExpect(content().string(containsString("既存のTODO")));
        mvc.perform(post("/todos"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "タイトルを入力してください。"));
        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void returnsJapanese404ForMissingTodo() throws Exception {
        for (String action : new String[] { "toggle", "delete" }) {
            mvc.perform(post("/todos/999/" + action))
                    .andExpect(status().isNotFound())
                    .andExpect(view().name("not-found"))
                    .andExpect(content().string(containsString("指定されたTODOは見つかりません。")));
        }
    }

    @Test
    void rendersUserInputAsTextInsteadOfHtml() throws Exception {
        service.add("<script>alert('test')</script>");
        mvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>"))));
    }
}
