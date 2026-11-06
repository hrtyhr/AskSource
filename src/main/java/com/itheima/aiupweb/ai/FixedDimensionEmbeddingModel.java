package com.itheima.aiupweb.ai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

public class FixedDimensionEmbeddingModel implements EmbeddingModel {

    private final EmbeddingModel delegate;
    private final int dimensions;

    public FixedDimensionEmbeddingModel(EmbeddingModel delegate, int dimensions) {
        this.delegate = delegate;
        this.dimensions = dimensions;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        return delegate.call(request);
    }

    @Override
    public float[] embed(Document document) {
        return delegate.embed(document);
    }

    @Override
    public String getEmbeddingContent(Document document) {
        return delegate.getEmbeddingContent(document);
    }

    @Override
    public int dimensions() {
        return dimensions;
    }
}
