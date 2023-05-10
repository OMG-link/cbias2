package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * riscv的普通寄存器为x0~x31
 */
public class Register extends AsmOperand {
    private final int index;
    private final String name;

    /**
     * 生成指定下标的寄存器
     *
     * @param index 下标，下标为负数时代表其为暂时未分配的普通临时寄存器，等待分配过程
     */
    public Register(int index) {
        super(TYPE.REG);
        this.index = index;
        this.name = null;
    }

    /**
     * 生成指定名称的寄存器
     *
     * @param name 寄存器名称
     */
    public Register(String name) {
        super(TYPE.REG);
        this.name = name;
        this.index = -1;
    }

    public String emit() {
        if (index >= 0) {
            return "x" + index;
        } else {
            return name;
        }
    }

}
