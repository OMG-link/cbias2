package com.bit.newnewcc.ir.exception;

public class IllegalBitWidthException extends CompileProcessCheckFailException{
    public IllegalBitWidthException() {
    }

    public IllegalBitWidthException(String message) {
        super(message);
    }

    public IllegalBitWidthException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalBitWidthException(Throwable cause) {
        super(cause);
    }
}
