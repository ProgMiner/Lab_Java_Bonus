package ru.byprogminer.servertester.server;

import java.util.concurrent.atomic.AtomicLong;


public final class ServerTestMetrics {

    private final int requests;

    public final AtomicLong computationTime = new AtomicLong();
    public final AtomicLong handleTime = new AtomicLong();

    public ServerTestMetrics(int requests) {
        this.requests = requests;
    }

    public ServerTestResult freeze() {
        return new ServerTestResult(
                computationTime.get() / requests,
                handleTime.get() / requests
        );
    }
}
