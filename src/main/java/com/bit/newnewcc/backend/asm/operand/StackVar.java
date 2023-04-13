package com.bit.newnewcc.backend.asm.operand;

//栈上变量，被表示为栈帧寄存器+地址偏移量的形式，如-32(s0), 24(sp)

public class StackVar extends AsmOperand {
    private final int offset;
    private final Register baseAddress;

    public StackVar(int offset, Register baseAddress) {
        super(TYPE.SVAR);
        this.offset = offset;
        this.baseAddress = baseAddress;
    }

    public String emit() {
        return String.format("%d(%s)", offset, baseAddress.emit());
    }
}
