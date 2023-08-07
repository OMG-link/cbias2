package cn.edu.bit.newnewcc.backend.asm.operand;

public abstract class Register extends AsmOperand implements RegisterReplaceable {
    public enum Type {
        INT, FLOAT
    }

    public abstract int getIndex();

    /**
     * 仅在虚拟寄存器复制合并时使用！！！
     * @param index 下标
     */
    public abstract void setIndex(int index);

    /**
     * 获取寄存器下标的**绝对值**
     */
    public int getAbsoluteIndex() {
        return Math.abs(getIndex());
    }

    public boolean isVirtual() {
        return getIndex() < 0;
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
