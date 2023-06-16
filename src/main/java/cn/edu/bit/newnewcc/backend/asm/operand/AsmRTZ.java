package cn.edu.bit.newnewcc.backend.asm.operand;

public class AsmRTZ extends AsmOperand {
    public AsmRTZ() {
        super(TYPE.NON);
    }
    public String emit() {
        return "rtz";
    }
}
