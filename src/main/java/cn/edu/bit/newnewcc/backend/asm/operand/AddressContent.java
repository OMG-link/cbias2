package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 地址操作数，被表示为基址+偏移量的形式
 */
public class AddressContent extends Address {
    /**
     * 新建一个以寄存器存储的地址为基址，偏移量为立即数的栈变量
     *
     * @param offset      偏移量
     * @param baseAddress 基址寄存器
     */
    public AddressContent(long offset, IntRegister baseAddress) {
        super(TYPE.ADDR, offset, baseAddress);
    }
}
