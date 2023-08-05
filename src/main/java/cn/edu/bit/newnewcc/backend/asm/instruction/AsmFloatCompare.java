package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmFloatCompare extends AsmInstruction{
    public enum Opcode {
        FEQS("feq.s"),
        FLTS("flt.s"),
        FLES("fle.s");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum Condition {
        EQ, LT, LE
    }

    private final Opcode opcode;
    private final Condition condition;

    private AsmFloatCompare(Opcode opcode, Condition condition, IntRegister dest, FloatRegister source1, FloatRegister source2) {
        super(dest, source1, source2);
        this.opcode = opcode;
        this.condition = condition;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public String emit() {
        return String.format("\t%s %s, %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2), getOperand(3));
    }

    public static AsmFloatCompare createEQ(IntRegister dest, FloatRegister source1, FloatRegister source2) {
        return new AsmFloatCompare(Opcode.FEQS, Condition.EQ, dest, source1, source2);
    }

    public static AsmFloatCompare createLT(IntRegister dest, FloatRegister source1, FloatRegister source2) {
        return new AsmFloatCompare(Opcode.FLTS, Condition.LT, dest, source1, source2);
    }

    public static AsmFloatCompare createLE(IntRegister dest, FloatRegister source1, FloatRegister source2) {
        return new AsmFloatCompare(Opcode.FLES, Condition.LE, dest, source1, source2);
    }
}
