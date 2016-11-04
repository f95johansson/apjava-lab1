/*
 * File: IllegalFormatException.java
 * Author: Fredrik Johansson
 * Date: 2016-10-29
 */

/**
 * Exception to be thrown when a TestClass has an invalid format,
 * i.e. not according to specifications.
 */
public class IllegalFormatException extends Exception {

    public IllegalFormatException() {
        super();
    }

    public IllegalFormatException(String message) {
        super(message);
    }

    public IllegalFormatException(Throwable cause) {
        super(cause);
    }

    public IllegalFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}