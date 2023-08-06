package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 逻辑右移指令
 */
public class AsmShiftRightLogical extends AsmInstruction {
    public enum Opcode {
        SRL("srl"),
        SRLI("srli"),
        SRLW("srlw"),
        SRLIW("srliw");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    public AsmShiftRightLogical(IntRegister dest, IntRegister source1, AsmOperand source2, int bitLength) {
        super(dest, source1, source2);

        if (bitLength != 64 && bitLength != 32)
            throw new IllegalArgumentException();

        if (!(source2 instanceof IntRegister) && !(source2 instanceof Immediate))
            throw new IllegalArgumentException();

        if (bitLength == 32) {
            if (source2 instanceof Immediate) opcode = Opcode.SRLIW;
            else opcode = Opcode.SRLW;
        } else {
            if (source2 instanceof Immediate) opcode = Opcode.SRLI;
            else opcode = Opcode.SRL;
        }
    }

    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public String toString() {
        return String.format("%s %s, %s, %s", getOpcode().getName(), getOperand(1), getOperand(2), getOperand(3));
    }

    @Override
    public String emit() {
        return "\t" + this + "\n";
    }
}
