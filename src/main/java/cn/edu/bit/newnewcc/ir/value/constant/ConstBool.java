package cn.edu.bit.newnewcc.ir.value.constant;

import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.value.Constant;

import java.util.HashMap;
import java.util.Map;

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

    private static Map<Boolean, ConstBool> instanceMap;

    public static ConstBool getInstance(boolean value) {
        if (instanceMap == null) {
            instanceMap = new HashMap<>();
        }
        if (!instanceMap.containsKey(value)) {
            instanceMap.put(value, new ConstBool(value ? 1 : 0));
        }
        return instanceMap.get(value);
    }

    @Override
    public String getValueName() {
        return String.valueOf(value);
    }
}
