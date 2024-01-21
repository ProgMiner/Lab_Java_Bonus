package ru.byprogminer.servertester.server;


import java.util.concurrent.atomic.AtomicLong;

public final class ServerTestMetrics {

    public final int clients;

    public final AtomicLong computationTime = new AtomicLong();
    public final AtomicLong handleTime = new AtomicLong();

    public ServerTestMetrics(int clients) {
        this.clients = clients;
    }

    public ServerTestResult freeze() {
        return new ServerTestResult(
                computationTime.get() / clients,
                handleTime.get() / clients
        );
    }
}
