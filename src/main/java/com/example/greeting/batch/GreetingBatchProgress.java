package com.example.greeting.batch;

import org.springframework.stereotype.Component;

/**
 * 今回のバッチで更新した件数をTaskletから起動側へ渡す。
 * 起動処理が同時に1つだけになるため、この値は実行のたびにリセットして使う。
 */
@Component
public class GreetingBatchProgress {

    private volatile int processedCount;

    public void reset() {
        this.processedCount = 0;
    }

    public void setProcessedCount(final int processedCount) {
        this.processedCount = processedCount;
    }

    public int getProcessedCount() {
        return processedCount;
    }
}
