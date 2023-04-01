package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.type.IntegerType;

public abstract class CompareInst extends BinaryInstruction {
    public CompareInst(Type operandType) {
        super(IntegerType.getI1(), operandType, operandType);
    }
}
