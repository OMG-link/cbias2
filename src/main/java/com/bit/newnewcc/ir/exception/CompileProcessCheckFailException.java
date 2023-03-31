package com.bit.newnewcc.ir.exception;

public class CompileProcessCheckFailException extends RuntimeException{
    public CompileProcessCheckFailException() {
    }

    public CompileProcessCheckFailException(String message) {
        super(message);
    }

    public CompileProcessCheckFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompileProcessCheckFailException(Throwable cause) {
        super(cause);
    }
}
