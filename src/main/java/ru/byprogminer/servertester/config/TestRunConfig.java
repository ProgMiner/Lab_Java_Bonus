package ru.byprogminer.servertester.config;

import java.util.Objects;


public class TestRunConfig {

    public final int clientRequests;
    public final int arraySize;
    public final int clients;
    public final PrettyDuration requestDelta;

    public TestRunConfig(int clientRequests, int arraySize, int clients, PrettyDuration requestDelta) {
        this.clientRequests = clientRequests;
        this.arraySize = arraySize;
        this.clients = clients;
        this.requestDelta = Objects.requireNonNull(requestDelta);
    }

    public TestRunConfig withClientRequests(int clientRequests) {
        return new TestRunConfig(clientRequests, arraySize, clients, requestDelta);
    }

    public TestRunConfig withArraySize(int arraySize) {
        return new TestRunConfig(clientRequests, arraySize, clients, requestDelta);
    }

    public TestRunConfig withClients(int clients) {
        return new TestRunConfig(clientRequests, arraySize, clients, requestDelta);
    }

    public TestRunConfig withRequestDelta(PrettyDuration requestDelta) {
        return new TestRunConfig(clientRequests, arraySize, clients, requestDelta);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TestRunConfig that = (TestRunConfig) o;
        return clientRequests == that.clientRequests
                && arraySize == that.arraySize
                && clients == that.clients
                && Objects.equals(requestDelta, that.requestDelta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientRequests, arraySize, clients, requestDelta);
    }

    @Override
    public String toString() {
        return "TestRunConfig{" +
                "clientRequests=" + clientRequests +
                ", arraySize=" + arraySize +
                ", clients=" + clients +
                ", requestDelta=" + requestDelta +
                '}';
    }
}
