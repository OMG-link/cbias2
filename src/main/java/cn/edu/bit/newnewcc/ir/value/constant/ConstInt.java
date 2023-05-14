package cn.edu.bit.newnewcc.ir.value.constant;

import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.value.Constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 整型常量
 */
public class ConstInt extends Constant {

    private final int value;

    private ConstInt(int value) {
        super(IntegerType.getI32());
        this.value = value;
    }

    @Override
    public boolean isFilledWithZero() {
        return value == 0;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String getValueName() {
        return String.valueOf(value);
    }

    private static class Cache {
        private static final Map<Integer, ConstInt> cache = new HashMap<>();
    }

    public static ConstInt getInstance(int value) {
        if (!Cache.cache.containsKey(value))
            Cache.cache.put(value, new ConstInt(value));

        return Cache.cache.get(value);
    }
}
