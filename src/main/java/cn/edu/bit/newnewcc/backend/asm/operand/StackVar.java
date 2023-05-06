package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 栈上变量，被表示为栈帧寄存器+地址偏移量的形式，如-32(s0), 24(sp)
 */
public class StackVar extends AsmOperand {
    private final int offset;
    private final Register baseAddress;

    /**
     * 新建一个以寄存器存储的地址为基址，偏移量为立即数的栈变量
     *
     * @param offset      偏移量
     * @param baseAddress 基址寄存器
     */
    public StackVar(int offset, Register baseAddress) {
        super(TYPE.SVAR);
        this.offset = offset;
        this.baseAddress = baseAddress;
    }

    public String emit() {
        return String.format("%d(%s)", offset, baseAddress.emit());
    }
}
