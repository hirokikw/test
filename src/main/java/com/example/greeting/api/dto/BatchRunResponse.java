package com.example.greeting.api.dto;

/**
 * バッチ実行APIのレスポンス。processedCountは今回更新した件数。
 */
public record BatchRunResponse(long jobExecutionId, String status, int processedCount) {
}
