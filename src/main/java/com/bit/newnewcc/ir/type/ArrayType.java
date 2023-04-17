package com.bit.newnewcc.ir.type;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.value.Constant;
import com.bit.newnewcc.ir.value.constant.ConstArray;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class ArrayType extends Type {
    private final int length;
    private final Type baseType;

    public ArrayType(int length, Type baseType) {
        this.length = length;
        this.baseType = baseType;
    }

    public int getLength() {
        return length;
    }

    public Type getBaseType() {
        return baseType;
    }

    private ConstArray defaultInitialization;

    @Override
    public Constant getDefaultInitialization() {
        if (defaultInitialization == null) {
            defaultInitialization = new ConstArray(baseType, length, new ArrayList<>());
        }
        return defaultInitialization;
    }

    @Override
    protected String getTypeName_() {
        return String.format("[%d x %s]", length, baseType.getTypeName());
    }

    @Override
    public long getSize() {
        return baseType.getSize() * length;
    }

    private static Map<ArrayType, ArrayType> instanceMap;

    public static ArrayType getInstance(int length, Type baseType) {
        var keyType = new ArrayType(length, baseType);
        if (!instanceMap.containsKey(keyType)) {
            instanceMap.put(keyType, keyType);
        }
        return instanceMap.get(keyType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayType arrayType = (ArrayType) o;
        return length == arrayType.length && baseType == arrayType.baseType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(length, baseType);
    }
}
