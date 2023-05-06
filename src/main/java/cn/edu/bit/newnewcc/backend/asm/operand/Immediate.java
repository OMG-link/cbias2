package cn.edu.bit.newnewcc.backend.asm.operand;

//riscv的立即数即为普通的32位整数表示
public class Immediate extends AsmOperand {
    private int value;

    public Immediate(int value) {
        super(TYPE.IMM);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String emit() {
        return String.valueOf(value);
    }
}
