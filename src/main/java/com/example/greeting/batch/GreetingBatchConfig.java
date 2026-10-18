package com.example.greeting.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * あいさつ生成バッチのJobとStep。
 * {@code @EnableBatchProcessing} は付けない。付けるとBootの自動設定が外れ、
 * メタデータテーブルの作成も行われなくなる。
 */
@Configuration
public class GreetingBatchConfig {

    public static final String JOB_NAME = "greetingJob";

    @Bean
    public Job greetingJob(final JobRepository jobRepository, final Step greetingStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(greetingStep)
                .build();
    }

    @Bean
    public Step greetingStep(final JobRepository jobRepository,
                             final PlatformTransactionManager transactionManager,
                             final GreetingTasklet greetingTasklet) {
        // Taskletの実行は、ここで渡したトランザクションマネージャのトランザクションで囲まれる
        return new StepBuilder("greetingStep", jobRepository)
                .tasklet(greetingTasklet, transactionManager)
                .build();
    }
}
