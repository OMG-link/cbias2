package cn.edu.bit.newnewcc.backend.asm.operand;

import java.util.HashMap;
import java.util.Map;

//riscv的浮点寄存器为f0~f31
public class FloatRegister extends Register {
    /**
     * 生成指定下标的浮点寄存器
     */
    private FloatRegister(int index) {
        super(index, Type.FLOAT);
    }

    private static final Map<Integer, FloatRegister> PHYSICAL_REGISTERS = new HashMap<>();

    public static FloatRegister getVirtual(int index) {
        return new FloatRegister(-index);
    }

    public static FloatRegister getPhysical(int index) {
        if (!PHYSICAL_REGISTERS.containsKey(index)) {
            PHYSICAL_REGISTERS.put(index, new FloatRegister(index));
        }
        return PHYSICAL_REGISTERS.get(index);
    }

    public static FloatRegister getParameter(int index) {
        return getPhysical(index + 10);
    }

    public String emit() {
        if (getIndex() >= 0) {
            return "f" + getAbsoluteIndex();
        } else {
            return "vf" + (-getAbsoluteIndex());
        }
    }
}
