package cn.edu.bit.newnewcc.frontend.util;

import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.type.ArrayType;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.type.IntegerType;

public final class Types {
    private Types() {
    }

    public static Type getCommonType(Type firstType, Type secondType) {
        if (firstType.equals(secondType)) return firstType;
        if (firstType instanceof IntegerType && secondType instanceof IntegerType)
            return IntegerType.getInstance(
                Math.max(((IntegerType) firstType).getBitWidth(), ((IntegerType) secondType).getBitWidth()));
        if (firstType instanceof IntegerType && secondType == FloatType.getFloat()) return FloatType.getFloat();
        if (firstType == FloatType.getFloat() && secondType instanceof IntegerType) return FloatType.getFloat();
        throw new IllegalArgumentException();
    }

    public static int countElements(Type type) {
        if (type instanceof ArrayType arrayType)
            return arrayType.getLength() * countElements(arrayType.getBaseType());
        else
            return 1;
    }
}
