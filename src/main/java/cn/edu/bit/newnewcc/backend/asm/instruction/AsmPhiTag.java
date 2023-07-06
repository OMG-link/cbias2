package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;
import cn.edu.bit.newnewcc.backend.asm.util.Others;

public class AsmPhiTag extends AsmInstruction {
    AsmTag sourceBlockTag;
    public AsmPhiTag(AsmTag goalBlockTag, AsmTag sourceBlockTag) {
        super((".phi_tag_" + Others.getTagName(goalBlockTag) + "_" + Others.getTagName(sourceBlockTag)) + ":", null, null, null);
        this.sourceBlockTag = sourceBlockTag;
    }

    public AsmTag getSourceBlockTag() {
        return sourceBlockTag;
    }
    public GlobalTag getInnerTag() {
        return new GlobalTag(Others.deleteCharString(this.emit(), ":.\n\t "), false);
    }
}
