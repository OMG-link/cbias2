package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.FloatType;
import com.bit.newnewcc.ir.type.IntegerType;
import com.bit.newnewcc.ir.value.Instruction;

import java.util.ArrayList;
import java.util.List;

/**
 * 浮点数转有符号整数指令
 * @see <a href="https://llvm.org/docs/LangRef.html#fptosi-to-instruction">LLVM IR文档</a>
 */
public class FloatToSignedIntegerInst extends Instruction {
    private final Operand sourceOperand;

    public FloatToSignedIntegerInst(FloatType sourceType, IntegerType targetType){
        super(targetType);
        this.sourceOperand = new Operand(this,sourceType,null);
    }

    public FloatToSignedIntegerInst(Value sourceValue, IntegerType targetType){
        super(targetType);
        if(!(sourceValue.getType() instanceof FloatType)){
            throw new IllegalArgumentException("Source value must have a float type.");
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
                "%s = fptosi %s %s to %s",
                this.getValueNameIR(),
                getSourceOperand().getTypeName(),
                getSourceOperand().getValueNameIR(),
                this.getTypeName()
        );
    }
}
