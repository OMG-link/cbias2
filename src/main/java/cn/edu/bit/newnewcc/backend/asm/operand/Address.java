package cn.edu.bit.newnewcc.backend.asm.operand;

public abstract class Address extends AsmOperand implements RegisterReplaceable {
    protected final long offset;
    protected final IntRegister baseAddress;
    protected Address(long offset, IntRegister baseAddress) {
        this.offset = offset;
        this.baseAddress = baseAddress;
    }

    public abstract Address replaceBaseRegister(IntRegister newBaseRegister);

    public abstract Address addOffset(long offsetDiff);

    public abstract Address setOffset(long newOffset);

    public AddressContent getAddressContent() {
        return new AddressContent(offset, baseAddress);
    }

    public AddressDirective getAddressDirective() {
        return new AddressDirective(offset, baseAddress);
    }

    public String emit() {
        return String.format("%d(%s)", offset, baseAddress.emit());
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public IntRegister getRegister() {
        return baseAddress;
    }

    @Override
    public Address replaceRegister(Register register) {
        if (register instanceof IntRegister intRegister) {
            return replaceBaseRegister(intRegister);
        } else {
            throw new RuntimeException("put float register into address");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Address address = (Address) o;

        if (offset != address.offset) return false;
        return baseAddress.equals(address.baseAddress);
    }

    @Override
    public int hashCode() {
        int result = (int) (offset ^ (offset >>> 32));
        result = 31 * result + baseAddress.hashCode();
        return result;
    }
}
