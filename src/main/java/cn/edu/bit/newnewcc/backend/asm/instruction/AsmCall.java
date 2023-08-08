package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.util.Registers;
import cn.edu.bit.newnewcc.ir.exception.IndexOutOfBoundsException;

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
    public void setOperand(int index, AsmOperand operand) {
        if (index == 1) throw new UnsupportedOperationException();
        throw new IndexOutOfBoundsException();
    }

    @Override
    public String toString() {
        if (getReturnRegister() != null) {
            return String.format("AsmCall(%s, %s->%s)", getOperand(1), getParamRegList(), getReturnRegister());
        } else {
            return String.format("AsmCall(%s, %s)", getOperand(1), getParamRegList());
        }
    }

    @Override
    public String emit() {
        return String.format("\tcall %s\n", getOperand(1).emit());
    }

    @Override
    public Set<Register> getDef() {
        return Registers.CALLER_SAVED_REGISTERS;
    }

    @Override
    public Set<Register> getUse() {
        return Set.copyOf(getParamRegList());
    }

    @Override
    public boolean willReturn() {
        return true;
    }

    @Override
    public boolean mayWriteToMemory() {
        return true;
    }
}
