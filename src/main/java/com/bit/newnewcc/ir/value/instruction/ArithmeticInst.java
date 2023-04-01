package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Type;

public abstract class ArithmeticInst extends BinaryInstruction {
    public ArithmeticInst(Type operandType) {
        super(operandType, operandType, operandType);
    }
}
