package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 地址操作数，被表示为基址+偏移量的形式
 */
public class Address extends AsmOperand {
    private final int offset;
    private final IntRegister baseAddress;

    /**
     * 新建一个以寄存器存储的地址为基址，偏移量为立即数的栈变量
     *
     * @param offset      偏移量
     * @param baseAddress 基址寄存器
     */
    public Address(int offset, IntRegister baseAddress) {
        super(TYPE.ADDR);
        this.offset = offset;
        this.baseAddress = baseAddress;
    }

    public String emit() {
        return String.format("%d(%s)", offset, baseAddress.emit());
    }
}
