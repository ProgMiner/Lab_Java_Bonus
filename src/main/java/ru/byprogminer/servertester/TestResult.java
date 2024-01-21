package ru.byprogminer.servertester;

import ru.byprogminer.servertester.client.ClientTestResult;
import ru.byprogminer.servertester.server.ServerTestResult;

import java.util.Objects;


public final class TestResult {

    public final ServerTestResult server;
    public final ClientTestResult client;

    public TestResult(ServerTestResult server, ClientTestResult client) {
        this.server = Objects.requireNonNull(server);
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "server=" + server +
                ", client=" + client +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TestResult that = (TestResult) o;
        return Objects.equals(server, that.server) && Objects.equals(client, that.client);
    }

    @Override
    public int hashCode() {
        return Objects.hash(server, client);
    }
}
