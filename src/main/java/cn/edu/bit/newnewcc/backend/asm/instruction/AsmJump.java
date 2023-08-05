package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmJump extends AsmInstruction {
    /**
     * 跳转条件，目前阶段无需条件跳转，因此只有不等于0的时候跳转和无条件跳转
     */
    public enum JUMPTYPE {
        UNCONDITIONAL,
        NEZ
    }

    /**
     * 基本跳转指令，按照条件向指定位置跳转
     *
     * @param targetLabel  跳转的目标位置
     * @param type     跳转条件的类型
     * @param source1 跳转条件参数1
     * @param source2 跳转条件参数2
     */
    public AsmJump(Label targetLabel, JUMPTYPE type, IntRegister source1, IntRegister source2) {
        super("", null, null, null);
        if (type == JUMPTYPE.UNCONDITIONAL) {
            setInstructionName("j");
            setOperand1(targetLabel);
        } else if (type == JUMPTYPE.NEZ) {
            setInstructionName("bnez");
            setOperand1(source1);
            setOperand2(targetLabel);
        }
    }

    /**
     * 寄存器跳转指令
     *
     * @param addressRegister 存储这目标地址的寄存器
     */
    public AsmJump(IntRegister addressRegister) {
        super("jr", addressRegister, null, null);
    }

    public boolean isUnconditional() {
        return getInstructionName().equals("j");
    }
}
