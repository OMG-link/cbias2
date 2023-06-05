package cn.edu.bit.newnewcc.backend.asm.operand;

/**
 * 栈变量内容，专用于栈帧重新分配过程.
 */
public class ExStackVarContent extends StackVar implements ExStackVarAdd {
    boolean isS0Based;
    ExStackVarContent(Address address, int size, boolean isS0Based) {
        super(address, size, isS0Based);
    }
    public static ExStackVarContent transform(StackVar stackVar) {
        return new ExStackVarContent(stackVar.getAddress(), stackVar.getSize(), stackVar.isS0Based());
    }
    public static ExStackVarContent transform(StackVar stackVar, Address address) {
        return new ExStackVarContent(address, stackVar.getSize(), stackVar.isS0Based());
    }

    @Override
    public AsmOperand add(int diff) {
        if (isS0Based()) {
            return new ExStackVarContent(getAddress().addOffset(diff), getSize(), true);
        } else {
            return this;
        }
    }
}
