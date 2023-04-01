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
 */
public class FloatCompareInst extends CompareInst {

    public enum Condition {
        OEQ, ONE, OLT, OLE, OGT, OGE
    }

    private final Condition condition;

    /**
     * @param operandType 待比较数据的类型，必须是FloatType
     * @param condition   比较的方法
     */
    public FloatCompareInst(FloatType operandType, Condition condition) {
        this(operandType, condition, null, null);
    }

    /**
     * @param operandType 待比较数据的类型，必须是FloatType
     * @param condition   比较的方法
     * @param operand1    操作数1
     * @param operand2    操作数2
     */
    public FloatCompareInst(FloatType operandType, Condition condition, Value operand1, Value operand2) {
        super(operandType);
        this.condition = condition;
        setOperand1(operand1);
        setOperand2(operand2);
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public IntegerType getType() {
        return (IntegerType) super.getType();
    }

    @Override
    protected String getInstName() {
        return "fcmp " + switch (condition) {
            case OEQ -> "oeq";
            case ONE -> "one";
            case OLT -> "olt";
            case OLE -> "ole";
            case OGT -> "ogt";
            case OGE -> "oge";
        };
    }

}
