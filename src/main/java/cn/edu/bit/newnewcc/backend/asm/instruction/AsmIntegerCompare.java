package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmIntegerCompare extends AsmInstruction {
    public enum Opcode {
        SEQZ("seqz"),
        SNEZ("snez"),
        SLT("slt"),
        SLTI("slti");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum Condition {
        EQZ, NEZ, LT
    }

    private final Opcode opcode;
    private final Condition condition;

    private AsmIntegerCompare(Opcode opcode, Condition condition, IntRegister dest, AsmOperand source1, AsmOperand source2) {
        super("", dest, source1, source2);
        this.opcode = opcode;
        this.condition = condition;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public Condition getCondition() {
        return condition;
    }

    public static AsmIntegerCompare createEQZ(IntRegister dest, IntRegister source) {
        return new AsmIntegerCompare(Opcode.SEQZ, Condition.EQZ, dest, source, null);
    }

    public static AsmIntegerCompare createNEZ(IntRegister dest, IntRegister source) {
        return new AsmIntegerCompare(Opcode.SNEZ, Condition.NEZ, dest, source, null);
    }

    @Override
    public String emit() {
        return switch (getOpcode()) {
            case SEQZ, SNEZ -> String.format("\t%s %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2));
            case SLT, SLTI -> String.format("\t%s %s, %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2), getOperand(3));
        };
    }

    public static AsmIntegerCompare createLT(IntRegister dest, IntRegister source1, IntRegister source2) {
        return new AsmIntegerCompare(Opcode.SLT, Condition.LT, dest, source1, source2);
    }

    public static AsmIntegerCompare createLT(IntRegister dest, IntRegister source1, Immediate source2) {
        return new AsmIntegerCompare(Opcode.SLTI, Condition.LT, dest, source1, source2);
    }
}
