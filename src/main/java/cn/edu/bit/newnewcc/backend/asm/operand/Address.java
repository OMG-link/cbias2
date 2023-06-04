package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 地址操作数，被表示为基址+偏移量的形式
 */
public class Address extends AsmOperand {
    private final long offset;
    private final IntRegister baseAddress;

    /**
     * 新建一个以寄存器存储的地址为基址，偏移量为立即数的栈变量
     *
     * @param offset      偏移量
     * @param baseAddress 基址寄存器
     */
    public Address(long offset, IntRegister baseAddress) {
        super(TYPE.ADDR);
        this.offset = offset;
        this.baseAddress = baseAddress;
    }

    public long getOffset() {
        return offset;
    }

    public Register getRegister() {
        return baseAddress;
    }

    public String emit() {
        return String.format("%d(%s)", offset, baseAddress.emit());
    }

    AddressTag getAddressTag() {
        return new AddressTag(offset, baseAddress);
    }
}
