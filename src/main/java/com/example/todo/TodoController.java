package com.example.todo;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/** TODOの画面表示とフォーム操作を受け付ける。 */
@Controller
public class TodoController {
    private final TodoService todoService;

    /**
     * Springから渡されるServiceを使ってControllerを作成する。
     *
     * @param todoService TODOを管理するService
     */
    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * トップページへのアクセスをTODO一覧へ転送する。
     *
     * @return TODO一覧へのリダイレクト指定
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/todos";
    }

    /**
     * 追加順のTODO一覧をModelに設定し、一覧画面を表示する。
     *
     * @param model 画面に渡すデータ
     * @return 一覧画面のテンプレート名
     */
    @GetMapping("/todos")
    public String list(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "todos";
    }

    /**
     * TODOを登録し、一覧へ転送する。入力が不正な場合は、タイトルとエラーを
     * Modelに設定して一覧画面を再表示する。
     *
     * @param title 入力されたタイトル。リクエストで省略された場合は空文字
     * @param model 入力エラー時に画面へ渡すデータ
     * @return 登録成功時は一覧へのリダイレクト指定、入力エラー時は一覧のテンプレート名
     */
    @PostMapping("/todos")
    public String add(@RequestParam(defaultValue = "") String title, Model model) {
        try {
            todoService.add(title);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("title", title);
            return list(model);
        }
        // POSTの後にGETへ誘導し、画面の再読込による二重登録を防ぐ。
        return "redirect:/todos";
    }

    /**
     * TODOの完了状態を反転し、一覧へ転送する。
     *
     * @param id 対象のTODOの識別子
     * @return TODO一覧へのリダイレクト指定
     * @throws NoSuchElementException 指定したIDのTODOが存在しない場合
     */
    @PostMapping("/todos/{id}/toggle")
    public String toggle(@PathVariable long id) {
        todoService.toggle(id);
        return "redirect:/todos";
    }

    /**
     * TODOを削除し、一覧へ転送する。
     *
     * @param id 削除するTODOの識別子
     * @return TODO一覧へのリダイレクト指定
     * @throws NoSuchElementException 指定したIDのTODOが存在しない場合
     */
    @PostMapping("/todos/{id}/delete")
    public String delete(@PathVariable long id) {
        todoService.delete(id);
        return "redirect:/todos";
    }

    /**
     * TODOが見つからない場合の例外を処理し、日本語の案内画面をHTTP 404で返す。
     *
     * @param model 案内メッセージを渡すデータ
     * @return 対象が見つからない場合のテンプレート名
     */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        model.addAttribute("message", "指定されたTODOは見つかりません。");
        return "not-found";
    }
}
