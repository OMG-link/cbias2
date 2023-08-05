package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmShiftLeft extends AsmBinaryInstruction {
    public AsmShiftLeft(IntRegister rd, IntRegister rs1, AsmOperand shiftLength, int bitLength) {
        super("sll", rd, rs1, shiftLength);
        if (shiftLength instanceof Immediate) {
            setInstructionName("slli");
        } else if (!(shiftLength instanceof IntRegister)) {
            throw new RuntimeException("shift operand not register or immediate");
        }
        if (bitLength == 32) {
            setInstructionName(getInstructionName() + "w");
        }
    }
}
