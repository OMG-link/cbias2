package cn.edu.bit.newnewcc.backend.asm.operand;

import java.util.*;

public abstract class Register extends AsmOperand implements RegisterReplaceable {
    //寄存器在调用过程中保留与否，保留的寄存器需要在函数头尾额外保存
    protected int index;
    private final RTYPE rtype;

    public enum RTYPE {
        INT, FLOAT
    }

    public boolean isInt() {
        return rtype == RTYPE.INT;
    }

    public boolean isFloat() {
        return rtype == RTYPE.FLOAT;
    }

    public RTYPE getType() {
        return rtype;
    }

    Register(int index, RTYPE type) {
        super(TYPE.REG);
        this.index = index;
        this.rtype = type;
    }

    public boolean isVirtual() {
        return index < 0;
    }

    /**
     * 获取寄存器下标的**绝对值**
     */
    public int getIndex() {
        return Math.abs(index);
    }

    /**
     * 仅在虚拟寄存器复制合并时使用！！！
     * @param idx 下标
     */
    public void setIndex(int idx) {
        this.index = idx;
    }

    @Override
    public Register getRegister() {
        return this;
    }

    @Override
    public Register replaceRegister(Register register) {
        return register;
    }

    public static Collection<Register> getUsableRegisters() {
        Set<Register> res = new HashSet<>();
        for (int i = 0; i <= 31; i++) {
            if ((5 <= i && i <= 7) || (18 <= i)) {
                res.add(IntRegister.getPhysical(i));
            }
            if (i <= 9 || 18 <= i) {
                res.add(FloatRegister.getPhysical(i));
            }
        }
        return res;
    }

    private enum PTYPE {
        PRESERVED, UNPRESERVED
    }

    public static final Map<Register, PTYPE> registerPreservedType = new HashMap<>();
    private static void initPreservedType() {
        if (registerPreservedType.isEmpty()) {
            for (int i = 0; i <= 31; i++) {
                if ((i == 2) || (8 <= i && i <= 9) || (18 <= i && i <= 27)) {
                    registerPreservedType.put(IntRegister.getPhysical(i), PTYPE.PRESERVED);
                } else {
                    registerPreservedType.put(IntRegister.getPhysical(i), PTYPE.UNPRESERVED);
                }
                if ((8 <= i && i <= 9) || (18 <= i && i <= 27)) {
                    registerPreservedType.put(FloatRegister.getPhysical(i), PTYPE.PRESERVED);
                } else {
                    registerPreservedType.put(FloatRegister.getPhysical(i), PTYPE.UNPRESERVED);
                }
            }
        }
    }

    public boolean isPreserved() {
        initPreservedType();
        return registerPreservedType.get(this) == PTYPE.PRESERVED;
    }
}
