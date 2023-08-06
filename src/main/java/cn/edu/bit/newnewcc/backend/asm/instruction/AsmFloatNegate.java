package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.Set;

public class AsmFloatNegate extends AsmInstruction {
    public AsmFloatNegate(FloatRegister dest, FloatRegister source) {
        super(dest, source, null);
    }

    @Override
    public String toString() {
        return String.format("AsmFloatNegate(%s, %s)", getOperand(1), getOperand(2));
    }

    @Override
    public String emit() {
        return String.format("\tfneg.s %s, %s\n", getOperand(1).emit(), getOperand(2).emit());
    }

    @Override
    public Set<Register> getDef() {
        return Set.of((Register) getOperand(1));
    }

    @Override
    public Set<Integer> getUse() {
        return Set.of(2);
    }
}
