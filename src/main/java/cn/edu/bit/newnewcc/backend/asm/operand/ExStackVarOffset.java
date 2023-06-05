package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 栈偏移量，专用于栈帧重新分配的过程.
 */
public class ExStackVarOffset extends Immediate implements ExStackVarAdd {
    boolean isS0Based;
    ExStackVarOffset(int val, boolean isS0Based) {
        super(val);
        this.isS0Based = isS0Based;
    }
    public static ExStackVarOffset transform(StackVar stackVar, long value) {
        return new ExStackVarOffset(Math.toIntExact(value), stackVar.isS0Based());
    }

    @Override
    public AsmOperand add(int diff) {
        if (isS0Based) {
            return new ExStackVarOffset(getValue() + diff, true);
        } else {
            return this;
        }
    }
}
