package com.group_finity.mascot.exception;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
// TODO 学习当这类异常被捕获时，要如何确认一些事情，比如mascot是否要被废弃, 因为不是每个实例都会造成这种情况。
public class CantBeAliveException extends Exception {
    public CantBeAliveException(final String message) {
        super(message);
    }

    public CantBeAliveException(final String message, final Throwable cause) {
        super(message, cause);
    }
}