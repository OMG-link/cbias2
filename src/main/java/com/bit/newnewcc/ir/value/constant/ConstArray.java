package com.bit.newnewcc.ir.value.constant;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.exception.IndexOutOfBoundsException;
import com.bit.newnewcc.ir.type.ArrayType;
import com.bit.newnewcc.ir.value.Constant;

import java.util.List;

/**
 * 常量数组 <br>
 * 出于性能的考虑，此类型并非单例 <br>
 *
 * @see <a href="https://llvm.org/docs/LangRef.html#complex-constants">LLVM IR文档</a>
 */
public class ConstArray extends Constant {
    private final int length;
    private final List<Constant> valueList;

    /**
     * @param baseType        数组的基类。若想定义高维数组，请使用“数组的数组”
     * @param length          数组的实际长度
     * @param initializerList 数组已初始化部分的长度。未初始化部分将被初始化为0。
     */
    public ConstArray(Type baseType, int length, List<Constant> initializerList) {
        super(ArrayType.getInstance(length, baseType));
        this.length = length;
        this.valueList = initializerList;
    }

    @Override
    public ArrayType getType() {
        return (ArrayType) super.getType();
    }

    /**
     * @return 数组已初始化部分的长度
     */
    public int getInitializedLength() {
        return valueList.size();
    }

    /**
     * @return 数组（最高维）的长度
     */
    public int getLength() {
        return length;
    }

    public Constant getValueAt(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(index, 0, length);
        }
        if (index < valueList.size()) {
            return valueList.get(index);
        } else {
            return getType().getBaseType().getDefaultInitialization();
        }
    }

    @Override
    public boolean isFilledWithZero() {
        return valueList.size() == 0;
    }

    @Override
    public String getValueName() {
        if (isFilledWithZero()) {
            return "zeroinitializer";
        } else {
            var builder = new StringBuilder();
            builder.append('[');
            for (var i = 0; i < length; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append(getType().getBaseType().getTypeName()).append(' ');
                if (i < valueList.size()) {
                    builder.append(valueList.get(i).getValueNameIR());
                } else {
                    builder.append(getType().getBaseType().getDefaultInitialization().getValueNameIR());
                }
            }
            builder.append(']');
            return builder.toString();
        }
    }

}
