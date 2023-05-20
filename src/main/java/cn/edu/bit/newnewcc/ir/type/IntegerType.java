package cn.edu.bit.newnewcc.ir.type;

import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.value.Constant;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;

/**
 * 整数类型
 */
public class IntegerType extends Type {
    private final int bitWidth;

    private IntegerType(int bitWidth) {
        this.bitWidth = bitWidth;
    }

    public int getBitWidth() {
        return bitWidth;
    }

    @Override
    protected String getTypeName_() {
        return String.format("i%d", bitWidth);
    }

    @Override
    public Constant getZeroInitialization() {
        return ConstInt.getInstance(0);
    }

    @Override
    public long getSize() {
        if (bitWidth != 32)
            throw new UnsupportedOperationException();

        return 4;
    }

    private static class I1Holder {
        private static final IntegerType INSTANCE = new IntegerType(1);
    }

    private static class I32Holder {
        private static final IntegerType INSTANCE = new IntegerType(32);
    }

    public static IntegerType getI1() {
        return I1Holder.INSTANCE;
    }

    public static IntegerType getI32() {
        return I32Holder.INSTANCE;
    }
}
