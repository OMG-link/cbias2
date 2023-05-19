package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * riscv的普通寄存器为x0~x31
 */
public class IntRegister extends Register {

    /**
     * 生成指定下标的寄存器
     *
     * @param index 下标，下标为负数时代表其为暂时未分配的普通临时寄存器，等待分配过程
     */
    public IntRegister(int index) {
        super(index, RTYPE.INT);
    }

    /**
     * 生成指定名称的寄存器
     *
     * @param name 寄存器名称
     */
    public IntRegister(String name) {
        super(name, RTYPE.INT);
    }

    public String emit() {
        if (name != null) {
            return name;
        } else {
            if (index >= 0) {
                return "x" + index;
            } else {
                return "VRegInt" + (-index);
            }
        }
    }
}
