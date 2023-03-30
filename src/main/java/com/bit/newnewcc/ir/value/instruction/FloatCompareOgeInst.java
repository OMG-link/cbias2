package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.FloatType;

/**
 * 浮点数有序大于等于比较 <br>
 * 有序指的是两个操作数均不为NaN，SysY的测试数据保证所有数字都不是NaN或INF <br>
 */
public class FloatCompareOgeInst extends FloatCompareInst{

    /**
     * @param operandType 待比较数据的类型，必须是FloatType
     * @param operand1 操作数1
     * @param operand2 操作数2
     */
    public FloatCompareOgeInst(FloatType operandType, Value operand1, Value operand2) {
        super(operandType, operand1, operand2);
    }

    @Override
    protected String getCompareCondition() {
        return "oge";
    }
}
