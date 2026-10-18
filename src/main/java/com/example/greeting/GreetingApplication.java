package com.example.greeting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 画面、REST API、バッチを1つのアプリにまとめた起動クラス。
 * {@code @SpringBootApplication} がコンポーネントスキャンと自動設定を有効にする。
 */
@SpringBootApplication
public class GreetingApplication {

    public static void main(final String[] args) {
        SpringApplication.run(GreetingApplication.class, args);
    }
}
