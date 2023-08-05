package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.util.Others;

/**
 * 标签也被视为一类指令
 */
public class AsmLabel extends AsmInstruction {
    private final Label label;

    public AsmLabel(Label label) {
        super("", null, null, null);
        this.label = label;
    }

    public Label getLabel() {
        return label;
    }

    public String getPureName() {
        return Others.deleteCharString(emit(), ":.\n\t ");
    }

    @Override
    public String emit() {
        return String.format("%s:\n", getLabel().getLabelName());
    }
}
