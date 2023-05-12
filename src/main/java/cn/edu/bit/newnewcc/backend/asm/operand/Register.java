package cn.edu.bit.newnewcc.backend.asm.operand;

public abstract class Register extends AsmOperand {
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

    Register(int index, RTYPE type) {
        super(TYPE.REG);
        this.name = null;
        this.index = index;
        this.rtype = type;
    }

    Register(String name, RTYPE type) {
        super(TYPE.REG);
        this.name = name;
        this.index = -1;
        this.rtype = type;
    }
}
