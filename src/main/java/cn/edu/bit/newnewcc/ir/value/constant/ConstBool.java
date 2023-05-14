package cn.edu.bit.newnewcc.ir.value.constant;

import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.value.Constant;

public class ConstBool extends Constant {

    private final boolean value;

    private ConstBool(boolean value) {
        super(IntegerType.getI1());
        this.value = value;
    }

    @Override
    public boolean isFilledWithZero() {
        return !value;
    }

    @Override
    public String getValueName() {
        return String.valueOf(value);
    }

    public boolean getValue() {
        return value;
    }

    private static class TrueHolder {
        private static final ConstBool INSTANCE = new ConstBool(true);
    }

    private static class FalseHolder {
        private static final ConstBool INSTANCE = new ConstBool(false);
    }

    public static ConstBool getInstance(boolean value) {
        return value ? TrueHolder.INSTANCE : FalseHolder.INSTANCE;
    }
}
