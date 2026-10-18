package com.example.greeting.api.dto;

import java.time.format.DateTimeFormatter;

import com.example.greeting.domain.Greeting;

/**
 * 登録・一覧APIのレスポンス。日時は日本時間のオフセット付き文字列にする。
 */
public record GreetingResponse(
        Long id,
        String name,
        String status,
        String message,
        String createdAt,
        String processedAt) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static GreetingResponse from(final Greeting greeting) {
        String processedAt = greeting.getProcessedAt() == null
                ? null
                : FORMATTER.format(greeting.getProcessedAt());
        return new GreetingResponse(
                greeting.getId(),
                greeting.getName(),
                greeting.getStatus().name(),
                greeting.getMessage(),
                FORMATTER.format(greeting.getCreatedAt()),
                processedAt);
    }
}
