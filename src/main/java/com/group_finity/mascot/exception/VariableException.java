package com.group_finity.mascot.exception;

public class VariableException extends Exception{
    public VariableException(final String message) {
        super(message);
    }

    public VariableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
