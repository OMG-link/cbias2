package cn.edu.bit.newnewcc.ir.value.instruction;

import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.IntegerType;

/**
 * 整数比较语句
 */
public class IntegerCompareInst extends CompareInst {

    public enum Condition{
        EQ, NE, SLT, SLE, SGT, SGE
    }

    private final Condition condition;

    /**
     * @param operandType 待比较数据的类型，必须是IntegerType
     * @param condition   比较的方法
     */
    public IntegerCompareInst(IntegerType operandType, Condition condition) {
        this(operandType, condition, null, null);
    }

    /**
     * @param operandType 待比较数据的类型，必须是IntegerType
     * @param condition   比较的方法
     * @param operand1    操作数1
     * @param operand2    操作数2
     */
    public IntegerCompareInst(IntegerType operandType, Condition condition, Value operand1, Value operand2) {
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
        return "icmp " + switch (condition){
            case EQ -> "eq";
            case NE -> "ne";
            case SLT -> "slt";
            case SLE -> "sle";
            case SGT -> "sgt";
            case SGE -> "sge";
        };
    }

}
