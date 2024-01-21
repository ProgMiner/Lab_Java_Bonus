package ru.byprogminer.servertester.server;

import java.util.Objects;


public final class ServerTestResult {

    public final long computationTime;
    public final long handleTime;

    public ServerTestResult(long computationTime, long handleTime) {
        this.computationTime = computationTime;
        this.handleTime = handleTime;
    }

    @Override
    public String toString() {
        return "ServerTestResult{" +
                "computationTime=" + computationTime +
                ", handleTime=" + handleTime +
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

        final ServerTestResult that = (ServerTestResult) o;
        return computationTime == that.computationTime && handleTime == that.handleTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(computationTime, handleTime);
    }
}
