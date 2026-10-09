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

@Controller
public class TodoController {
    private final TodoService todoService;

    // SpringがServiceを渡す（コンストラクターによる依存性注入）。
    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/todos";
    }

    @GetMapping("/todos")
    public String list(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "todos";
    }

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

    @PostMapping("/todos/{id}/toggle")
    public String toggle(@PathVariable long id) {
        todoService.toggle(id);
        return "redirect:/todos";
    }

    @PostMapping("/todos/{id}/delete")
    public String delete(@PathVariable long id) {
        todoService.delete(id);
        return "redirect:/todos";
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        model.addAttribute("message", "指定されたTODOは見つかりません。");
        return "not-found";
    }
}
