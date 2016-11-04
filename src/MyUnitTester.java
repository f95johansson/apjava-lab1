/*
 * File: MyUnitTester.java
 * Author: Fredrik Johansson
 * Date: 2016-10-29
 */

import java.lang.reflect.Method;
import java.util.Observable;
import java.util.Observer;

/**
 * Takes care of user interaction and starts the necessary test.
 * Controller in MVC.
 */
public class MyUnitTester implements Observer {

    private TestListener mCurrentListener;

    /**
     * Triggers when the observable reports a change. In this case it
     * will be whenever a user inputted a test class name and hit "run".
     *
     * @param o The observable interface
     * @param arg Should be a string of user inputted text
     */
    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof String && o instanceof UI) {//checks for safe casting
            UI ui = (UI) o;
            if (arg.equals("")) {
                ui.addUserError("Please specify name of test class");
                return;
            }

            try {
                if (mCurrentListener != null) {
                    mCurrentListener.stop();
                }
                mCurrentListener = new TestListener(ui);
                new Thread(new Tester((String) arg, mCurrentListener)).start();

            } catch (ClassNotFoundException | IllegalFormatException e) {
                ui.addUserError(e.getMessage());
            }
        }
    }

    /**
     * Helper class to interface between the ui and the tester.
     * Separated in to its own class to better handle threaded use
     */
    private static class TestListener implements Tester.ResultListener {

        private UI mUI;
        private boolean isStopped = false;

        public TestListener(UI ui) {
            mUI = ui;
        }

        public void stop() {
            isStopped = true;
        }

        @Override
        public void onTestStart(Method method) {
            if (!isStopped) {
                mUI.addTestStarted(method.getName());
            }
        }

        @Override
        public void onTestWarning(Method method, String reason) {
            if (!isStopped) {
                mUI.addTestWarning(method.getName()+": "+reason);
            }
        }

        @Override
        public void onTestFinished(Method method, boolean success) {
            if (!isStopped) {
                mUI.addTestResult(method.getName(), success);
            }
        }

        @Override
        public void onTestFinished(Method method,
                                   boolean success, Throwable exception) {
            if (!isStopped) {
                mUI.addTestResult(method.getName(), success, exception);
            }
        }

        @Override
        public void onAllTestsFinished(int successes,
                                       int failedWithoutException,
                                       int failedFromException) {
            if (!isStopped) {
                mUI.updateStatusDone(
                        successes+failedWithoutException+failedFromException,
                        successes,
                        failedWithoutException+failedFromException,
                        failedFromException);
            }
        }

        @Override
        public void onNonTestException(Throwable exception) {
            if (!isStopped) {
                mUI.addTestWarning(exception.getMessage());
                isStopped = true;
            }
        }

    }

    /**
     * Starts the ui and the UnitTester (controller)
     */
    public static void main(String[] args) {
        new UI().addObserver(new MyUnitTester());
    }
}
