package com.itheima.aiupweb.config;

import com.itheima.aiupweb.ai.AiProperties;
import com.itheima.aiupweb.ai.FixedDimensionEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AiConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean(name = "aiExecutor")
    public ThreadPoolTaskExecutor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ai-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return aiExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("异步任务执行失败: {}", method.getName(), throwable);
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(OpenAiEmbeddingModel delegate, AiProperties properties) {
        return new FixedDimensionEmbeddingModel(delegate, properties.getEmbeddingDimensions());
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public List<RedisVectorStore.MetadataField> redisMetadataFields() {
        return List.of(
                RedisVectorStore.MetadataField.tag("postId"),
                RedisVectorStore.MetadataField.tag("category"),
                RedisVectorStore.MetadataField.tag("author"),
                RedisVectorStore.MetadataField.tag("sourceType")
        );
    }
}
