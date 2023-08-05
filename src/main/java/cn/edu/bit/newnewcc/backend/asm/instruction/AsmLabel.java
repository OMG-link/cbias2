package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.util.Others;

/**
 * 标签也被视为一类指令
 */
public class AsmLabel extends AsmInstruction {
    public AsmLabel(Label label) {
        super(label.labelExpression(), null, null, null);
    }

    public String getPureName() {
        return Others.deleteCharString(emit(), ":.\n\t ");
    }
}
