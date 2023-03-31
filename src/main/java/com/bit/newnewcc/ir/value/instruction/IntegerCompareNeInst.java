package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.IntegerType;

/**
 * 整数不等于比较语句
 */
public class IntegerCompareNeInst extends IntegerCompareInst{

    /**
     * @param operandType 待比较数据的类型，必须是IntegerType
     */
    public IntegerCompareNeInst(IntegerType operandType) {
        super(operandType);
    }

    /**
     * @param operandType 待比较数据的类型，必须是IntegerType
     * @param operand1 操作数1
     * @param operand2 操作数2
     */
    public IntegerCompareNeInst(IntegerType operandType, Value operand1, Value operand2) {
        super(operandType, operand1, operand2);
    }

    @Override
    protected String getCompareCondition() {
        return "ne";
    }
}
