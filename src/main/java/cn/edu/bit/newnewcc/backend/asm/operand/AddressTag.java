package cn.edu.bit.newnewcc.backend.asm.operand;

public class AddressTag extends AsmOperand {
    private final long offset;
    private final IntRegister baseAddress;
    public AddressTag(long offset, IntRegister baseAddress) {
        super(TYPE.ADDT);
        this.offset = offset;
        this.baseAddress = baseAddress;
    }
    Address getAddress() {
        return new Address(offset, baseAddress);
    }
    public String emit() {
        return String.format("%d(%s)", offset, baseAddress.emit());
    }
}
