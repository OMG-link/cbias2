package com.bit.newnewcc.ir.value.constant;

import com.bit.newnewcc.ir.type.FloatType;
import com.bit.newnewcc.ir.value.Constant;

import java.util.HashMap;
import java.util.Map;

/**
 * const float
 */
public class ConstFloat extends Constant {

    private final float value;

    private ConstFloat(float value) {
        super(FloatType.getFloat());
        this.value = value;
    }

    @Override
    public boolean isFilledWithZero() {
        return value == 0;
    }

    public float getValue() {
        return value;
    }

    private static Map<Float, ConstFloat> instanceMap;

    public static ConstFloat getInstance(float value) {
        if (instanceMap == null) {
            instanceMap = new HashMap<>();
        }
        if(!instanceMap.containsKey(value)){
            instanceMap.put(value,new ConstFloat(value));
        }
        return instanceMap.get(value);
    }

    @Override
    public String getValueName() {
        return "0x"+Long.toHexString(Double.doubleToLongBits(value));
    }
}
