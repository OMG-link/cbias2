package com.bit.newnewcc.backend.asm.operand;

//riscv的浮点寄存器为f0~f31
public class FloatRegister extends AsmOperand {
    private final int index;

    public FloatRegister(int index) {
        super(TYPE.FREG);
        this.index = index;
    }

    public String emit() {
        return "f" + index;
    }
}

