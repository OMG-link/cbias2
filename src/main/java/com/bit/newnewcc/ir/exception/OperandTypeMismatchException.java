package com.bit.newnewcc.ir.exception;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.value.Instruction;

public class OperandTypeMismatchException extends CompilationProcessCheckFailedException {
    public OperandTypeMismatchException() {
    }

    public OperandTypeMismatchException(String message) {
        super(message);
    }

    public OperandTypeMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperandTypeMismatchException(Throwable cause) {
        super(cause);
    }

    public OperandTypeMismatchException(Instruction context, Type expectedType, Type actualType){
        this(String.format("In instruction %s, expected type %s, got type %s",context,expectedType.getTypeName(),actualType.getTypeName()));
    }
}
