package cn.edu.bit.newnewcc.backend.asm.operand;

//riscv的浮点寄存器为f0~f31
public class FloatRegister extends AsmOperand {
    private final int index;
    private final String name;

    /**
     * 生成指定下标的浮点寄存器
     */
    public FloatRegister(int index) {
        super(TYPE.FREG);
        this.index = index;
        this.name = null;
    }

    /**
     * 生成指定名称的浮点寄存器
     */
    public FloatRegister(String name) {
        super(TYPE.FREG);
        this.name = name;
        this.index = -1;
    }

    public String emit() {
        if (index >= 0) {
            return "f" + index;
        } else {
            return name;
        }
    }
}

