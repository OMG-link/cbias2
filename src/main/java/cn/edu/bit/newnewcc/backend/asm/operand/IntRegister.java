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
        super(index, RTYPE.INT);
    }

    final static Map<Integer, IntRegister> physicalRegisters = new HashMap<>();
    final public static IntRegister zero = getPhysical(0);
    final public static IntRegister ra = getPhysical(1);
    final public static IntRegister sp = getPhysical(2);
    final public static IntRegister s0 = getPhysical(8);
    final public static IntRegister s1 = getPhysical(9);

    public static IntRegister getVirtual(int index) {
        return new IntRegister(-index);
    }

    public static IntRegister getPhysical(int index) {
        if (!physicalRegisters.containsKey(index)) {
            physicalRegisters.put(index, new IntRegister(index));
        }
        return physicalRegisters.get(index);
    }

    public static IntRegister getParameter(int index) {
        return getPhysical(index + 10);
    }

    public String emit() {
        if (index >= 0) {
            return "x" + index;
        } else {
            return "VRegInt" + (-index);
        }
    }
}
