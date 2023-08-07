package cn.edu.bit.newnewcc.backend.asm.operand;

public class Address extends AsmOperand implements RegisterReplaceable {
    private final long offset;
    private final IntRegister baseAddress;

    public Address(long offset, IntRegister baseAddress) {
        this.offset = offset;
        this.baseAddress = baseAddress;
    }

    public Address withBaseRegister(IntRegister newBaseRegister) {
        return new Address(getOffset(), newBaseRegister);
    }

    public Address addOffset(long diff) {
        return new Address(getOffset() + diff, getBaseAddress());
    }

    public Address setOffset(long newOffset) {
        return new Address(newOffset, getBaseAddress());
    }

    public Address getAddress() {
        return new Address(getOffset(), getBaseAddress());
    }

    public long getOffset() {
        return offset;
    }

    public IntRegister getBaseAddress() {
        return baseAddress;
    }

    @Override
    public IntRegister getRegister() {
        return getBaseAddress();
    }

    @Override
    public Address replaceRegister(Register register) {
        if (!(register instanceof IntRegister))
            throw new IllegalArgumentException();

        return withBaseRegister((IntRegister) register);
    }

    public String emit() {
        return String.format("%d(%s)", getOffset(), getBaseAddress().emit());
    }

    @Override
    public String toString() {
        return String.format("Address(%s, %s)", getOffset(), getBaseAddress());
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
