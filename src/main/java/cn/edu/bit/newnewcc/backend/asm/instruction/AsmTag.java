package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;
import cn.edu.bit.newnewcc.backend.asm.util.Others;

/**
 * 标签也被视为一类指令
 */
public class AsmTag extends AsmInstruction {
    public AsmTag(GlobalTag tag) {
        super(tag.tagExpress(), null, null, null);
    }

    public String getPureName() {
        return Others.deleteCharString(emit(), ":.\n\t ");
    }
}
