package ru.byprogminer.servertester;

import ru.byprogminer.servertester.config.TestConfig;
import ru.byprogminer.servertester.config.TestRunConfig;
import ru.byprogminer.servertester.server.BlockingTestServer;
import ru.byprogminer.servertester.server.TestServer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;


public class TestRunner {

    private static final String TEST_CONFIG_FILENAME = "config.txt";

    private static final Map<TestConfig.Architecture, Function<TestRunConfig, TestServer>> ARCHITECTURES
            = constructArchitecturesMap();

    private final TestConfig config;

    private final Function<TestRunConfig, TestServer> serverFactory;

    public TestRunner(TestConfig config) {
        this.config = Objects.requireNonNull(config);

        serverFactory = ARCHITECTURES.get(config.getArch());
        if (serverFactory == null) {
            throw new IllegalArgumentException("architecture " + config.getArch() + " is not supported");
        }
    }

    public void runTest() throws IOException {
        printTestConfig();

        for (final TestRunConfig runConfig : config) {
            System.out.println("Run config: " + runConfig);

            final TestServer server = serverFactory.apply(runConfig);
            server.serve();

            // TODO run clients

            while (true) {
                try {
                    server.shutdown();
                    break;
                } catch (InterruptedException e) {
                    // ignored
                }
            }

            final Throwable e = server.getExceptions().stream().reduce(null, Utils::nextException);
            if (e != null) {
                e.printStackTrace();
                break;
            }

            System.out.println("Test result: " + server.getTestResult());
        }
    }

    private void printTestConfig() throws IOException {
        final Path path = config.getOutputDir().resolve(TEST_CONFIG_FILENAME);

        try (final BufferedWriter writer = Files.newBufferedWriter(path)) {
            printTestConfig(config, writer);
        }
    }

    private static void printTestConfig(TestConfig config, Writer writer) {
        final PrintWriter out = new PrintWriter(writer);

        out.println("Server architecture: " + config.getArch());
        out.println("Requests per client: " + config.getClientRequests());
        out.println("Array size: " + config.getArraySize());
        out.println("Number of clients: " + config.getClients());
        out.println("Duration between client requests: " + config.getRequestDelta());
    }

    private static Map<TestConfig.Architecture, Function<TestRunConfig, TestServer>> constructArchitecturesMap() {
        final Map<TestConfig.Architecture, Function<TestRunConfig, TestServer>> architectures
                = new EnumMap<>(TestConfig.Architecture.class);

        architectures.put(TestConfig.Architecture.BLOCK, BlockingTestServer::new);

        // TODO

        return Collections.unmodifiableMap(architectures);
    }
}
