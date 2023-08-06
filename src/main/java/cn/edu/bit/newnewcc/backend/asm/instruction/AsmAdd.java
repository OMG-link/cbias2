package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.*;

/**
 * 汇编加指令，分为普通加和加立即数两种
 */
public class AsmAdd extends AsmInstruction {
    public enum Opcode {
        ADD("add"),
        ADDI("addi"),
        ADDW("addw"),
        ADDIW("addiw"),
        FADDS("fadd.s");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    public AsmAdd(IntRegister dest, IntRegister source1, AsmOperand source2, int bitLength) {
        super(dest, source1, source2);

        if (bitLength != 64 && bitLength != 32)
            throw new IllegalArgumentException();

        if (source2 instanceof Immediate || source2 instanceof Label || source2 instanceof AddressDirective) {
            if (bitLength == 32) opcode = Opcode.ADDIW;
            else opcode = Opcode.ADDI;
        } else {
            if (bitLength == 32) opcode = Opcode.ADDW;
            else opcode = Opcode.ADD;
        }
    }
    public AsmAdd(FloatRegister dest, FloatRegister source1, FloatRegister source2) {
        super(dest, source1, source2);

        opcode = Opcode.FADDS;
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
