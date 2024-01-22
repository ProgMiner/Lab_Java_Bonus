package ru.byprogminer.servertester;

import com.opencsv.CSVWriter;
import ru.byprogminer.servertester.client.TestClientRunner;
import ru.byprogminer.servertester.config.TestConfig;
import ru.byprogminer.servertester.config.TestRunConfig;
import ru.byprogminer.servertester.server.BlockingTestServer;
import ru.byprogminer.servertester.server.NonBlockingTestServer;
import ru.byprogminer.servertester.server.TestServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;


public class TestRunner {

    private static final String TEST_CONFIG_FILENAME = "config.txt";
    private static final String DATA_FILENAME = "data.csv";

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

        final Map<TestRunConfig, TestResult> results = new LinkedHashMap<>();

        for (final TestRunConfig runConfig : config) {
            System.out.println("Run config: " + runConfig);

            final TestServer server = serverFactory.apply(runConfig);
            server.serve();

            final TestClientRunner clientRunner = new TestClientRunner(runConfig, server.getPort());
            clientRunner.runClients();

            while (true) {
                try {
                    server.shutdown();
                    break;
                } catch (InterruptedException e) {
                    // ignored
                }
            }

            final Throwable e = Stream.concat(
                    server.getExceptions().stream(),
                    clientRunner.getExceptions().stream()
            ).reduce(null, Utils::nextException);

            if (e != null) {
                e.printStackTrace();
                break;
            }

            final TestResult result = new TestResult(
                    server.getTestResult(),
                    clientRunner.getTestResult()
            );

            results.put(runConfig, result);
        }

        printResults(results);
    }

    private void printTestConfig() throws IOException {
        final Path path = config.getOutputDir().resolve(TEST_CONFIG_FILENAME);

        try (final Writer writer = Files.newBufferedWriter(path)) {
            printTestConfig(config, writer);
        }
    }

    private void printResults(Map<TestRunConfig, TestResult> results) throws IOException {
        final Path path = config.getOutputDir().resolve(DATA_FILENAME);

        try (final Writer writer = Files.newBufferedWriter(path)) {
            printResults(results, writer);
        }
    }

    private void printResults(Map<TestRunConfig, TestResult> results, Writer writer) throws IOException {
        final CSVWriter csvWriter = new CSVWriter(writer);

        csvWriter.writeNext(new String[] {
                config.getVariableParameter(),
                "computation time",
                "server request time",
                "client request time",
        });

        for (final Map.Entry<TestRunConfig, TestResult> entry : results.entrySet()) {
            csvWriter.writeNext(new String[] {
                    entry.getKey().getVariableParameter().toString(),
                    Long.toString(entry.getValue().server.computationTime),
                    Long.toString(entry.getValue().server.handleTime),
                    Long.toString(entry.getValue().client.averageRequestTime),
            });
        }

        final IOException ex = csvWriter.getException();
        if (ex != null) {
            throw ex;
        }
    }

    private static void printTestConfig(TestConfig config, Writer writer) {
        final PrintWriter out = new PrintWriter(writer);

        out.println("Server architecture: " + config.getArch());
        out.println("Requests per client: " + config.getClientRequests());
        out.println("Array size: " + config.getArraySize());
        out.println("Number of clients: " + config.getClients());
        out.println("Duration between client requests: " + config.getRequestDelta());

        out.flush();
    }

    private static Map<TestConfig.Architecture, Function<TestRunConfig, TestServer>> constructArchitecturesMap() {
        final Map<TestConfig.Architecture, Function<TestRunConfig, TestServer>> architectures
                = new EnumMap<>(TestConfig.Architecture.class);

        architectures.put(TestConfig.Architecture.BLOCK, BlockingTestServer::new);
        architectures.put(TestConfig.Architecture.NONBLOCK, NonBlockingTestServer::new);

        // TODO async server

        return Collections.unmodifiableMap(architectures);
    }
}
