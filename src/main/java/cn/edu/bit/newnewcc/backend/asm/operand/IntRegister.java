package cn.edu.bit.newnewcc.backend.asm.operand;

import java.util.HashMap;
import java.util.Map;

/**
 * riscv的普通寄存器为x0~x31
 */
public class IntRegister extends Register {

    /**
     * 生成指定下标的寄存器
     *
     * @param index 下标，下标为负数时代表其为暂时未分配的普通临时寄存器，等待分配过程
     */
    private IntRegister(int index) {
        super(index, Type.INT);
    }

    private static final Map<Integer, IntRegister> PHYSICAL_REGISTERS = new HashMap<>();
    public static final IntRegister ZERO = getPhysical(0);
    public static final IntRegister RA = getPhysical(1);
    public static final IntRegister SP = getPhysical(2);
    public static final IntRegister S0 = getPhysical(8);
    public static final IntRegister S1 = getPhysical(9);

    public static IntRegister getVirtual(int index) {
        return new IntRegister(-index);
    }

    public static IntRegister getPhysical(int index) {
        if (!PHYSICAL_REGISTERS.containsKey(index)) {
            PHYSICAL_REGISTERS.put(index, new IntRegister(index));
        }
        return PHYSICAL_REGISTERS.get(index);
    }

    public static IntRegister getParameter(int index) {
        return getPhysical(index + 10);
    }

    public String emit() {
        if (isVirtual()) {
            return "vx" + getAbsoluteIndex();
        } else {
            return "x" + (getAbsoluteIndex());
        }
    }
}
