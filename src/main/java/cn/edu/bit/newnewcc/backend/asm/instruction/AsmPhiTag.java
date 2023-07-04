package cn.edu.bit.newnewcc.backend.asm.instruction;

public class AsmPhiTag extends AsmInstruction {
    AsmTag sourceBlockTag;
    public AsmPhiTag(String name, AsmTag sourceBlockTag) {
        super(name, null, null, null);
        this.sourceBlockTag = sourceBlockTag;
    }

    public AsmTag getSourceBlockTag() {
        return sourceBlockTag;
    }
}
