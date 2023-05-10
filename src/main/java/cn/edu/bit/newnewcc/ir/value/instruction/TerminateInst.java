package cn.edu.bit.newnewcc.ir.value.instruction;

import cn.edu.bit.newnewcc.ir.type.VoidType;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.Collection;

/**
 * 基本块终止语句
 */
public abstract class TerminateInst extends Instruction {

    public TerminateInst() {
        super(VoidType.getInstance());
    }

    @Override
    public VoidType getType() {
        return (VoidType) super.getType();
    }

    @Override
    public String getValueName() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return 该语句可能跳转到的所有基本块
     */
    public abstract Collection<BasicBlock> getExits();
}
