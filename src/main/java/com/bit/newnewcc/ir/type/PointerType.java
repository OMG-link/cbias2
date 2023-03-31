package com.bit.newnewcc.ir.type;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.value.Constant;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 指针类型
 */
public class PointerType extends Type {

    private final Type pointedType;

    /**
     * @param pointedType 被指向的类型
     */
    private PointerType(Type pointedType) {
        this.pointedType = pointedType;
    }

    public Type getPointedType() {
        return pointedType;
    }

    @Override
    protected String getTypeName_() {
        return pointedType.getTypeName() + "*";
    }

    @Override
    public Constant getDefaultInitialization() {
        throw new UnsupportedOperationException();
    }

    // 此处使用Map而非Set，是因为需要保证getInstance返回的实例是唯一的
    // 使用Set时，无法通过临时构建的实例找到缓存了的唯一返回实例
    private static Map<PointerType, PointerType> instanceMap;

    public static PointerType getInstance(Type pointedType) {
        if(instanceMap==null){
            instanceMap = new HashMap<>();
        }
        var keyType = new PointerType(pointedType);
        if(!instanceMap.containsKey(keyType)){
            instanceMap.put(keyType,keyType);
        }
        return instanceMap.get(keyType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointerType that = (PointerType) o;
        // 所有类型都是单例的，这里直接用==比较地址，可以避免递归比较内部类型
        return pointedType == that.pointedType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pointedType);
    }

}
