package cn.edu.bit.newnewcc.backend.asm.operand;

public abstract class Register extends AsmOperand implements RegisterReplaceable {
    String name;
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
        this.name = null;
        this.index = index;
        this.rtype = type;
    }

    Register(String name, RTYPE type) {
        super(TYPE.REG);
        this.name = name;
        this.index = 0;
        this.rtype = type;
    }

    public abstract Register replaceIndex(int index);

    public boolean isVirtual() {
        return name == null && index < 0;
    }

    /**
     * 获取寄存器下标的**绝对值**
     */
    public int getIndex() {
        return Math.abs(index);
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Register register = (Register) o;

        if (index != register.index) return false;
        return rtype == register.rtype;
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + rtype.hashCode();
        return result;
    }
}
