package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 有符号整数求余数运算
 */
public class AsmSignedIntegerRemainder extends AsmBinaryInstruction {
    public AsmSignedIntegerRemainder(IntRegister goal, IntRegister rsource1, IntRegister rsource2, int bitLength) {
        super("rem", goal, rsource1, rsource2);
        if (bitLength == 32) {
            setInstructionName("remw");
        }
    }
}
