package com.flightstats.hub.util;

import com.flightstats.hub.config.S3Property;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Slf4j
public class ChunkOutputStream extends OutputStream {

    private static final int MEGABYTES = 1024 * 1024;

    private List<ListenableFuture<String>> futures = new ArrayList<>();
    private int count = 1;
    private Chunk chunk;

    private ListeningExecutorService service;
    private Function<Chunk, String> chunkFunction;
    private int maxChunkInMB;


    public ChunkOutputStream(int threads, int maxChunkInMB, Function<Chunk, String> chunkFunction) {
        this.maxChunkInMB = maxChunkInMB;
        this.chunkFunction = chunkFunction;

        this.chunk = new Chunk(count, getSize(count));
        service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(threads));
        log.info("creating ChunkOutputStream with {} threads", threads);
    }

    public void write(int b) throws IOException {
        if (!chunk.add(b)) {
            sendChunk(chunk);
            count++;
            chunk = new Chunk(count, getSize(count));
            chunk.add(b);
        }
    }

    private void sendChunk(Chunk chunk) {
        futures.add(service.submit(() -> chunkFunction.apply(chunk)));
    }

    @Override
    public void close() throws IOException {
        if (chunk.hasData()) {
            sendChunk(chunk);
        }
        ListenableFuture<String> allFutures = Futures.whenAllSucceed(futures).call(() -> "ok", MoreExecutors.directExecutor());
        try {
            allFutures.get();
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            service.shutdown();
        }
    }

     int getSize(int count) {
        int chunkMB = (Math.floorDiv(count, 3) + 1) * 5;
        return Math.min(chunkMB, maxChunkInMB) * MEGABYTES;
    }
}
