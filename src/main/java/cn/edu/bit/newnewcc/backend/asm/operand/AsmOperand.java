package cn.edu.bit.newnewcc.backend.asm.operand;

public abstract class AsmOperand {
    TYPE type;

    public boolean isGlobalTag() {
        return type == TYPE.GTAG;
    }

    public boolean isAddressContent() {
        return type == TYPE.ADDC;
    }

    public boolean isAddressTag() { return type == TYPE.ADDT;}

    public boolean isStackVar() {
        return type == TYPE.SVAR;
    }

    //汇编语言中的操作数，包含立即数、寄存器、全局标记
    // （全局变量以标记的形式存储，实际取变量所在地址）、地址和栈变量
    public enum TYPE {
        IMM,
        REG,
        ADDC,
        ADDT,
        GTAG,
        SVAR,
        NON
    }

    public boolean isImmediate() {
        return type == TYPE.IMM;
    }

    public boolean isRegister() {
        return type == TYPE.REG;
    }

    public AsmOperand(TYPE type) {
        this.type = type;
    }

    abstract public String emit();
}
