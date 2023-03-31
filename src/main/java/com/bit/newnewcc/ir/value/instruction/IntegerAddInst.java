package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.IntegerType;

/**
 * 整数加法语句
 */
public class IntegerAddInst extends IntegerCalculateInst {

    /**
     * @param type 语句的返回类型，必须是IntegerType
     * @param operand1 操作数1
     * @param operand2 操作数2
     */
    public IntegerAddInst(IntegerType type, Value operand1, Value operand2) {
        super(type, operand1, operand2);
    }

    @Override
    protected String getInstName() {
        return "add";
    }

}
