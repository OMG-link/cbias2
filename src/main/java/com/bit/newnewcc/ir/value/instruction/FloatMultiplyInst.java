package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.FloatType;

/**
 * 浮点数乘法语句
 */
public class FloatMultiplyInst extends FloatCalculateInst{

    /**
     * @param type 语句的返回类型，必须是FloatType
     * @param operand1 操作数1
     * @param operand2 操作数2
     */
    public FloatMultiplyInst(FloatType type, Value operand1, Value operand2) {
        super(type, operand1, operand2);
    }

    @Override
    protected String getInstName() {
        return "fmul";
    }
}
