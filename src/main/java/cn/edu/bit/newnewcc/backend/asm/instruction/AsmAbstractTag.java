package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.util.Others;

public abstract class AsmAbstractTag extends AsmInstruction{
    public AsmAbstractTag(String instructionName, AsmOperand operand1, AsmOperand operand2, AsmOperand operand3) {
        super(instructionName, operand1, operand2, operand3);
    }
    public String getPureName() {
        return Others.deleteCharString(emit(), ":.\n\t ");
    }
}
