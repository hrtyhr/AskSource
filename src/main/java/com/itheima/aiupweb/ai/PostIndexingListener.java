package com.itheima.aiupweb.ai;

import com.itheima.aiupweb.forum.event.PostContentChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PostIndexingListener {

    private final AiProperties properties;
    private final IndexingService indexingService;

    public PostIndexingListener(AiProperties properties, IndexingService indexingService) {
        this.properties = properties;
        this.indexingService = indexingService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPostContentChanged(PostContentChangedEvent event) {
        if (properties.isIndexingEnabled() && event.postId() != null) {
            indexingService.reindexPostAsync(event.postId());
        }
    }
}
