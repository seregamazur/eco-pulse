package com.seregamazur.pulse.reading.s3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3AsyncObjectsCollector implements Subscriber<ListObjectsV2Response> {

    private final CompletableFuture<List<S3Object>> result = new CompletableFuture<>();
    private final List<S3Object> acc = Collections.synchronizedList(new ArrayList<>());
    private volatile Subscription subscription;

    public CompletableFuture<List<S3Object>> future() {
        return result;
    }

    @Override
    public void onSubscribe(Subscription s) {
        this.subscription = s;
        s.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(ListObjectsV2Response page) {
        if (page != null && page.contents() != null) {
            acc.addAll(page.contents());
        }
    }

    @Override
    public void onError(Throwable t) {
        result.completeExceptionally(t);
        Subscription s = this.subscription;
        if (s != null) s.cancel();
    }

    @Override
    public void onComplete() {
        result.complete(acc);
    }
}
