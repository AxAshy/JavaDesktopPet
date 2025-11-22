package com.group_finity.mascot.exception;

public class ActionInstantiationException extends Exception {
    public ActionInstantiationException(final String message) {
        super(message);
    }

    public ActionInstantiationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
