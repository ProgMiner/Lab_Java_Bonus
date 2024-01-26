package ru.byprogminer.servertester.server;

import com.google.protobuf.InvalidProtocolBufferException;
import ru.byprogminer.servertester.Messages;
import ru.byprogminer.servertester.Utils;
import ru.byprogminer.servertester.config.TestRunConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public abstract class AbstractTestServer implements TestServer {

    protected final TestRunConfig config;

    protected final ServerTestMetrics metrics;
    protected final CountDownLatch clientsLatch;

    protected final ExecutorService taskExecutor = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

    public AbstractTestServer(TestRunConfig config) {
        this.config = config;

        this.metrics = new ServerTestMetrics(config.clients * config.clientRequests);
        this.clientsLatch = new CountDownLatch(config.clients);
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

    protected abstract int getConnectedClients();

    protected void measureMetrics(Consumer<Long> block) {
        final long time = System.currentTimeMillis();

        if (getConnectedClients() == config.clients) {
            block.accept(time);
        }
    }

    protected void addException(Throwable e) {
        exceptions.add(e);
    }

    protected byte[] handleRequest(byte[] request) throws InvalidProtocolBufferException {
        final long beginTime = System.currentTimeMillis();

        final byte[] response = handleRequest0(request);

        measureMetrics(t -> metrics.computationTime.addAndGet(t - beginTime));
        return response;
    }

    private static byte[] handleRequest0(byte[] request) throws InvalidProtocolBufferException {
        final Messages.SortRequest sortRequest = Messages.SortRequest.parseFrom(request);

        final int[] values = sortRequest.getValueList().stream().mapToInt(Integer::intValue).toArray();
        Utils.cringeSort(values);

        final Messages.SortResponse sortResponse = Messages.SortResponse.newBuilder()
                .addAllValue(IntStream.of(values).boxed().collect(Collectors.toList()))
                .build();

        return sortResponse.toByteArray();
    }
}
