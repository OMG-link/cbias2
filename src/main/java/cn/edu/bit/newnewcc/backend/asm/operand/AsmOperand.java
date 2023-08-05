package cn.edu.bit.newnewcc.backend.asm.operand;

public abstract class AsmOperand {
    TYPE type;

    public boolean isLabel() {
        return type == TYPE.LABEL;
    }

    public boolean isAddressContent() {
        return type == TYPE.ADDC;
    }

    public boolean isAddressDirective() { return type == TYPE.ADDD;}

    public boolean isStackVar() {
        return type == TYPE.SVAR;
    }

    //汇编语言中的操作数，包含立即数、寄存器、全局标记
    // （全局变量以标记的形式存储，实际取变量所在地址）、地址和栈变量
    protected enum TYPE {
        IMM,
        REG,
        ADDC,
        ADDD,
        LABEL,
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

    @Override
    public boolean equals(Object v) {
        return v instanceof AsmOperand op && type == op.type && emit().equals(op.emit());
    }

    @Override
    public String toString() {
        return emit();
    }
}
