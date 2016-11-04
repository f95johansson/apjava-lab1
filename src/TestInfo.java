/*
 * File: TestInfo.java
 * Author: Fredrik Johansson
 * Date: 2016-10-31
 */

/**
 * Container class for information about a test
 */
public class TestInfo {

    public enum Status {
        SUCCESS,
        FAILED,
        RUNNING,
        WARNING
    }

    private Status mStatus;
    private String mName;
    private Throwable mException;

    public TestInfo(Status status, String name) {
        mStatus = status;
        mName = name;
    }

    public void setStatus(Status status) {
        mStatus = status;
    }

    public Status getStatus() {
        return mStatus;
    }

    public String getName() {
        return mName;
    }

    public boolean hasException() {
        return mException != null;
    }

    public void setException(Throwable exception) {
        mException = exception;
    }

    public Throwable getException() {
        return mException;
    }

    public boolean isRunning() {
        return mStatus == Status.RUNNING;
    }
}
