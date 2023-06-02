package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 汇编加指令，分为普通加和加立即数两种
 */
public class AsmSub extends AsmBinaryInstruction {
    /**
     * 汇编加指令
     *
     * @param goal    结果存储的寄存器
     * @param source1 源寄存器1，存储被减数
     * @param source2 源2，存储减数，可能为寄存器或立即数
     */
    public AsmSub(IntRegister goal, IntRegister source1, AsmOperand source2) {
        super("sub", goal, source1, source2);
        if (source2.isImmediate()) {
            setInstructionName("addi");
            setOperand3(new Immediate(-((Immediate)source2).getValue()));
        }
    }
}
