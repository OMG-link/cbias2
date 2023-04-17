package com.bit.newnewcc.ir.type;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.value.Constant;

import java.util.*;

/**
 * 函数类型
 */
public class FunctionType extends Type {
    private final Type returnType;
    private final List<Type> parameterTypes;

    /**
     * @param returnType     函数返回值的类型
     * @param parameterTypes 函数参数类型的列表
     */
    private FunctionType(Type returnType, List<Type> parameterTypes) {
        this.returnType = returnType;
        this.parameterTypes = new ArrayList<>(parameterTypes);
    }

    /**
     * @return 函数返回值类型
     */
    public Type getReturnType() {
        return returnType;
    }

    /**
     * @return 函数参数类型的列表，该列表是只读的
     */
    public List<Type> getParameterTypes() {
        return Collections.unmodifiableList(parameterTypes);
    }

    @Override
    protected String getTypeName_() {
        var builder = new StringBuilder();
        builder.append(returnType.getTypeName());
        builder.append('(');
        boolean needCommaBefore = false;
        for (var parameterType : parameterTypes) {
            if (needCommaBefore) {
                builder.append(',');
            }
            builder.append(parameterType.getTypeName());
            needCommaBefore = true;
        }
        builder.append(')');
        return builder.toString();
    }

    @Override
    public Constant getDefaultInitialization() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSize() {
        // 理论上应该返回指针的位数，但是SysY不涉及指针，所以这个方法应该不会被调用到
        throw new UnsupportedOperationException();
    }

    // 此处使用Map而非Set，是因为需要保证getInstance返回的实例是唯一的
    // 使用Set时，无法通过临时构建的实例找到缓存了的唯一返回实例
    private static Map<FunctionType, FunctionType> instanceMap;

    public static FunctionType getInstance(Type returnType, List<Type> parameterTypes) {
        if (instanceMap == null) {
            instanceMap = new HashMap<>();
        }
        var keyType = new FunctionType(returnType, parameterTypes);
        if (!instanceMap.containsKey(keyType)) {
            instanceMap.put(keyType, keyType);
        }
        return instanceMap.get(keyType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionType that = (FunctionType) o;
        return returnType.equals(that.returnType) && parameterTypes.equals(that.parameterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, parameterTypes);
    }
}
