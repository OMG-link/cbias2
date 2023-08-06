package cn.edu.bit.newnewcc.backend.asm.operand;

public abstract class Register extends AsmOperand implements RegisterReplaceable {
    //寄存器在调用过程中保留与否，保留的寄存器需要在函数头尾额外保存
    private int index;
    private final Type type;

    public enum Type {
        INT, FLOAT
    }

    public boolean isInt() {
        return type == Type.INT;
    }

    public boolean isFloat() {
        return type == Type.FLOAT;
    }

    public Type getType() {
        return type;
    }

    Register(int index, Type type) {
        this.index = index;
        this.type = type;
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
