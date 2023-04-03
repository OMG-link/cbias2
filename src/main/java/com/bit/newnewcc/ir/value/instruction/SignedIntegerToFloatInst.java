package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.exception.IllegalArgumentException;
import com.bit.newnewcc.ir.type.FloatType;
import com.bit.newnewcc.ir.type.IntegerType;
import com.bit.newnewcc.ir.value.Instruction;

import java.util.ArrayList;
import java.util.List;

/**
 * 有符号整数转浮点数指令
 * @see <a href="https://llvm.org/docs/LangRef.html#sitofp-to-instruction">LLVM IR文档</a>
 */
public class SignedIntegerToFloatInst extends Instruction {

    private final Operand sourceOperand;

    public SignedIntegerToFloatInst(IntegerType sourceType, FloatType targetType){
        super(targetType);
        this.sourceOperand = new Operand(this, sourceType,null);
    }

    public SignedIntegerToFloatInst(Value sourceValue, FloatType targetType){
        super(targetType);
        if(!(sourceValue.getType() instanceof IntegerType)){
            throw new IllegalArgumentException("Source value must have an integer type.");
        }
        this.sourceOperand = new Operand(this,sourceValue.getType(),sourceValue);
    }

    public Value getSourceOperand() {
        return sourceOperand.getValue();
    }

    public void setSourceOperand(Value value){
        sourceOperand.setValue(value);
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(sourceOperand);
        return list;
    }

    @Override
    public String toString() {
        return String.format(
                "%s = sitofp %s %s to %s",
                this.getValueNameIR(),
                getSourceOperand().getTypeName(),
                getSourceOperand().getValueNameIR(),
                this.getTypeName()
        );
    }
}
