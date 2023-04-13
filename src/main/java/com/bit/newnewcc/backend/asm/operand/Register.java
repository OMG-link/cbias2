package com.bit.newnewcc.backend.asm.operand;

//riscv的普通寄存器为x0~x31
public class Register extends AsmOperand {
    private final int index;

    public Register(int index) {
        super(TYPE.REG);
        this.index = index;
    }

    public String emit() {
        return "x" + index;
    }

}
