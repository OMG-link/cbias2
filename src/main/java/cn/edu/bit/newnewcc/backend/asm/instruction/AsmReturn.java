package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.Set;

public class AsmReturn extends AsmInstruction {
    public AsmReturn() {
        super(null, null, null);
    }

    @Override
    public String toString() {
        return "AsmReturn()";
    }

    @Override
    public String emit() {
        return "\tret\n";
    }

    @Override
    public Set<Register> getDef() {
        return Set.of();
    }

    @Override
    public Set<Register> getUse() {
        return Set.of();
    }

    @Override
    public boolean willReturn() {
        return false;
    }

    @Override
    public boolean mayHaveSideEffects() {
        throw new UnsupportedOperationException();
    }
}
