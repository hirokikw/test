package com.example.greeting.domain;

/**
 * あいさつの処理状態。DBにはこの列挙名（PENDING / COMPLETED）を文字列で保存する。
 */
public enum GreetingStatus {
    PENDING,
    COMPLETED
}
