package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.Label;

/**
 * 标签也被视为一类指令
 */
public class AsmLabel extends AsmInstruction {
    private final Label label;

    public AsmLabel(Label label) {
        super(null, null, null);
        this.label = label;
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return String.format("%s:", getLabel().getLabelName());
    }

    @Override
    public String emit() {
        return this + "\n";
    }
}
