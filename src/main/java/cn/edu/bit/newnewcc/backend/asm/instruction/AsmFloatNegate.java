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
        return String.format("fneg.s %s, %s", getOperand(1), getOperand(2));
    }

    @Override
    public String emit() {
        return "\t" + this + "\n";
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
