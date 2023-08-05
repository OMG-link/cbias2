package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 逻辑右移指令
 */
public class AsmShiftRightLogical extends AsmBinaryInstruction {
    public AsmShiftRightLogical(IntRegister rd, IntRegister rs1, AsmOperand shiftLength, int bitLength) {
        super("srl", rd, rs1, shiftLength);
        if (shiftLength instanceof Immediate) {
            setInstructionName("srli");
        } else if (!(shiftLength instanceof IntRegister)) {
            throw new RuntimeException("shift operand not register or immediate");
        }
        if (bitLength == 32) {
            setInstructionName(getInstructionName() + "w");
        }
    }
}
