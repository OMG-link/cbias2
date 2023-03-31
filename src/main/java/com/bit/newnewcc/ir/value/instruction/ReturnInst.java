package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.IntegerType;
import com.bit.newnewcc.ir.value.BasicBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 返回语句
 */
public class ReturnInst extends TerminateInst{
    private final Operand returnValueOperand;

    /**
     * 构建一个空的返回语句
     */
    public ReturnInst() {
        this(null);
    }

    /**
     * 构建一个返回语句
     * @param returnValue 返回值
     */
    public ReturnInst(Value returnValue) {
        this.returnValueOperand = new Operand(this, null, returnValue);
    }

    public Value getReturnValue() {
        return returnValueOperand.getValue();
    }

    public void setReturnValue(Value returnValue) {
        this.returnValueOperand.setValue(returnValue);
    }

    @Override
    public String toString() {
        var returnValue = returnValueOperand.getValue();
        return String.format("ret %s %s", returnValue.getTypeName(), returnValue.getValueName());
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(returnValueOperand);
        return list;
    }

    @Override
    public Collection<BasicBlock> getExits() {
        return new ArrayList<>();
    }
}
