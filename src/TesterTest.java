/*
 * File: TesterTest.java
 * Author: Fredrik Johansson
 * Date: 2016-11-02
 */

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class TesterTest {

    public Tester.ResultListener emptyListener = new Tester.ResultListener() {
        @Override
        public void onTestStart(Method method) {}

        @Override
        public void onTestFinished(Method method, boolean success) {}

        @Override
        public void onTestWarning(Method method, String reason) {}

        @Override
        public void onTestFinished(Method method, boolean success,
                                   Throwable exception) {}

        @Override
        public void onAllTestsFinished(int successes,
                                       int failedWithoutException,
                                       int failedFromException) {}

        @Override
        public void onNonTestException(Throwable exception) {}
    };

    public static class TestNoImplement {}
    public static class TestConstructorParameter implements TestClass {
        public TestConstructorParameter(int i) {}
    }
    public static class TestNonPublicConstructor implements TestClass {
        protected TestNonPublicConstructor() {}
    }
    public static class TestValidConstructor implements TestClass {
        public TestValidConstructor() {}
    }

    public static class TestValid implements TestClass {
        public TestValid() {}
        public boolean testSuccess() {
            return true;
        }
        public boolean testFail() {
            return false;
        }
        public boolean testException() throws Exception {
            throw new Exception();
        }
        protected boolean testNonPublic () {
            return true;
        }
        public void testWrongReturnType () {}

        public boolean testParameters(int i) {
            return true;
        }
        public boolean invalidName() {
            return true;
        }
    }


    @Test(expected=ClassNotFoundException.class)
    public void shouldGiveErrorOnClassNotFound() throws Exception {
        new Tester("TestDontExist", emptyListener);
    }

    @Test(expected=IllegalFormatException.class)
    public void shouldNotAcceptUnimplementedTest() throws Exception {
        new Tester("TesterTest$TestNoImplement", emptyListener);
    }

    @Test(expected=IllegalFormatException.class)
    public void shouldNotAcceptConstructorWithParameter() throws Exception {
        new Tester("TesterTest$TestConstructorParameter", emptyListener);
    }

    @Test(expected=IllegalFormatException.class)
    public void shouldNotAcceptNonPublicConstructor() throws Exception {
        new Tester("TesterTest$TestNonPublicConstructor", emptyListener);
    }

    @Test
    public void shouldAcceptValidConstructor() throws Exception {
        new Tester("TesterTest$TestValidConstructor", emptyListener);
    }

    @Test
    public void shouldRunTests() throws Exception {
        Set<String> validMethods = new HashSet<>();
        validMethods.add("testSuccess");
        validMethods.add("testFail");
        validMethods.add("testException");

        new Tester("TesterTest$TestValid", new Tester.ResultListener() {
            @Override
            public void onTestStart(Method method) {}

            @Override
            public void onTestWarning(Method method, String reason) {}

            @Override
            public void onTestFinished(Method method, boolean success) {
                validMethods.remove(method.getName());
            }

            @Override
            public void onTestFinished(Method method, boolean success,
                                       Throwable exception) {
                validMethods.remove(method.getName());
            }

            @Override
            public void onAllTestsFinished(int successes,
                                           int failedWithoutException,
                                           int failedFromException) {}

            @Override
            public void onNonTestException(Throwable exception) {}
        }).run();
        assertEquals(0, validMethods.size());
    }

    @Test
    public void shouldGiveCorrectTestResult() throws Exception {
        boolean[] failed = new boolean[] {false, false};
        new Tester("TesterTest$TestValid", new Tester.ResultListener() {
            @Override
            public void onTestStart(Method method) {}

            @Override
            public void onTestWarning(Method method, String reason) {}

            @Override
            public void onTestFinished(Method method, boolean success) {
                if (method.getName().equals("testSuccess")
                        && success != true) {
                    failed[0] = true;
                } else if (method.getName().equals("testFail")
                        && success != false) {
                    failed[1] = true;
                }
            }

            @Override
            public void onTestFinished(Method method, boolean success,
                                       Throwable exception) {}

            @Override
            public void onAllTestsFinished(int successes,
                                           int failedWithoutException,
                                           int failedFromException) {}

            @Override
            public void onNonTestException(Throwable exception) {}
        }).run();

        if (failed[0]) {
            fail("Wrong return value for testSuccess");
        }
        if (failed[1]) {
            fail("Wrong return value for testFail");
        }
    }

    @Test
    public void shouldGiveTestException() throws Exception {
        boolean[] failed = new boolean[] {false};
        new Tester("TesterTest$TestValid", new Tester.ResultListener() {
            @Override
            public void onTestStart(Method method) {}

            @Override
            public void onTestWarning(Method method, String reason) {}

            @Override
            public void onTestFinished(Method method, boolean success) {
                if (method.getName().equals("testException")) {
                    failed[0] = true;
                }
            }

            @Override
            public void onTestFinished(Method method, boolean success,
                                       Throwable exception) {
                if (!method.getName().equals("testException")
                        || exception == null) {

                    failed[0] = true;
                }
            }

            @Override
            public void onAllTestsFinished(int successes,
                                           int failedWithoutException,
                                           int failedFromException) {}

            @Override
            public void onNonTestException(Throwable exception) {}
        }).run();

        if (failed[0]) {
            fail("Failed to give exception from testException");
        }
    }

    @Test
    public void shouldNotRunIncorrectTestMethodResultType() throws Exception {
        boolean[] failed = new boolean[] {false};
        new Tester("TesterTest$TestValid", new Tester.ResultListener() {
            @Override
            public void onTestStart(Method method) {}

            @Override
            public void onTestWarning(Method method, String reason) {}

            @Override
            public void onTestFinished(Method method, boolean success) {
                if (method.getName().equals("testWrongReturnType")) {
                    failed[0] = true;
                }
            }

            @Override
            public void onTestFinished(Method method, boolean success,
                                       Throwable exception) {}

            @Override
            public void onAllTestsFinished(int successes,
                                           int failedWithoutException,
                                           int failedFromException) {}

            @Override
            public void onNonTestException(Throwable exception) {}
        }).run();

        if (failed[0]) {
            fail("Should not have run: testWrongReturnType");
        }
    }

    @Test
    public void shouldNotRunIncorrectTestMethodNotPublic() throws Exception {
        boolean[] failed = new boolean[] {false};
        new Tester("TesterTest$TestValid", new Tester.ResultListener() {
            @Override
            public void onTestStart(Method method) {}

            @Override
            public void onTestWarning(Method method, String reason) {}

            @Override
            public void onTestFinished(Method method, boolean success) {
                if (method.getName().equals("testNonPublic")) {
                    failed[0] = true;
                }
            }

            @Override
            public void onTestFinished(Method method, boolean success,
                                       Throwable exception) {}

            @Override
            public void onAllTestsFinished(int successes,
                                           int failedWithoutException,
                                           int failedFromException) {}

            @Override
            public void onNonTestException(Throwable exception) {}
        }).run();

        if (failed[0]) {
            fail("Should not have run: testNonPublic");
        }
    }

    @Test
    public void shouldNotRunIncorrectTestMethodParameters() throws Exception {
        boolean[] failed = new boolean[] {false};
        new Tester("TesterTest$TestValid", new Tester.ResultListener() {
            @Override
            public void onTestStart(Method method) {}

            @Override
            public void onTestWarning(Method method, String reason) {}

            @Override
            public void onTestFinished(Method method, boolean success) {
                if (method.getName().equals("testParameters")) {
                    failed[0] = true;
                }
            }

            @Override
            public void onTestFinished(Method method, boolean success,
                                       Throwable exception) {}

            @Override
            public void onAllTestsFinished(int successes,
                                           int failedWithoutException,
                                           int failedFromException) {}

            @Override
            public void onNonTestException(Throwable exception) {}
        }).run();

        if (failed[0]) {
            fail("Should not have run: testParameters");
        }
    }

    @Test
    public void shouldNotRunIncorrectTestMethodName() throws Exception {
        boolean[] failed = new boolean[] {false};
        new Tester("TesterTest$TestValid", new Tester.ResultListener() {
            @Override
            public void onTestStart(Method method) {}

            @Override
            public void onTestWarning(Method method, String reason) {}

            @Override
            public void onTestFinished(Method method, boolean success) {
                if (method.getName().equals("invalidName")) {
                    failed[0] = true;
                }
            }

            @Override
            public void onTestFinished(Method method, boolean success,
                                       Throwable exception) {}

            @Override
            public void onAllTestsFinished(int successes,
                                           int failedWithoutException,
                                           int failedFromException) {}

            @Override
            public void onNonTestException(Throwable exception) {}
        }).run();

        if (failed[0]) {
            fail("Should not have run: invalidName");
        }
    }
}