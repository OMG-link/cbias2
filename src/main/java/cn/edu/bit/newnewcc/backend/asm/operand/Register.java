package cn.edu.bit.newnewcc.backend.asm.operand;

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
}
