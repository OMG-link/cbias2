package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

/**
 * 汇编加指令，分为普通加和加立即数两种
 */
public class AsmAdd extends AsmBinaryInstruction {
    /**
     * 汇编加指令
     *
     * @param dest    结果存储的寄存器
     * @param source1 源寄存器1，存储第一个加数
     * @param source2 源2，存储第二个加数，可能为寄存器或立即数
     */
    public AsmAdd(IntRegister dest, IntRegister source1, AsmOperand source2) {
        super("add", dest, source1, source2);
        if (source2.isImmediate() || source2.isLabel() || source2.isAddressDirective()) {
            setInstructionName("addi");
        }
    }
    public AsmAdd(IntRegister dest, IntRegister source1, AsmOperand source2, int bitLength) {
        super("add", dest, source1, source2);
        if (bitLength == 32) {
            setInstructionName("addw");
        }
        if (source2.isImmediate() || source2.isLabel() || source2.isAddressDirective()) {
            setInstructionName("addi");
            if (bitLength == 32) {
                setInstructionName("addiw");
            }
        }
    }
    public AsmAdd(FloatRegister dest, FloatRegister source1, FloatRegister source2) {
        super("fadd.s", dest, source1, source2);
    }
}
