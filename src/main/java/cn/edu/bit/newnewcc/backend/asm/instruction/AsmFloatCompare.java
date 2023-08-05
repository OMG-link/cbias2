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

    private final Opcode opcode;

    private AsmFloatCompare(Opcode opcode, IntRegister dest, FloatRegister source1, FloatRegister source2) {
        super("", dest, source1, source2);
        this.opcode = opcode;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public String emit() {
        return String.format("\t%s %s, %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2), getOperand(3));
    }

    public static AsmFloatCompare FEQS(IntRegister dest, FloatRegister source1, FloatRegister source2) {
        return new AsmFloatCompare(Opcode.FEQS, dest, source1, source2);
    }

    public static AsmFloatCompare FLTS(IntRegister dest, FloatRegister source1, FloatRegister source2) {
        return new AsmFloatCompare(Opcode.FLTS, dest, source1, source2);
    }

    public static AsmFloatCompare FLES(IntRegister dest, FloatRegister source1, FloatRegister source2) {
        return new AsmFloatCompare(Opcode.FLES, dest, source1, source2);
    }
}
