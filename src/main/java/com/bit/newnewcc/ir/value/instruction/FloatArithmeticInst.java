package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.FloatType;
import com.bit.newnewcc.ir.value.Instruction;

import java.util.ArrayList;
import java.util.List;

/**
 * 浮点数运算语句 <br>
 * 此类没有存在的必要，只是为了共享浮点数运算语句通用的代码 <br>
 */
public abstract class FloatArithmeticInst extends ArithmeticInst {

    /**
     * @param type 语句的返回类型，必须是FloatType
     */
    public FloatArithmeticInst(FloatType type) {
        this(type, null, null);
    }

    /**
     * @param operandType 语句的返回类型，必须是FloatType
     * @param operand1    操作数1
     * @param operand2    操作数2
     */
    public FloatArithmeticInst(FloatType operandType, Value operand1, Value operand2) {
        super(operandType);
        setOperand1(operand1);
        setOperand2(operand2);
    }

    @Override
    public FloatType getType() {
        return (FloatType) super.getType();
    }

}
