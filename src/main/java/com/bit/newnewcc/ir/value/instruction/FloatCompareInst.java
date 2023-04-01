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
public abstract class FloatCompareInst extends CompareInst {

    /**
     * @param operandType 待比较数据的类型，必须是FloatType
     */
    public FloatCompareInst(FloatType operandType) {
        this(operandType, null, null);
    }

    /**
     * @param operandType 待比较数据的类型，必须是FloatType
     * @param operand1    操作数1
     * @param operand2    操作数2
     */
    public FloatCompareInst(FloatType operandType, Value operand1, Value operand2) {
        super(operandType);
        setOperand1(operand1);
        setOperand2(operand2);
    }

    @Override
    public IntegerType getType() {
        return (IntegerType) super.getType();
    }

    protected abstract String getCompareCondition();

    @Override
    protected String getInstName() {
        return "fcmp " + getCompareCondition();
    }

}
