package com.itheima.aiupweb.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class IndexRecoveryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndexRecoveryRunner.class);

    private final AiProperties properties;
    private final AiAvailability availability;
    private final IndexingService indexingService;

    public IndexRecoveryRunner(AiProperties properties, AiAvailability availability,
                               IndexingService indexingService) {
        this.properties = properties;
        this.availability = availability;
        this.indexingService = indexingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isRecoverOnStartup() || !properties.isIndexingEnabled() || !availability.isAvailable()) {
            return;
        }
        var postIds = indexingService.recoverablePostIds();
        if (!postIds.isEmpty()) {
            log.info("检测到 {} 个待恢复索引任务", postIds.size());
            postIds.forEach(indexingService::reindexPostAsync);
        }
    }
}
