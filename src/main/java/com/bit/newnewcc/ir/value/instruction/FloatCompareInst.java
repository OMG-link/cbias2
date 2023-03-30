package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.FloatType;
import com.bit.newnewcc.ir.type.IntegerType;
import com.bit.newnewcc.ir.value.Instruction;

import java.util.ArrayList;
import java.util.List;

/**
 * 浮点数比较语句 <br>
 * 此类没有存在的必要，只是为了共享浮点数比较语句通用的代码 <br>
 */
public abstract class FloatCompareInst extends Instruction {
    private final Operand operand1, operand2;

    /**
     * @param operandType 待比较数据的类型，必须是FloatType
     * @param operand1 操作数1
     * @param operand2 操作数2
     */
    public FloatCompareInst(FloatType operandType, Value operand1, Value operand2) {
        super(IntegerType.getI1());
        this.operand1 = new Operand(this,operandType,operand1);
        this.operand2 = new Operand(this,operandType,operand2);
    }

    public Value getOperand1() {
        return operand1.getValue();
    }

    public void setOperand1(Value value) {
        operand1.setValue(value);
    }

    public Value getOperand2() {
        return operand2.getValue();
    }

    public void setOperand2(Value value) {
        operand2.setValue(value);
    }

    @Override
    public IntegerType getType() {
        return (IntegerType) super.getType();
    }

    protected abstract String getCompareCondition();

    @Override
    public String toString() {
        return String.format(
                "%s = fcmp %s %s %s, %s",
                this.getValueName(),
                this.getCompareCondition(),
                this.getType(),
                getOperand1().getValueName(),
                getOperand2().getValueName()
        );
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(operand1);
        list.add(operand2);
        return list;
    }
}
