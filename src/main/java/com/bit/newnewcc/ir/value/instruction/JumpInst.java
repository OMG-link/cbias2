package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.LabelType;
import com.bit.newnewcc.ir.value.BasicBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 无条件跳转语句
 */
public class JumpInst extends TerminateInst{
    private final Operand exitOperand;

    public JumpInst(){
        this(null);
    }
    /**
     * @param exit 跳转的目标基本块
     */
    public JumpInst(BasicBlock exit) {
        this.exitOperand = new Operand(this, LabelType.getInstance(),exit);
    }

    public BasicBlock getExit() {
        return (BasicBlock) exitOperand.getValue();
    }

    public void setExit(BasicBlock exit) {
        exitOperand.setValue(exit);
    }

    @Override
    public String toString() {
        return String.format("br label %s",getExit().getValueName());
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(exitOperand);
        return list;
    }

    @Override
    public Collection<BasicBlock> getExits() {
        var list = new ArrayList<BasicBlock>();
        list.add(getExit());
        return list;
    }

}
