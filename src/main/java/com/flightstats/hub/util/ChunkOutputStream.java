package com.flightstats.hub.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class ChunkOutputStream extends OutputStream {
    private static final Logger logger = LoggerFactory.getLogger(ChunkOutputStream.class);

    private ListeningExecutorService service;
    private List<ListenableFuture<String>> futures = new ArrayList<>();
    private int count = 1;
    private Chunk chunk = new Chunk(count, ChunkStrategy.getSize(count));
    private Function<Chunk, String> chunkFunction;

    public ChunkOutputStream(int threads, Function<Chunk, String> chunkFunction) {
        this.chunkFunction = chunkFunction;
        service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(threads));
        logger.info("creating ChunkOutputStream with {} threads", threads);
    }

    public void write(int b) throws IOException {
        if (!chunk.add(b)) {
            sendChunk(chunk);
            count++;
            chunk = new Chunk(count, ChunkStrategy.getSize(count));
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
        ListenableFuture<String> allFutures = Futures.whenAllSucceed(futures).call(() -> "ok");
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
}
