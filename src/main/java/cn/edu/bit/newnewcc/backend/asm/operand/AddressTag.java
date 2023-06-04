package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 地址标记类，类似于指针，通常在指令中被视为立即数，可以和地址类之间互相转换
 */
public class AddressTag extends Address {
    public AddressTag(long offset, IntRegister baseAddress) {
        super(TYPE.ADDT, offset, baseAddress);
    }
}
