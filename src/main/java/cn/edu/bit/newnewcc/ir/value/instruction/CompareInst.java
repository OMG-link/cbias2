package cn.edu.bit.newnewcc.ir.value.instruction;

import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.type.IntegerType;

public abstract class CompareInst extends BinaryInstruction {

    private final Type operandType;

    public CompareInst(Type operandType) {
        super(IntegerType.getI1(), operandType, operandType);
        this.operandType = operandType;
    }

    @Override
    public String toString() {
        return String.format(
                "%s = %s %s %s, %s",
                this.getValueNameIR(),
                this.getInstName(),
                this.operandType.getTypeName(),
                getOperand1().getValueNameIR(),
                getOperand2().getValueNameIR()
        );
    }
}
