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
    private final Type baseType;
    private final int length;
    private final List<Constant> valueList;

    public ConstArray(Type baseType, int length, List<Constant> initializerList) {
        super(ArrayType.getInstance(length, baseType));
        this.baseType = baseType;
        this.length = length;
        this.valueList = initializerList;
    }

    public boolean isZero() {
        return valueList.size() == 0;
    }

    public int getNoneZeroLength() {
        return valueList.size();
    }

    public Constant getValueAt(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(index, 0, length);
        }
        if (index < valueList.size()) {
            return valueList.get(index);
        } else {
            return baseType.getDefaultInitialization();
        }
    }

    @Override
    public String getValueName() {
        if (isZero()) {
            return "zeroinitializer";
        } else {
            var builder = new StringBuilder();
            builder.append('[');
            for (var i = 0; i < length; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append(baseType.getTypeName()).append(' ');
                if (i < valueList.size()) {
                    builder.append(valueList.get(i).getValueNameIR());
                } else {
                    builder.append(baseType.getDefaultInitialization().getValueNameIR());
                }
            }
            builder.append(']');
            return builder.toString();
        }
    }

}
