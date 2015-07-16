package com.codery.utils.cli;

/**
 * Created by Thomas on 7/9/2015.
 */
public class ShellCliException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 4143975241910293146L;

    public ShellCliException(String msg) {
        super(msg);
    }

    public ShellCliException(Throwable th) {
        super(th);
    }

    public ShellCliException(String msg, Throwable th) {
        super(msg, th);
    }
}
