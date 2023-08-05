package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmJump extends AsmInstruction {
    public enum Opcode {
        J("j"),
        BNEZ("bnez"),
        JR("jr");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    private AsmJump(Opcode opcode, AsmOperand op1, AsmOperand op2, AsmOperand op3) {
        super("", op1, op2, op3);
        this.opcode = opcode;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public String emit() {
        return switch (getOpcode()) {
            case J, JR -> String.format("\t%s %s\n", getOpcode().getName(), getOperand(1));
            case BNEZ -> String.format("\t%s %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2));
        };
    }

    public static AsmJump J(Label targetLabel) {
        return new AsmJump(Opcode.J, targetLabel, null, null);
    }

    public static AsmJump BNEZ(Label targetLabel, IntRegister source) {
        return new AsmJump(Opcode.BNEZ, source, targetLabel, null);
    }

    public static AsmJump JR(IntRegister addressRegister) {
        return new AsmJump(Opcode.JR, addressRegister, null, null);
    }
}
