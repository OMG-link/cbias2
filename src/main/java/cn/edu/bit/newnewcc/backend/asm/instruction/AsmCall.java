package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.util.Registers;

import java.util.List;
import java.util.Set;

public class AsmCall extends AsmInstruction {
    private final List<Register> paramRegList;
    private final Register returnRegister;
    public AsmCall(Label label, List<Register> paramRegList, Register returnRegister) {
        super(label, null, null);
        this.paramRegList = paramRegList;
        this.returnRegister = returnRegister;
    }

    public List<Register> getParamRegList() {
        return paramRegList;
    }

    public Register getReturnRegister() {
        return returnRegister;
    }

    @Override
    public String toString() {
        return String.format("AsmCall(%s, (%s)->%s)", getOperand(1), getParamRegList(), getReturnRegister());
    }

    @Override
    public String emit() {
        return String.format("\tcall %s\n", getOperand(1).emit());
    }

    @Override
    public Set<Register> getDef() {
        return Registers.getCallerSavedRegisters();
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
    public boolean mayWriteToMemory() {
        throw new UnsupportedOperationException();
    }
}
