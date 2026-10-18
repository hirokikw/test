package com.example.greeting.batch;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Service;

import com.example.greeting.api.dto.BatchRunResponse;

/**
 * 画面やAPIからの依頼でJobを同期実行する。
 */
@Service
public class GreetingBatchLauncher {

    private static final Logger log = LoggerFactory.getLogger(GreetingBatchLauncher.class);

    private final JobOperator jobOperator;
    private final Job greetingJob;
    private final GreetingBatchProgress progress;
    private final ReentrantLock lock = new ReentrantLock();

    public GreetingBatchLauncher(final JobOperator jobOperator,
                                 final Job greetingJob,
                                 final GreetingBatchProgress progress) {
        this.jobOperator = jobOperator;
        this.greetingJob = greetingJob;
        this.progress = progress;
    }

    /**
     * Jobをその場で最後まで実行し、Spring Batchの終了状態を見て結果を返す。
     * 実行のたびに異なるJobParametersを付け、完了済みJobInstanceの再実行エラーを避ける。
     */
    public BatchRunResponse launch() {
        // 同じバッチの並行実行を防ぐ。取れなければ実行中なので409にする
        if (!lock.tryLock()) {
            throw new BatchAlreadyRunningException("バッチは実行中です。完了してから再度実行してください。");
        }
        try {
            progress.reset();
            JobParameters parameters = new JobParametersBuilder()
                    .addString("run.id", UUID.randomUUID().toString())
                    .toJobParameters();
            JobExecution execution = start(parameters);
            if (execution.getStatus() != BatchStatus.COMPLETED) {
                log.error("バッチが失敗しました。jobExecutionId={}, status={}, exitStatus={}",
                        execution.getId(), execution.getStatus(), execution.getExitStatus());
                throw new BatchFailedException("あいさつメッセージの生成に失敗しました。");
            }
            return new BatchRunResponse(
                    execution.getId(),
                    execution.getStatus().name(),
                    progress.getProcessedCount());
        } finally {
            lock.unlock();
        }
    }

    private JobExecution start(final JobParameters parameters) {
        try {
            // 標準のJobOperatorは同期実行のため、このメソッドはJob完了後に戻る
            return jobOperator.start(greetingJob, parameters);
        } catch (RuntimeException ex) {
            log.error("バッチの起動に失敗しました", ex);
            throw new BatchFailedException("バッチの起動に失敗しました。", ex);
        } catch (Exception ex) {
            log.error("バッチの起動に失敗しました", ex);
            throw new BatchFailedException("バッチの起動に失敗しました。", ex);
        }
    }
}
