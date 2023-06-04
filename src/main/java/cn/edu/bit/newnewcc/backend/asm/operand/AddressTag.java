package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 地址标记类，类似于指针，通常在指令中被视为立即数，可以和地址类之间互相转换
 */
public class AddressTag extends AsmOperand {
    private final long offset;
    private final IntRegister baseAddress;
    public AddressTag(long offset, IntRegister baseAddress) {
        super(TYPE.ADDT);
        this.offset = offset;
        this.baseAddress = baseAddress;
    }
    public Address getAddress() {
        return new Address(offset, baseAddress);
    }
    public String emit() {
        return String.format("%d(%s)", offset, baseAddress.emit());
    }
}
