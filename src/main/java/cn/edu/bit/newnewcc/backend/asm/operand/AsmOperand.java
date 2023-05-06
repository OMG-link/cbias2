package cn.edu.bit.newnewcc.backend.asm.operand;

public abstract class AsmOperand {
    TYPE type;

    //汇编语言中的操作数，包含立即数、寄存器、浮点寄存器、全局标记（全局变量以标记的形式存储，实际取变量所在地址）和栈上变量
    public enum TYPE {
        IMM,
        REG,
        FREG,
        GTAG,
        SVAR
    }

    public boolean isGlobalTag() {
        return type == TYPE.GTAG;
    }

    public boolean isImmediate() {
        return type == TYPE.IMM;
    }

    public boolean isRegister() {
        return type == TYPE.REG;
    }

    public boolean isFloatReg() {
        return type == TYPE.FREG;
    }

    public boolean isStackVar() {
        return type == TYPE.SVAR;
    }

    public AsmOperand(TYPE type) {
        this.type = type;
    }

    abstract public String emit();
}
