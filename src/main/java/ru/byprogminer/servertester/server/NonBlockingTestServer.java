package ru.byprogminer.servertester.server;

import ru.byprogminer.servertester.config.TestRunConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;


public class NonBlockingTestServer extends AbstractTestServer implements TestServer {

    private ServerSocketChannel serverChannel;
    private Thread serverThread;

    private ReadThread readThread;
    private WriteThread writeThread;

    private final ConcurrentMap<SocketChannel, Client> clients = new ConcurrentHashMap<>();

    public NonBlockingTestServer(TestRunConfig config) {
        super(config);
    }

    @Override
    public void serve() throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(null);

        serverThread = new Thread(this::serverRoutine);
        serverThread.start();

        readThread = new ReadThread();
        readThread.start();

        writeThread = new WriteThread();
        writeThread.start();
    }

    @Override
    public void shutdown() throws IOException, InterruptedException {
        serverChannel.close();
        serverThread.join();
        readThread.shutdown();
        writeThread.shutdown();
        super.shutdown();
    }

    @Override
    public int getPort() {
        return serverChannel.socket().getLocalPort();
    }

    private void serverRoutine() {
        try (final ServerSocketChannel ignored = serverChannel) {
            while (serverChannel.isOpen()) {
                final SocketChannel channel;

                try {
                    channel = serverChannel.accept();
                } catch (IOException e) {
                    if (!serverChannel.isOpen()) {
                        break;
                    }

                    throw e;
                }

                clients.entrySet().removeIf(e -> !e.getKey().isOpen());

                channel.configureBlocking(false);

                final Client client = new Client(channel);
                if (clients.put(channel, client) != null) {
                    throw new IllegalStateException("duplicate channel");
                }

                readThread.registerClient(new Client(channel));
                clientsLatch.countDown();
            }
        } catch (Throwable e) {
            addException(e);
        } finally {
            for (final SocketChannel channel : clients.keySet()) {
                try {
                    channel.close();
                } catch (Throwable e) {
                    addException(e);
                }
            }

            taskExecutor.shutdown();
        }
    }

    @Override
    protected int getConnectedClients() {
        return clients.size();
    }

    private class ReadThread extends Thread {

        private final Selector selector;

        private final Queue<Client> registeringClients = new ConcurrentLinkedQueue<>();

        public ReadThread() throws IOException {
            selector = Selector.open();
        }

        public void registerClient(Client client) {
            registeringClients.add(client);
            selector.wakeup();
        }

        public void shutdown() throws IOException, InterruptedException {
            selector.close();
            join();
        }

        @Override
        public void run() {
            try (final Selector ignored = selector) {
                clientsLatch.await();

                while (selector.isOpen()) {
                    selector.select();

                    if (!selector.isOpen()) {
                        break;
                    }

                    while (!registeringClients.isEmpty()) {
                        final Client client = registeringClients.poll();
                        client.channel.register(selector, SelectionKey.OP_READ, client);
                    }

                    final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        final Client client = (Client) it.next().attachment();
                        it.remove();

                        try {
                            client.channel.read(client.readBuffer);
                        } catch (IOException e) {
                            if (!client.channel.isOpen()) {
                                continue;
                            }

                            throw e;
                        }

                        if (client.previousRequestTime == 0) {
                            client.previousRequestTime = System.currentTimeMillis();
                        }

                        if (!client.readBuffer.hasRemaining()) {
                            client.readBuffer.flip();

                            final ByteBuffer newBuffer = ByteBuffer.allocate(client.readBuffer.remaining() * 2);
                            newBuffer.put(client.readBuffer);
                            client.readBuffer = newBuffer;
                        }

                        if (client.readBuffer.position() < Integer.BYTES) {
                            continue;
                        }

                        final int requestSize = client.readBuffer.getInt(0);
                        if (client.readBuffer.position() < Integer.BYTES + requestSize) {
                            continue;
                        }

                        final byte[] request = new byte[requestSize];

                        client.readBuffer.flip();
                        client.readBuffer.getInt();
                        client.readBuffer.get(request);
                        client.readBuffer.compact();

                        final long requestTime = client.previousRequestTime;
                        client.previousRequestTime = 0;

                        taskExecutor.submit(() -> {
                            try {
                                final byte[] response = handleRequest(request);

                                final ByteBuffer responseBuffer = ByteBuffer
                                        .allocate(Integer.BYTES + response.length);

                                responseBuffer.putInt(response.length);
                                responseBuffer.put(response);
                                responseBuffer.flip();

                                client.answers.add(new Answer(responseBuffer, requestTime));
                                writeThread.registerClient(client);
                            } catch (Throwable e) {
                                addException(e);
                            }
                        });
                    }
                }
            } catch (Throwable e) {
                addException(e);
            }
        }
    }

    private class WriteThread extends Thread {

        private final Selector selector;

        private final Queue<Client> registeringClients = new ConcurrentLinkedQueue<>();

        public WriteThread() throws IOException {
            selector = Selector.open();
        }

        public void registerClient(Client client) {
            registeringClients.add(client);
            selector.wakeup();
        }

        public void shutdown() throws IOException, InterruptedException {
            selector.close();
            join();
        }

        @Override
        public void run() {
            try (final Selector ignored = selector) {
                clientsLatch.await();

                while (selector.isOpen()) {
                    selector.select();

                    if (!selector.isOpen()) {
                        break;
                    }

                    while (!registeringClients.isEmpty()) {
                        final Client client = registeringClients.poll();
                        client.channel.register(selector, SelectionKey.OP_WRITE, client);
                    }

                    final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        final SelectionKey key = it.next();
                        final Client client = (Client) key.attachment();
                        it.remove();

                        final Answer currentAnswer = client.answers.peek();
                        if (currentAnswer == null) {
                            key.cancel();
                            continue;
                        }

                        client.channel.write(currentAnswer.buffer);

                        if (!currentAnswer.buffer.hasRemaining()) {
                            measureMetrics(t -> metrics.handleTime.addAndGet(t - currentAnswer.requestTime));
                            client.answers.poll();
                        }
                    }
                }
            } catch (Throwable e) {
                addException(e);
            }
        }
    }

    private static class Client {

        private final SocketChannel channel;

        private ByteBuffer readBuffer = ByteBuffer.allocate(4096);
        private final Queue<Answer> answers = new ConcurrentLinkedQueue<>();

        private long previousRequestTime;

        public Client(SocketChannel channel) {
            this.channel = channel;
        }
    }

    private static class Answer {

        private final ByteBuffer buffer;
        private final long requestTime;

        public Answer(ByteBuffer buffer, long requestTime) {
            this.buffer = Objects.requireNonNull(buffer);
            this.requestTime = requestTime;
        }
    }
}
