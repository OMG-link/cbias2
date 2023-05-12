package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmJump extends AsmInstruction{
    /**
     * 跳转条件，目前阶段无需条件跳转，因此只有不等于0的时候跳转和无条件跳转
     */
    public enum JUMPTYPE {
        NON,
        NEZ
    }
    private GlobalTag goalTag;

    /**
     * 基本跳转指令，按照条件向指定位置跳转
     *
     * @param goalTag 跳转的目标位置
     * @param type 跳转条件的类型
     * @param operand1 跳转条件参数1
     * @param operand2 跳转条件参数2
     */
    public AsmJump(GlobalTag goalTag, JUMPTYPE type, IntRegister operand1, IntRegister operand2) {
        super("", null, null, null);
        this.goalTag = goalTag;
        if (type == JUMPTYPE.NON) {
            setInstructionName("j");
            setOperand1(goalTag);
        } else if (type == JUMPTYPE.NEZ) {
            setInstructionName("bnez");
            setOperand1(operand1);
            setOperand2(goalTag);
        }
    }

    /**
     * 寄存器跳转指令
     * @param addressRegister 存储这目标地址的寄存器
     */
    public AsmJump(IntRegister addressRegister) {
        super("jr", addressRegister, null, null);
    }

}
