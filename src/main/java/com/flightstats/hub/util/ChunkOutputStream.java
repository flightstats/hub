package com.flightstats.hub.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class ChunkOutputStream extends OutputStream {

    //todo - gfm - evaluate 3
    private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(3));
    private List<ListenableFuture<String>> futures = new ArrayList<>();
    private int count = 1;
    private Chunk chunk = new Chunk(count);
    private Function<Chunk, String> chunkFunction;

    public ChunkOutputStream(Function<Chunk, String> chunkFunction) {
        this.chunkFunction = chunkFunction;
    }

    public void write(int b) throws IOException {
        if (!chunk.add(b)) {
            sendChunk();
            count++;
            //todo - gfm - use a Strategy to control the rate that Chunk changes
            chunk = new Chunk(count);
            chunk.add(b);
        }
    }

    private void sendChunk() {
        futures.add(service.submit(() -> chunkFunction.apply(chunk)));
    }

    @Override
    public void close() throws IOException {
        if (chunk.hasData()) {
            sendChunk();
        }
        ListenableFuture<String> allFutures = Futures.whenAllSucceed(futures).call(() -> "ok");
        try {
            allFutures.get();
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
