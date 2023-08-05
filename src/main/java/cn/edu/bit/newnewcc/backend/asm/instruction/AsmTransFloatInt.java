package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmTransFloatInt extends AsmInstruction {
    public enum Opcode {
        FCVTWS("fcvt.w.s"),
        FCVTSW("fcvt.s.w");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    public AsmTransFloatInt(IntRegister dest, FloatRegister source) {
        super("", dest, source, null);
        opcode = Opcode.FCVTWS;
    }

    public AsmTransFloatInt(FloatRegister dest, IntRegister source) {
        super("", dest, source, null);
        opcode = Opcode.FCVTSW;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public String emit() {
        if (opcode == Opcode.FCVTWS)
            return String.format("\t%s %s, %s, rtz\n", getOpcode().getName(), getOperand(1), getOperand(2));
        else
            return String.format("\t%s %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2));
    }
}
