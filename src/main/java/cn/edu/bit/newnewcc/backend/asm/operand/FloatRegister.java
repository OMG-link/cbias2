package cn.edu.bit.newnewcc.backend.asm.operand;

import java.util.HashMap;
import java.util.Map;

//riscv的浮点寄存器为f0~f31
public class FloatRegister extends Register {
    /**
     * 生成指定下标的浮点寄存器
     */
    private FloatRegister(int index) {
        super(index, RTYPE.FLOAT);
    }

    static Map<Integer, FloatRegister> physicalRegisters = new HashMap<>();

    public static FloatRegister getVirtual(int index) {
        return new FloatRegister(-index);
    }

    public static FloatRegister getPhysical(int index) {
        if (!physicalRegisters.containsKey(index)) {
            physicalRegisters.put(index, new FloatRegister(index));
        }
        return physicalRegisters.get(index);
    }

    public static FloatRegister getParameter(int index) {
        return getPhysical(index + 10);
    }

    public String emit() {
        if (index >= 0) {
            return "f" + index;
        } else {
            return "VRegFloat" + (-index);
        }
    }
}

