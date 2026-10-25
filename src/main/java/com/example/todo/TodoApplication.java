package com.example.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** TODOアプリの設定と起動を担う。 */
@SpringBootApplication
public class TodoApplication {

	/**
	 * Spring Bootを起動し、TODOアプリを実行する。
	 *
	 * @param args 起動時のコマンドライン引数
	 */
	public static void main(String[] args) {
		SpringApplication.run(TodoApplication.class, args);
	}

}
