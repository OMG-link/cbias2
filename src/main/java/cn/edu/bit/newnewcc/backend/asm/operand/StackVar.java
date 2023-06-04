package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 栈变量在汇编中的表现完全为栈帧寄存器s0+地址偏移量的形式组成，但需要存储变量在栈上所占据的大小
 */
public class StackVar extends AsmOperand {

    AddressContent addressContent;
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
            this.addressContent = new AddressContent(offset, new IntRegister("s0"));
        } else {
            this.addressContent = new AddressContent(offset, new IntRegister("sp"));
        }
        this.size = size;
    }

    public StackVar flip() {
        return new StackVar(this.addressContent.getOffset(), this.size, !this.isS0);
    }

    public AddressContent getAddress() {
        return this.addressContent;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String emit() {
        return addressContent.emit();
    }
}
