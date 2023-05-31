package cn.edu.bit.newnewcc.ir.value.instruction;

import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.type.IntegerType;

/**
 * 浮点数比较语句
 */
public class FloatCompareInst extends CompareInst {

    public enum Condition {
        OEQ, ONE, OLT, OLE, OGT, OGE
    }

    private final Condition condition;

    /**
     * @param comparedType 待比较数据的类型，必须是FloatType
     * @param condition    比较的方法
     */
    public FloatCompareInst(FloatType comparedType, Condition condition) {
        this(comparedType, condition, null, null);
    }

    /**
     * @param comparedType 待比较数据的类型，必须是FloatType
     * @param condition    比较的方法
     * @param operand1     操作数1
     * @param operand2     操作数2
     */
    public FloatCompareInst(FloatType comparedType, Condition condition, Value operand1, Value operand2) {
        super(comparedType);
        this.condition = condition;
        setOperand1(operand1);
        setOperand2(operand2);
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public FloatType getComparedType() {
        return (FloatType) super.getComparedType();
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
