package cn.edu.bit.newnewcc.backend.asm.operand;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class Register extends AsmOperand implements RegisterReplaceable {
    int index;
    RTYPE rtype;

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
}
