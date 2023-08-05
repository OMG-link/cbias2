package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 汇编乘法指令，仅支持寄存器间的乘法
 */
public class AsmMul extends AsmBinaryInstruction {
    public enum Opcode {
        MUL("mul"),
        MULW("mulw"),
        FMULS("fmul.s");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    public AsmMul(IntRegister dest, IntRegister source1, IntRegister source2, int bitLength) {
        super("", dest, source1, source2);

        if (bitLength != 64 && bitLength != 32)
            throw new IllegalArgumentException();

        if (bitLength == 32) opcode = Opcode.MULW;
        else opcode = Opcode.MUL;
    }
    public AsmMul(FloatRegister dest, FloatRegister source1, FloatRegister source2) {
        super("", dest, source1, source2);

        opcode = Opcode.FMULS;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    @Override
    public String emit() {
        return String.format("\t%s %s, %s, %s\n", getOpcode().getName(), getOperand(1), getOperand(2), getOperand(3));
    }
}
