package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.Set;

public class AsmIndirectJump extends AsmInstruction {
    public AsmIndirectJump(IntRegister addressRegister) {
        super(addressRegister, null, null);
    }

    @Override
    public String toString() {
        return String.format("jr %s", getOperand(1));
    }

    @Override
    public String emit() {
        return "\t" + this + "\n";
    }

    @Override
    public Set<Register> getDef() {
        return Set.of();
    }

    @Override
    public Set<Integer> getUse() {
        return Set.of(1);
    }
}
