package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 地址标记类，类似于指针，通常在指令中被视为立即数，可以和地址类之间互相转换
 */
public class AddressDirective extends Address {
    public AddressDirective(long offset, IntRegister baseAddress) {
        super(offset, baseAddress);
    }

    @Override
    public Address replaceBaseRegister(IntRegister newBaseRegister) {
        return new AddressDirective(offset, newBaseRegister);
    }

    @Override
    public Address addOffset(long offsetDiff) {
        return new AddressDirective(offset + offsetDiff, baseAddress);
    }

    @Override
    public Address setOffset(long newOffset) {
        return new AddressDirective(newOffset, baseAddress);
    }

    @Override
    public String toString() {
        return String.format("AddressDirective(%s, %s)\n", getOffset(), getBaseAddress());
    }
}
