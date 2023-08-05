package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmJump extends AsmInstruction {
    public enum Opcode {
        J("j"),
        BNEZ("bnez");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public enum Condition {
        UNCONDITIONAL,
        NEZ
    }

    private final Opcode opcode;
    private final Condition condition;

    private AsmJump(Opcode opcode, Condition condition, AsmOperand op1, AsmOperand op2, AsmOperand op3) {
        super("", op1, op2, op3);
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
        return switch (getOpcode()) {
            case J -> String.format("\t%s %s\n", getOpcode().getName(), getOperand(1));
            case BNEZ -> String.format("\t%s %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2));
        };
    }

    public static AsmJump createUnconditional(Label targetLabel) {
        return new AsmJump(Opcode.J, Condition.UNCONDITIONAL, targetLabel, null, null);
    }

    public static AsmJump createNEZ(Label targetLabel, IntRegister source) {
        return new AsmJump(Opcode.BNEZ, Condition.NEZ, source, targetLabel, null);
    }
}
