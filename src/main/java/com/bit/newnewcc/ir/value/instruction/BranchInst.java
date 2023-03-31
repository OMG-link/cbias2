package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.IntegerType;
import com.bit.newnewcc.ir.type.LabelType;
import com.bit.newnewcc.ir.type.VoidType;
import com.bit.newnewcc.ir.value.BasicBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 分支语句
 */
public class BranchInst extends TerminateInst{

    private final Operand conditionOperand;
    private final Operand trueExitOperand, falseExitOperand;

    /**
     * 构建一个空的分支语句
     */
    public BranchInst(){
        this(null,null,null);
    }

    /**
     * 构建一个分支语句
     * @param condition 分支条件
     * @param trueExit 条件为真时跳转的基本块
     * @param falseExit 条件为假时跳转的基本块
     */
    public BranchInst(Value condition, BasicBlock trueExit, BasicBlock falseExit){
        this.conditionOperand = new Operand(this, IntegerType.getI1(), condition);
        this.trueExitOperand = new Operand(this, LabelType.getInstance(), trueExit);
        this.falseExitOperand = new Operand(this, LabelType.getInstance(), falseExit);
    }

    public Value getCondition() {
        return conditionOperand.getValue();
    }

    public void setCondition(Value value) {
        conditionOperand.setValue(value);
    }

    public BasicBlock getTrueExit(){
        return (BasicBlock) trueExitOperand.getValue();
    }

    public void setTrueExit(BasicBlock trueExit){
        trueExitOperand.setValue(trueExit);
    }

    public BasicBlock getFalseExit(){
        return (BasicBlock) falseExitOperand.getValue();
    }

    public void setFalseExit(BasicBlock falseExit){
        falseExitOperand.setValue(falseExit);
    }

    @Override
    public String toString() {
        return String.format(
                "br i1 %s, label %s, label %s",
                getCondition().getValueName(),
                getTrueExit().getValueName(),
                getFalseExit().getValueName()
        );
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(conditionOperand);
        list.add(trueExitOperand);
        list.add(falseExitOperand);
        return list;
    }

    @Override
    public Collection<BasicBlock> getExits() {
        var list = new ArrayList<BasicBlock>();
        list.add(getTrueExit());
        list.add(getFalseExit());
        return list;
    }
}
