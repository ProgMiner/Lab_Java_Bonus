package ru.byprogminer.servertester.client;

import com.google.protobuf.InvalidProtocolBufferException;
import ru.byprogminer.servertester.Messages;
import ru.byprogminer.servertester.config.TestRunConfig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;


public class TestClientRunner {

    private final TestRunConfig config;
    private final int port;

    private final ClientTestMetrics metrics;

    private final Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

    public TestClientRunner(TestRunConfig config, int port) {
        this.config = config;
        this.port = port;

        this.metrics = new ClientTestMetrics(config.clients);
    }

    public void runClients() {
        final Thread[] threads = new Thread[config.clients];

        for (int i = 0; i < config.clients; ++i) {
            threads[i] = new Thread(this::clientRoutine);
            threads[i].start();
        }

        for (final Thread t : threads) {
            while (t.isAlive()) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        }
    }

    public ClientTestResult getTestResult() {
        return metrics.freeze();
    }

    public List<Throwable> getExceptions() {
        final List<Throwable> result = new ArrayList<>(exceptions.size());

        while (!exceptions.isEmpty()) {
            result.add(exceptions.poll());
        }

        return result;
    }

    private void clientRoutine() {
        try (final Socket socket = new Socket("127.0.0.1", port)) {
            final long beginTime = System.currentTimeMillis();

            final DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            for (int i = 0; i < config.clientRequests; ++i) {
                if (i > 0) {
                    Thread.sleep(config.requestDelta.value.toMillis());
                }

                final byte[] request = createRequest();

                out.writeInt(request.length);
                out.write(request);
            }

            final DataInputStream in = new DataInputStream(socket.getInputStream());

            for (int i = 0; i < config.clientRequests; ++i) {
                final int responseSize = in.readInt();

                final byte[] response = new byte[responseSize];
                in.readFully(response);

                checkResponse(response);
            }

            final long averageRequestTime = (System.currentTimeMillis() - beginTime) / config.clientRequests;
            metrics.averageRequestTime.addAndGet(averageRequestTime);
        } catch (Throwable e) {
            addException(e);
        }
    }

    private void addException(Throwable e) {
        exceptions.add(e);
    }

    private byte[] createRequest() {
        final Messages.SortRequest.Builder sortRequest = Messages.SortRequest.newBuilder();

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < config.arraySize; ++i) {
            sortRequest.addValue(random.nextInt());
        }

        return sortRequest.build().toByteArray();
    }

    private void checkResponse(byte[] response) throws InvalidProtocolBufferException {
        final Messages.SortResponse sortResponse = Messages.SortResponse.parseFrom(response);

        if (sortResponse.getValueCount() != config.arraySize) {
            throw new IllegalArgumentException("wrong array size (" + sortResponse.getValueCount()
                    + " vs " + config.arraySize + ")");
        }

        for (int i = 1; i < config.arraySize; ++i) {
            if (sortResponse.getValue(i - 1) > sortResponse.getValue(i)) {
                throw new IllegalArgumentException("array is not sorted");
            }
        }
    }
}
