package ru.byprogminer.servertester.client;

import java.util.concurrent.atomic.AtomicLong;


public final class ClientTestMetrics {

    private final int clients;

    public final AtomicLong averageRequestTime = new AtomicLong();

    public ClientTestMetrics(int clients) {
        this.clients = clients;
    }

    public ClientTestResult freeze() {
        return new ClientTestResult(
                averageRequestTime.get() / clients
        );
    }
}
