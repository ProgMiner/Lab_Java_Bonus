package ru.byprogminer.servertester.config;

import java.util.Objects;
import java.util.function.Function;


public class TestRunConfig {

    public final int clientRequests;
    public final int arraySize;
    public final int clients;
    public final int requestDelta;

    private final Function<TestRunConfig, Object> variableParameterGetter;

    public TestRunConfig(
            int clientRequests,
            int arraySize,
            int clients,
            int requestDelta,
            Function<TestRunConfig, Object> variableParameterGetter
    ) {
        this.clientRequests = clientRequests;
        this.arraySize = arraySize;
        this.clients = clients;
        this.requestDelta = requestDelta;
        this.variableParameterGetter = Objects.requireNonNull(variableParameterGetter);
    }

    public TestRunConfig withClientRequests(int clientRequests) {
        return new TestRunConfig(clientRequests, arraySize, clients, requestDelta, variableParameterGetter);
    }

    public TestRunConfig withArraySize(int arraySize) {
        return new TestRunConfig(clientRequests, arraySize, clients, requestDelta, variableParameterGetter);
    }

    public TestRunConfig withClients(int clients) {
        return new TestRunConfig(clientRequests, arraySize, clients, requestDelta, variableParameterGetter);
    }

    public TestRunConfig withRequestDelta(int requestDelta) {
        return new TestRunConfig(clientRequests, arraySize, clients, requestDelta, variableParameterGetter);
    }

    public Object getVariableParameter() {
        return variableParameterGetter.apply(this);
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
