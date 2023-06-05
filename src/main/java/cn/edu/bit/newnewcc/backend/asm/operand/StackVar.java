package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 栈变量在汇编中的表现完全为栈帧寄存器s0+地址偏移量的形式组成，但需要存储变量在栈上所占据的大小
 */
public class StackVar extends AsmOperand {

    Address address;
    int size;
    boolean isS0;

    /**
     * 创建一个栈上变量
     *
     * @param offset 变量在栈帧中的偏移量
     * @param size   变量占据的大小
     */
    public StackVar(long offset, int size, boolean isS0) {
        super(TYPE.SVAR);
        this.isS0 = isS0;
        if (isS0) {
            this.address = new AddressContent(offset, new IntRegister("s0"));
        } else {
            this.address = new AddressContent(offset, new IntRegister("sp"));
        }
        this.size = size;
    }

    protected StackVar(Address address, int size, boolean isS0Based) {
        super(TYPE.SVAR);
        this.address = address;
        this.size = size;
        this.isS0 = isS0Based;
    }

    public boolean isS0Based() {
        return this.isS0;
    }

    public StackVar flip() {
        return new StackVar(this.address.getOffset(), this.size, !this.isS0);
    }

    public Address getAddress() {
        return this.address;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String emit() {
        return address.emit();
    }
}
