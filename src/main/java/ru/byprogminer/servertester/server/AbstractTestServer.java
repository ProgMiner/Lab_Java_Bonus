package ru.byprogminer.servertester.server;

import com.google.protobuf.InvalidProtocolBufferException;
import ru.byprogminer.servertester.Messages;
import ru.byprogminer.servertester.Utils;
import ru.byprogminer.servertester.config.TestRunConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public abstract class AbstractTestServer implements TestServer {

    protected final TestRunConfig config;

    protected final ServerTestMetrics metrics;

    protected final ExecutorService taskExecutor = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

    public AbstractTestServer(TestRunConfig config) {
        this.config = config;

        this.metrics = new ServerTestMetrics(config.clients * config.clientRequests);
    }

    @Override
    public void shutdown() throws IOException, InterruptedException {
        taskExecutor.shutdown();
        Utils.awaitTermination(taskExecutor);
    }

    @Override
    public ServerTestResult getTestResult() {
        return metrics.freeze();
    }

    @Override
    public List<Throwable> getExceptions() {
        final List<Throwable> result = new ArrayList<>(exceptions.size());

        while (!exceptions.isEmpty()) {
            result.add(exceptions.poll());
        }

        return result;
    }

    protected void addException(Throwable e) {
        exceptions.add(e);
    }

    protected byte[] handleRequest(byte[] request) throws InvalidProtocolBufferException {
        final long beginTime = System.currentTimeMillis();

        final byte[] response = handleRequest0(request);

        metrics.computationTime.addAndGet(System.currentTimeMillis() - beginTime);

        return response;
    }

    private static byte[] handleRequest0(byte[] request) throws InvalidProtocolBufferException {
        final Messages.SortRequest sortRequest = Messages.SortRequest.parseFrom(request);

        final Integer[] values = sortRequest.getValueList().toArray(new Integer[0]);
        Arrays.sort(values);

        final Messages.SortResponse sortResponse = Messages.SortResponse.newBuilder()
                .addAllValue(Arrays.asList(values))
                .build();

        return sortResponse.toByteArray();
    }
}
