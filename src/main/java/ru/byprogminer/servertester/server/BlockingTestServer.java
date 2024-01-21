package ru.byprogminer.servertester.server;

import ru.byprogminer.servertester.Utils;
import ru.byprogminer.servertester.config.TestRunConfig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class BlockingTestServer extends AbstractTestServer implements TestServer {

    private ServerSocket serverSocket;
    private Thread serverThread;

    private final Map<Socket, Client> clients = new ConcurrentHashMap<>();

    public BlockingTestServer(TestRunConfig config) {
        super(config);
    }

    @Override
    public void serve() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(null);

        serverThread = new Thread(this::serverRoutine);
        serverThread.start();
    }

    @Override
    public void shutdown() throws IOException, InterruptedException {
        serverSocket.close();
        serverThread.join();

        while (!clients.isEmpty()) {
            final Iterator<Client> it = clients.values().iterator();

            if (!it.hasNext()) {
                break;
            }

            it.next().shutdown();
        }

        super.shutdown();
    }

    @Override
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    protected int getConnectedClients() {
        return clients.size();
    }

    private void serverRoutine() {
        try (final ServerSocket ignored = serverSocket) {
            while (!serverSocket.isClosed()) {
                final Socket socket;

                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        break;
                    }

                    throw e;
                }

                final Client client = new Client(socket);
                client.handle();

                if (clients.put(socket, client) != null) {
                    throw new IllegalStateException("duplicate socket");
                }
            }
        } catch (Throwable e) {
            addException(e);
        }
    }

    private class Client {

        private final Socket socket;
        private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

        private Thread readThread;

        public Client(Socket socket) {
            this.socket = socket;
        }

        public void handle() {
            readThread = new Thread(this::clientRoutine);
            readThread.start();
        }

        public void shutdown() throws IOException, InterruptedException {
            socket.close();
            readThread.join();
            writeExecutor.shutdown();
            Utils.awaitTermination(writeExecutor);
        }

        private void clientRoutine() {
            try (final Socket ignored = socket) {
                final DataInputStream in = new DataInputStream(socket.getInputStream());
                final DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                while (!socket.isClosed()) {
                    final int n = in.readInt();

                    final long requestBeginTime = System.currentTimeMillis();

                    final byte[] request = new byte[n];
                    in.readFully(request);

                    taskExecutor.submit(() -> {
                        try {
                            final byte[] response = handleRequest(request);

                            writeExecutor.submit(() -> {
                                try {
                                    out.writeInt(response.length);
                                    out.write(response);

                                    increaseMetrics(t -> metrics.handleTime.addAndGet(t - requestBeginTime));
                                } catch (Throwable e) {
                                    addException(e);
                                }
                            });
                        } catch (Throwable e) {
                            addException(e);
                        }
                    });
                }
            } catch (Throwable e) {
                addException(e);
            } finally {
                clients.remove(socket);
            }
        }
    }
}
