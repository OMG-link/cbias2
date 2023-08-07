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
        return String.format("AsmIndirectJump(%s)", getOperand(1));
    }

    @Override
    public String emit() {
        return String.format("\tjr %s\n", getOperand(1).emit());
    }

    @Override
    public Set<Register> getDef() {
        return Set.of();
    }

    @Override
    public Set<Integer> getUse() {
        return Set.of(1);
    }

    @Override
    public boolean willReturn() {
        return false;
    }

    @Override
    public boolean mayWriteToMemory() {
        throw new UnsupportedOperationException();
    }
}
