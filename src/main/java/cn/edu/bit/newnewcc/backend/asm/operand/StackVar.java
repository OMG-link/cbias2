package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 栈变量在汇编中的表现完全为栈帧寄存器s0+地址偏移量的形式组成，但需要存储变量在栈上所占据的大小
 */
public class StackVar extends AsmOperand {

    Address address;
    int size;

    /**
     * 创建一个栈上变量
     *
     * @param offset 变量在栈帧中的偏移量
     * @param size   变量占据的大小
     */
    public StackVar(int offset, int size) {
        super(TYPE.SVAR);
        this.address = new Address(offset, new IntRegister("s0"));
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String emit() {
        return address.emit();
    }
}
