package ru.byprogminer.servertester.client;

import java.util.Objects;


public final class ClientTestResult {

    public final long averageRequestTime;

    public ClientTestResult(long averageRequestTime) {
        this.averageRequestTime = averageRequestTime;
    }

    @Override
    public String toString() {
        return "ClientTestResult{" +
                "averageRequestTime=" + averageRequestTime +
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

        final ClientTestResult that = (ClientTestResult) o;
        return averageRequestTime == that.averageRequestTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(averageRequestTime);
    }
}
