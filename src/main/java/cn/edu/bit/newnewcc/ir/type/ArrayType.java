package cn.edu.bit.newnewcc.ir.type;

import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.value.Constant;
import cn.edu.bit.newnewcc.ir.value.constant.ConstArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 数组类型 <br>
 * 包含两个参数：长度、基类型 <br>
 * 若要定义多维数组，需使用“数组的数组”的方式实现 <br>
 */
public class ArrayType extends Type {
    /**
     * 数组的长度 <br>
     * 即：数组内包含length个baseType类型的元素 <br>
     */
    private final int length;
    /**
     * 基类型 <br>
     * 即每个数组内单个元素的类型 <br>
     * 若基类型也是数组类型，则构成一个多维数组 <br>
     */
    private final Type baseType;

    /**
     * @param length   数组长度
     * @param baseType 数组内元素的类型
     */
    public ArrayType(int length, Type baseType) {
        this.length = length;
        this.baseType = baseType;
    }

    /**
     * 获取数组长度 <br>
     * 若类型是多维数组，返回最高维的长度 <br>
     *
     * @return 数组长度
     */
    public int getLength() {
        return length;
    }

    /**
     * 获取基类型 <br>
     * 即数组解引用一次后的类型 <br>
     *
     * @return 基类型
     */
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

    /**
     * 获取数组类型的实例 <br>
     * 数组类型是单例的 <br>
     *
     * @param length   数组长度
     * @param baseType 数组的基类型
     * @return 数组类型
     */
    public static ArrayType getInstance(int length, Type baseType) {
        if (instanceMap == null) {
            instanceMap = new HashMap<>();
        }
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