package com.example.greeting.batch;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.example.greeting.service.GreetingService;

/**
 * 少量データ向けのTasklet。1回の実行で未処理レコードをまとめて更新する。
 */
@Component
public class GreetingTasklet implements Tasklet {

    private final GreetingService greetingService;
    private final GreetingBatchProgress progress;

    public GreetingTasklet(final GreetingService greetingService, final GreetingBatchProgress progress) {
        this.greetingService = greetingService;
        this.progress = progress;
    }

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) {
        int processedCount = greetingService.completePending();
        progress.setProcessedCount(processedCount);
        // FINISHEDを返すと、このStepは繰り返さず終わる
        return RepeatStatus.FINISHED;
    }
}
