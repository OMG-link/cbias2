package cn.edu.bit.newnewcc.backend.asm.operand;

public abstract class Address extends AsmOperand {
    protected final long offset;
    protected final IntRegister baseAddress;
    Address(TYPE type, long offset, IntRegister baseAddress) {
        super(type);
        this.offset = offset;
        this.baseAddress = baseAddress;
    }

    public Address replaceBaseRegister(IntRegister newBaseRegister) {
        if (isAddressContent()) {
            return new AddressContent(offset, newBaseRegister);
        } else {
            return new AddressTag(offset, newBaseRegister);
        }
    }

    public Address addOffset(long offsetDiff) {
        if (isAddressContent()) {
            return new AddressContent(offset + offsetDiff, baseAddress);
        } else {
            return new AddressTag(offset + offsetDiff, baseAddress);
        }
    }

    public AddressContent getAddressContent() {
        return new AddressContent(offset, baseAddress);
    }

    public AddressTag getAddressTag() {
        return new AddressTag(offset, baseAddress);
    }

    public String emit() {
        return String.format("%d(%s)", offset, baseAddress.emit());
    }

    public long getOffset() {
        return offset;
    }

    public Register getRegister() {
        return baseAddress;
    }
}
