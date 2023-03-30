package com.bit.newnewcc.ir.exception;

public class UseRelationCheckFailException extends CompileProcessCheckFailException{
    public UseRelationCheckFailException() {
    }

    public UseRelationCheckFailException(String message) {
        super(message);
    }

    public UseRelationCheckFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public UseRelationCheckFailException(Throwable cause) {
        super(cause);
    }
}
