package ru.byprogminer.servertester.server;

import ru.byprogminer.servertester.Utils;
import ru.byprogminer.servertester.config.TestRunConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class AsyncTestServer extends AbstractTestServer implements TestServer {

    private AsynchronousServerSocketChannel serverChannel;

    private final ConcurrentMap<AsynchronousSocketChannel, Client> clients = new ConcurrentHashMap<>();

    public AsyncTestServer(TestRunConfig config) {
        super(config);
    }

    @Override
    public void serve() throws IOException {
        serverChannel = AsynchronousServerSocketChannel.open();
        serverChannel.bind(null);

        serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {

            @Override
            public void completed(AsynchronousSocketChannel channel, Object attachment) {
                final Client client = new Client(channel);

                clients.put(channel, client);
                channel.read(client.buffer, client, client);

                serverChannel.accept(null, this);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                for (final AsynchronousSocketChannel channel : clients.keySet()) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        addException(e);
                    }
                }

                if (!serverChannel.isOpen()) {
                    return;
                }

                addException(exc);
            }
        });
    }

    @Override
    public void shutdown() throws IOException, InterruptedException {
        serverChannel.close();
        super.shutdown();
    }

    @Override
    public int getPort() {
        try {
            return ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private class Client implements CompletionHandler<Integer, Client> {

        private final AsynchronousSocketChannel channel;

        private ByteBuffer buffer = ByteBuffer.allocate(4096);
        private boolean writeMode = false;

        private long requestTime;

        public Client(AsynchronousSocketChannel channel) {
            this.channel = channel;
        }

        @Override
        public void completed(Integer result, Client attachment) {
            try {
                if (writeMode) {
                    if (writeCompleted(result)) {
                        channel.write(buffer, this, this);
                    }
                } else {
                    if (readCompleted(result)) {
                        channel.read(buffer, this, this);
                    }
                }
            } catch (Throwable e) {
                clients.remove(channel);
                addException(e);
            }
        }

        @Override
        public void failed(Throwable exc, Client attachment) {
            clients.remove(channel);

            if (!channel.isOpen()) {
                return;
            }

            addException(exc);
        }

        private boolean readCompleted(int wasRead) {
            if (wasRead < 0) {
                clients.remove(channel);
                return false;
            }

            if (!buffer.hasRemaining()) {
                buffer.flip();

                final ByteBuffer newBuffer = ByteBuffer.allocate(buffer.remaining() * 2);
                newBuffer.put(buffer);
                buffer = newBuffer;
                return true;
            }

            if (wasRead == 0) {
                throw new IllegalArgumentException("wut");
            }

            if (requestTime == 0) {
                requestTime = System.currentTimeMillis();
            }

            if (buffer.position() < Integer.BYTES) {
                return true;
            }

            final int requestSize = buffer.getInt(0);

            if (buffer.position() < Integer.BYTES + requestSize) {
                return true;
            }

            final byte[] request = new byte[requestSize];

            buffer.flip();
            buffer.getInt();
            buffer.get(request);
            buffer.compact();

            if (buffer.position() > 0) {
                throw new IllegalStateException("not REPL protocol detected");
            }

            taskExecutor.submit(() -> {
                try {
                    final byte[] response = handleRequest(request);

                    final int responseBufferSize = Integer.BYTES + response.length;
                    if (responseBufferSize > buffer.capacity()) {
                        buffer = ByteBuffer.allocate(Utils.minPowerOfTwo(responseBufferSize));
                    }

                    buffer.putInt(response.length);
                    buffer.put(response);
                    buffer.flip();

                    writeMode = true;
                    channel.write(buffer, this, this);
                } catch (Throwable e) {
                    addException(e);
                }
            });

            return false;
        }

        private boolean writeCompleted(int wasWrote) {
            if (wasWrote < 0) {
                clients.remove(channel);
                return false;
            }

            if (buffer.hasRemaining()) {
                return true;
            }

            if (wasWrote == 0) {
                throw new IllegalArgumentException("wut");
            }

            metrics.handleTime.addAndGet(System.currentTimeMillis() - requestTime);
            requestTime = 0;

            buffer.clear();

            writeMode = false;
            channel.read(buffer, this, this);
            return false;
        }
    }
}
