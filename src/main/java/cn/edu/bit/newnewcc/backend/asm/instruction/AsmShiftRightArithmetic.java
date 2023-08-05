package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 逻辑右移指令
 */
public class AsmShiftRightArithmetic extends AsmInstruction {
    public enum Opcode {
        SRA("sra"),
        SRAI("srai"),
        SRAW("sraw"),
        SRAIW("sraiw");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    public AsmShiftRightArithmetic(IntRegister dest, IntRegister source1, AsmOperand source2, int bitLength) {
        super(dest, source1, source2);

        if (bitLength != 64 && bitLength != 32)
            throw new IllegalArgumentException();

        if (!(source2 instanceof IntRegister) && !(source2 instanceof Immediate))
            throw new IllegalArgumentException();

        if (bitLength == 32) {
            if (source2 instanceof Immediate) opcode = Opcode.SRAIW;
            else opcode = Opcode.SRAW;
        } else {
            if (source2 instanceof Immediate) opcode = Opcode.SRAI;
            else opcode = Opcode.SRA;
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
