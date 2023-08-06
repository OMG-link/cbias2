package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.Set;

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

        if (bitLength == 64) {
            if (source2 instanceof Immediate) opcode = Opcode.SRAI;
            else opcode = Opcode.SRA;
        } else {
            if (source2 instanceof Immediate) opcode = Opcode.SRAIW;
            else opcode = Opcode.SRAW;
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

    @Override
    public Set<Register> getDef() {
        return Set.of((Register) getOperand(1));
    }

    @Override
    public Set<Integer> getUse() {
        return switch (getOpcode()) {
            case SRA, SRAW -> Set.of(2, 3);
            case SRAI, SRAIW -> Set.of(2);
        };
    }
}
