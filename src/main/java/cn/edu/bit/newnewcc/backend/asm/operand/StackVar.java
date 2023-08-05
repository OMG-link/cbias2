package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 栈变量在汇编中的表现完全为栈帧寄存器s0+地址偏移量的形式组成，但需要存储变量在栈上所占据的大小
 */
public class StackVar extends AsmOperand implements RegisterReplaceable {

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
            this.address = new AddressContent(offset, IntRegister.s0);
        } else {
            this.address = new AddressContent(offset, IntRegister.sp);
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

    /**
     * 注意，当拆解StackVar时，务必转换为ExStackVarOffset的形式，禁止暴露栈帧的裸地址，避免偏移计算出错
     * @return 返回栈变量对应地址
     */
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

    @Override
    public StackVar replaceRegister(Register register) {
        return new StackVar(address.replaceRegister(register), size, isS0);
    }

    @Override
    public Register getRegister() {
        return getAddress().getRegister();
    }

    @Override
    public boolean equals(Object v) {
        return v instanceof StackVar stackVar && size == stackVar.getSize() && emit().equals(stackVar.emit());
    }
}
