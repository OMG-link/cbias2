package cn.edu.bit.newnewcc.frontend.util;

import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.type.ArrayType;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.type.IntegerType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Types {
    private Types() {
    }

    public static Type getCommonType(Type firstType, Type secondType) {
        if (firstType.equals(secondType)) return firstType;
        if (firstType == IntegerType.getI32() && secondType == FloatType.getFloat()) return FloatType.getFloat();
        if (firstType == FloatType.getFloat() && secondType == IntegerType.getI32()) return FloatType.getFloat();
        throw new IllegalArgumentException();
    }

    public static List<Integer> getShape(Type type) {
        List<Integer> shape = new ArrayList<>();
        while (type instanceof ArrayType arrayType) {
            shape.add(arrayType.getLength());
            type = arrayType.getBaseType();
        }
        return Collections.unmodifiableList(shape);
    }
}
