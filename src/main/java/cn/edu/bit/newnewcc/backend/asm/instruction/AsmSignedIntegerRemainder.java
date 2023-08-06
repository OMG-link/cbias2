package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 有符号整数求余数运算
 */
public class AsmSignedIntegerRemainder extends AsmInstruction {
    public enum Opcode {
        REM("rem"),
        REMW("remw");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    public AsmSignedIntegerRemainder(IntRegister dest, IntRegister source1, IntRegister source2, int bitLength) {
        super(dest, source1, source2);

        if (bitLength != 32 && bitLength != 64)
            throw new IllegalArgumentException();

        if (bitLength == 32) opcode = Opcode.REMW;
        else opcode = Opcode.REM;
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
