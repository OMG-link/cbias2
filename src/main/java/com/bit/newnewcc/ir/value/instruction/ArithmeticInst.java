package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Type;

public abstract class ArithmeticInst extends BinaryInstruction {
    public ArithmeticInst(Type operandType) {
        super(operandType, operandType, operandType);
    }

    @Override
    public String toString() {
        return String.format(
                "%s = %s %s %s, %s",
                this.getValueNameIR(),
                this.getInstName(),
                this.getTypeName(),
                getOperand1().getValueNameIR(),
                getOperand2().getValueNameIR()
        );
    }
}
