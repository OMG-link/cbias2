package cn.edu.bit.newnewcc.backend.asm.operand;

public abstract class AsmOperand {

    abstract public String emit();

    @Override
    public boolean equals(Object o) {
        return o instanceof AsmOperand op && getClass() == op.getClass() && emit().equals(op.emit());
    }

    @Override
    public String toString() {
        return emit();
    }
}
