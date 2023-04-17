package com.bit.newnewcc.ir.value.constant;

import com.bit.newnewcc.ir.type.IntegerType;
import com.bit.newnewcc.ir.value.Constant;

import java.util.HashMap;
import java.util.Map;

/**
 * const int
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

    private static Map<Integer, ConstInt> instanceMap;

    public static ConstInt getInstance(int value) {
        if (instanceMap == null) {
            instanceMap = new HashMap<>();
        }
        if(!instanceMap.containsKey(value)){
            instanceMap.put(value,new ConstInt(value));
        }
        return instanceMap.get(value);
    }

    @Override
    public String getValueName() {
        return String.valueOf(value);
    }

}
