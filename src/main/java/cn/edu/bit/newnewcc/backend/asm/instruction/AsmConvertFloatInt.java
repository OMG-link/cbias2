package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.Set;

public class AsmConvertFloatInt extends AsmInstruction {
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

    public AsmConvertFloatInt(IntRegister dest, FloatRegister source) {
        super(dest, source, null);
        opcode = Opcode.FCVTWS;
    }

    public AsmConvertFloatInt(FloatRegister dest, IntRegister source) {
        super(dest, source, null);
        opcode = Opcode.FCVTSW;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public String toString() {
        return switch (getOpcode()) {
            case FCVTWS -> String.format("%s %s, %s, rtz", getOpcode().getName(), getOperand(1), getOperand(2));
            case FCVTSW -> String.format("%s %s, %s", getOpcode().getName(), getOperand(1), getOperand(2));
        };
    }

    @Override
    public String emit() {
        return "\t" + this + "\n";
    }

    @Override
    public Set<Register> getDef() {
        return Set.of((Register) getOperand(1));
    }

    @Override
    public Set<Integer> getUse() {
        return Set.of(2);
    }
}
