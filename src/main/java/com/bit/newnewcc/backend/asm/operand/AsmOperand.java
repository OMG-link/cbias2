package com.bit.newnewcc.backend.asm.operand;

public abstract class AsmOperand {
    //汇编语言中的操作数，包含立即数、寄存器、浮点寄存器、全局变量和栈上变量
    public enum TYPE {
        IMM,
        REG,
        FREG,
        GVAR,
        SVAR
    }
    TYPE type;

    public boolean isImmediate() { return type == TYPE.IMM; }
    public boolean isRegister() { return type == TYPE.REG; }
    public boolean isFloatReg() { return type == TYPE.FREG; }
    public boolean isGlobalVar() { return type == TYPE.GVAR; }
    public boolean isStackVar() { return type == TYPE.SVAR; }

    public AsmOperand(TYPE type) {
        this.type = type;
    }

    abstract public String emit();
}
