package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 汇编加指令，分为普通加和加立即数两种
 */
public class AsmSub extends AsmInstruction {
    public enum Opcode {
        SUB("sub"),
        SUBW("subw"),
        FSUBS("fsub.s");

        private final String name;

        Opcode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final Opcode opcode;

    public AsmSub(IntRegister dest, IntRegister source1, IntRegister source2, int bitLength) {
        super(dest, source1, source2);

        if (bitLength != 64 && bitLength != 32)
            throw new IllegalArgumentException();

        if (bitLength == 32) opcode = Opcode.SUBW;
        else opcode = Opcode.SUB;
    }
    public AsmSub(FloatRegister dest, FloatRegister source1, FloatRegister source2) {
        super(dest, source1, source2);
        opcode = Opcode.FSUBS;
    }

    @Override
    public String emit() {
        return String.format("\t%s %s, %s, %s\n", opcode.getName(), getOperand(1), getOperand(2), getOperand(3));
    }
}
