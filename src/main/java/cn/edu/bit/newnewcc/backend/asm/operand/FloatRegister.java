package cn.edu.bit.newnewcc.backend.asm.operand;

//riscv的浮点寄存器为f0~f31
public class FloatRegister extends Register {
    /**
     * 生成指定下标的浮点寄存器
     */
    public FloatRegister(int index) {
        super(index, RTYPE.FLOAT);
    }

    /**
     * 生成指定名称的浮点寄存器
     */
    public FloatRegister(String name) {
        super(name, RTYPE.FLOAT);
    }

    public String emit() {
        if (name != null) {
            return name;
        } else {
            if (index >= 0) {
                return "f" + index;
            } else {
                return "VRegFloat" + (-index);
            }
        }
    }
}

