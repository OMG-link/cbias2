package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.Set;

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
        return String.format("AsmLabel(%s)", getLabel());
    }

    @Override
    public String emit() {
        return String.format("%s:\n", getLabel().getLabelName());
    }

    @Override
    public Set<Register> getDef() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Integer> getUse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean willReturn() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean mayWriteToMemory() {
        throw new UnsupportedOperationException();
    }
}
