package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.type.VoidType;
import com.bit.newnewcc.ir.value.BasicBlock;
import com.bit.newnewcc.ir.value.Instruction;

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

    /**
     * @return 该语句可能跳转到的所有基本块
     */
    public abstract Collection<BasicBlock> getExits();
}
