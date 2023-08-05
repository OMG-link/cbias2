package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmShiftLeft extends AsmInstruction {
    public enum Opcode {
        SLL("sll"),
        SLLI("slli"),
        SLLW("sllw"),
        SLLIW("slliw");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    public AsmShiftLeft(IntRegister dest, IntRegister source1, AsmOperand source2, int bitLength) {
        super("sll", dest, source1, source2);

        if (bitLength != 64 && bitLength != 32)
            throw new IllegalArgumentException();

        if (!(source2 instanceof IntRegister) && !(source2 instanceof Immediate))
            throw new IllegalArgumentException();

        if (bitLength == 32) {
            if (source2 instanceof Immediate) opcode = Opcode.SLLIW;
            else opcode = Opcode.SLLW;
        } else {
            if (source2 instanceof Immediate) opcode = Opcode.SLLI;
            else opcode = Opcode.SLL;
        }
    }

    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public String emit() {
        return String.format("\t%s %s, %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2), getOperand(3));
    }
}
