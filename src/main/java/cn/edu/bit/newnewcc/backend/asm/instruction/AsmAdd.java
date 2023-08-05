package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

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
        super("", dest, source1, source2);

        if (bitLength != 64 && bitLength != 32)
            throw new IllegalArgumentException();

        if (source2.isImmediate() || source2.isLabel() || source2.isAddressDirective()) {
            if (bitLength == 32) opcode = Opcode.ADDIW;
            else opcode = Opcode.ADDI;
        } else {
            if (bitLength == 32) opcode = Opcode.ADDW;
            else opcode = Opcode.ADD;
        }
    }
    public AsmAdd(FloatRegister dest, FloatRegister source1, FloatRegister source2) {
        super("", dest, source1, source2);

        opcode = Opcode.FADDS;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public String emit() {
        return String.format("\t%s %s, %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2), getOperand(3));
    }
}
