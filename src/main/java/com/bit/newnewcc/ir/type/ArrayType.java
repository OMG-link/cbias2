package com.bit.newnewcc.ir.type;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.value.Constant;

import java.util.Map;
import java.util.Objects;

public class ArrayType extends Type {
    private final int length;
    private final Type baseType;

    public ArrayType(int length, Type baseType) {
        this.length = length;
        this.baseType = baseType;
    }

    @Override
    public Constant getDefaultInitialization() {
        //todo: zero_initializer
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getTypeName_() {
        return String.format("[%d x %s]",length,baseType.getTypeName());
    }

    private static Map<ArrayType,ArrayType> instanceMap;

    public static ArrayType getInstance(int length, Type baseType) {
        var keyType = new ArrayType(length,baseType);
        if(!instanceMap.containsKey(keyType)){
            instanceMap.put(keyType,keyType);
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
