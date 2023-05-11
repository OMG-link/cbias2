package cn.edu.bit.newnewcc.ir.value.constant;

import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.value.Constant;

public class ConstBool extends Constant {

    private final int value;

    private ConstBool(int value) {
        super(IntegerType.getI1());
        this.value = value;
    }

    @Override
    public boolean isFilledWithZero() {
        return value == 0;
    }

    public int getValue() {
        return value;
    }

    private static ConstBool constBoolTrue, constBoolFalse;

    public static ConstBool getInstance(boolean value) {
        if (value) {
            if (constBoolTrue == null) {
                constBoolTrue = new ConstBool(1);
            }
            return constBoolTrue;
        } else {
            if (constBoolFalse == null) {
                constBoolFalse = new ConstBool(0);
            }
            return constBoolFalse;
        }
    }

    @Override
    public String getValueName() {
        return String.valueOf(value);
    }
}
