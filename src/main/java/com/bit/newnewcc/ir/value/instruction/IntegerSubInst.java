package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.IntegerType;

/**
 * 整数减法语句
 */
public class IntegerSubInst extends IntegerCalculateInst {

    /**
     * @param type 语句的返回类型，必须是IntegerType
     * @param operand1 操作数1
     * @param operand2 操作数2
     */
    public IntegerSubInst(IntegerType type, Value operand1, Value operand2) {
        super(type, operand1, operand2);
    }

    /**
     * @param type 语句的返回类型，必须是IntegerType
     */
    public IntegerSubInst(IntegerType type) {
        super(type);
    }

    @Override
    protected String getInstName() {
        return "sub";
    }

}
