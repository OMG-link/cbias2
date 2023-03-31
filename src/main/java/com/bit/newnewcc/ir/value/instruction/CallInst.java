package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.type.FunctionType;
import com.bit.newnewcc.ir.value.Function;
import com.bit.newnewcc.ir.value.Instruction;

import java.util.ArrayList;
import java.util.List;

public class CallInst extends Instruction {
    private final Operand calleeOperand;

    public CallInst(FunctionType functionType){
        super(functionType);
        this.calleeOperand = new Operand(this,functionType,null);
    }

    public CallInst(Function function){
        super(function.getType());
        this.calleeOperand = new Operand(this, function.getType(), function);
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(calleeOperand);
        return list;
    }

    public Function getCallee() {
        return (Function) calleeOperand.getValue();
    }

    public void setCallee(Function function) {
        calleeOperand.setValue(function);
    }

}
