/*
 * File: Tester.java
 * Author: Fredrik Johansson
 * Date: 2016-10-28
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs a TestClass. All methods in a TestClass will be run as a test. If
 * method setUp exist, it will run before each test. If method tearDown exist,
 * it will run after each test.
 *
 * Model (logic) in MVC
 */
public class Tester implements Runnable {

    private Class<? extends TestClass> mTestClass;
    private Method mSetUp;
    private Method mTearDown;
    private ResultListener mResultListener;


    /**
     * Interface for the listener of the feedback from the test results
     */
    interface ResultListener {
        void onTestStart(Method method);
        void onTestFinished(Method method, boolean success);
        void onTestFinished(Method method, boolean success,
                            Throwable exception);
        void onTestWarning(Method method, String reason);
        void onAllTestsFinished(int successes,
                                int failedWithoutException,
                                int failedFromException);
        void onNonTestException(Throwable exception);
    }


    /**
     * Helper enum to keep track of results from tests
     */
    private enum TestResult {
        SUCCESS,
        FAILED_FROM_EXCEPTION,
        FAILED_WITHOUT_EXCEPTION
    }


    /**
     * Will assert if class and constructor is valid from the given class.
     *
     * @param testClassName Name of the class to run tests from
     * @param resultListener Listener for the results of the tests
     * @throws ClassNotFoundException No such class was found
     * @throws IllegalFormatException Class or constructor did not have valid
     *                                format according to specification
     */
    public Tester(String testClassName, ResultListener resultListener)
                        throws ClassNotFoundException, IllegalFormatException {

        assertClass(testClassName);
        assertConstructor();
        mResultListener = resultListener;
    }

    /**
     * See if class exists and implements TestClass
     * @param testClassName Name of class
     * @throws ClassNotFoundException No such class was found
     * @throws IllegalFormatException Class did implement TestClass
     */
    private void assertClass(String testClassName)
                        throws ClassNotFoundException, IllegalFormatException {
        try {
            mTestClass = Class.forName(testClassName)
                    .asSubclass(TestClass.class);

        } catch (ClassNotFoundException | NoClassDefFoundError cne) {
            throw new ClassNotFoundException("No such class: "+testClassName);
        } catch (ClassCastException cce) {
            throw new IllegalFormatException(
                    "Test class must implement interface TestClass");
        }
    }

    /**
     * See if constructor is valid according to specifications
     * @throws IllegalFormatException Multiple constructor, or it was not
     *                                public, or contained parameter
     */
    private void assertConstructor() throws IllegalFormatException {
        Constructor<?>[] constructors = mTestClass.getDeclaredConstructors();
        if (constructors.length < 1
                || constructors[0].getParameterCount() > 0
                || !Modifier.isPublic(constructors[0].getModifiers())) {
            throw new IllegalFormatException(
                    "Constructor must be public with no parameters");
        }
    }

    /**
     * Run all the test methods, with setUp/tearDown if they exists.
     * Will run each test in a separate thread.
     */
    public void run() {
        Method[] methods = extractMethods();

        Map<TestResult, Integer> resultsCount = new ConcurrentHashMap<>(3);
        resultsCount.put(TestResult.SUCCESS, 0);
        resultsCount.put(TestResult.FAILED_WITHOUT_EXCEPTION, 0);
        resultsCount.put(TestResult.FAILED_FROM_EXCEPTION, 0);

        // run max 6 tests at the same time
        ExecutorService executor = Executors.newFixedThreadPool(6);

        // Is used to know when all tests have finished
        CountDownLatch countDown = new CountDownLatch(methods.length);

        for (Method method : methods) {
            if (method.getName().startsWith("test")) {
                if (methodIsCorrectFormat(method, 0,
                                          boolean.class, Boolean.class)) {

                    mResultListener.onTestStart(method);
                    // Run on test on separate thread
                    executor.execute(() -> runTestCase(method,
                                                       resultsCount,
                                                       countDown));

                } else {
                    giveWarningFeedback(method);
                    countDown.countDown();
                }
            } else { // not prefix "test"
                countDown.countDown();
            }
        }

        try {
            countDown.await(); // wait for all tests to finish
                               // (for countDown to reach 0)
        } catch (InterruptedException e) {
            mResultListener.onNonTestException(e);
        }
        executor.shutdown();

        mResultListener.onAllTestsFinished(
                resultsCount.get(TestResult.SUCCESS),
                resultsCount.get(TestResult.FAILED_WITHOUT_EXCEPTION),
                resultsCount.get(TestResult.FAILED_FROM_EXCEPTION));
    }

    /**
     * Run a test method with setUp and tearDown.
     * @param method Method to run
     * @param resultsCount Used to sum up the total result
     * @param countDown To indicate how many method have been run
     */
    private void runTestCase(Method method,
                             Map<TestResult, Integer> resultsCount,
                             CountDownLatch countDown) {
        try {
            // new test object for each test for thread safety
            TestClass test = createTest();

            setUp(test);
            TestResult result = runTestMethod(test, method);
            countUpResult(result, resultsCount);
            tearDown(test);
        } catch (InstantiationException e) {
            mResultListener.onNonTestException(e);

        }
        countDown.countDown();
    }

    /**
     * Create a test object if possible
     * @return test object
     * @throws InstantiationException Could not run constructor
     */
    private TestClass createTest() throws InstantiationException {
        try {
            return mTestClass.newInstance();
        } catch (IllegalAccessException e) {
            // already tested to be public
            throw new IllegalStateException("No access to constructor even " +
                    "if it's public: "+mTestClass.getName());

        }
    }

    /**
     * Run a test method
     * @param test Test object to run method from
     * @param method Test method to run
     * @return Result enum which says how the test went
     */
    private TestResult runTestMethod(TestClass test, Method method) {
        try {
            Boolean returnValue = (Boolean) method.invoke(test);
            mResultListener.onTestFinished(method, returnValue);
            return returnValue ? TestResult.SUCCESS
                               : TestResult.FAILED_WITHOUT_EXCEPTION;

        } catch (IllegalAccessException ie) {
            // already tested to be public
            throw new IllegalStateException("No access to method " +
                    "even if it's public: "+method.getName());

        } catch (InvocationTargetException e) {
            mResultListener.onTestFinished(method, false,
                    e.getTargetException());
            return TestResult.FAILED_FROM_EXCEPTION;
        }
    }

    /**
     * Counts up the result type from a map
     * @param result TestResult
     * @param resultsCount Map of how all the tests have gone
     */
    private void countUpResult(TestResult result,
                               Map<TestResult, Integer> resultsCount) {
        resultsCount.compute(result, (r, count) -> count += 1);
    }

    /**
     * Extract all the methods from a class. Takes special consideration to
     * setUp and tearDown
     * @return All the methods
     */
    private Method[] extractMethods() {
        mSetUp = extractSpecialMethod("setUp", 0, void.class, Void.class);
        mTearDown = extractSpecialMethod("teardown", 0, void.class, Void.class);
        return mTestClass.getDeclaredMethods();
    }

    /**
     * Extract a method with a special signature
     * @param name Name of method
     * @param parameters How many parameters it takes
     * @param returnTypes What it returns, can be multiple options
     * @return The method
     */
    private Method extractSpecialMethod(String name, int parameters,
                                        Class... returnTypes) {
        Method method;
        try {
            Method special = mTestClass.getMethod(name);
            if (methodIsCorrectFormat(special, parameters, returnTypes)) {
                method = special;
            } else {
                method = null;
            }
        } catch (NoSuchMethodException e) {
            method = null;
        }
        return method;
    }

    /**
     * Runs the setUp method if possible/exists
     * @param test Test object to run method from
     */
    private void setUp(TestClass test) {
        if (mSetUp != null) {
            try {
                mSetUp.invoke(test);
            } catch (IllegalAccessException iae) {
                // already tested to be public
                throw new IllegalStateException("No access to method even " +
                        "if it's public: "+mSetUp.getName());
            } catch (InvocationTargetException ite) {
                throw new IllegalStateException("setUp method gave following " +
                        "exception: "+ite.getTargetException().getMessage());
            }
        }
    }

    /**
     * Runs the tearDown method if possible/exists
     * @param test Test object to run method from
     */
    private void tearDown(TestClass test) {
        if (mTearDown != null) {
            try {
                mTearDown.invoke(test);
            } catch (IllegalAccessException iae) {
                // already tested to be public
                throw new IllegalStateException("No access to method even " +
                        "if it's public: "+mTearDown.getName());
            } catch (InvocationTargetException ite) {
                throw new IllegalStateException(
                        "tearDown method gave following exception: "
                                + ite.getTargetException().getMessage());
            }
        }
    }

    /**
     * Check if method adhere to a special signature
     * @param method Method to examine
     * @param parameters How many parameters it takes
     * @param returnTypes What kind of return types is allowed
     * @return If all conditions are true or not
     */
    private boolean methodIsCorrectFormat(Method method, int parameters,
                                          Class... returnTypes) {
        return method.getParameterCount() == parameters
                && Modifier.isPublic(method.getModifiers())
                && Arrays.asList(returnTypes).contains(method.getReturnType());
    }

    /**
     * Give appropiet feedback on why a method did not run.
     * @param method Method which did not run
     */
    private void giveWarningFeedback(Method method) {
        StringBuilder builder = new StringBuilder("Did not run: ");
        if (method.getParameterCount() != 0) {
            builder.append("Method should have no parameters. ");
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            builder.append("Method must be public. ");
        }
        Class<?> returnType = method.getReturnType();
        if (!returnType.equals(boolean.class)
                && !returnType.equals(Boolean.class)) {
            builder.append("Method must return a boolean. ");
        }

        mResultListener.onTestWarning(method, builder.toString());
    }
}
