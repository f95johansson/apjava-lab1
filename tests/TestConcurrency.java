/*
 * File: TestConcurrency.java
 * Author: Fredrik Johansson
 * Date: 2016-10-31
 */

public class TestConcurrency implements TestClass {

    private Integer i;


    public boolean testConcurrentWriteFirst() throws InterruptedException {
        i = null;
        Thread.sleep(500);
        i = null;
        Thread.sleep(500);
        return i == null;
    }

    public boolean testConcurrentWriteSecond() throws InterruptedException {
        i = 1;
        Thread.sleep(500);
        i = 1;
        Thread.sleep(500);
        return i == 1;
    }

    public boolean testSleep() throws InterruptedException {
        Thread.sleep(3000);
        return true;
    }

    public boolean testThreads() throws InterruptedException {
        Thread t = new Thread(() -> {
            for (int i = 0; i < 100000000;) {
                i = i + 2 - 1;
            }
        });
        t.start();
        t.join();

        return true;
    }
}
