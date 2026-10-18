package com.example.greeting.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * あいさつ1件。画面から登録され、バッチがメッセージを生成する。
 */
@Entity
@Table(name = "greetings")
public class Greeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GreetingStatus status;

    @Column(length = 120)
    private String message;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime processedAt;

    protected Greeting() {
        // JPAが使う
    }

    public Greeting(final String name, final OffsetDateTime createdAt) {
        this.name = name;
        this.status = GreetingStatus.PENDING;
        this.createdAt = createdAt;
    }

    /**
     * 未処理のときだけメッセージと処理日時を設定する。処理済みは変更しない。
     */
    public void markCompleted(final String message, final OffsetDateTime processedAt) {
        if (this.status == GreetingStatus.COMPLETED) {
            return;
        }
        this.status = GreetingStatus.COMPLETED;
        this.message = message;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public GreetingStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
