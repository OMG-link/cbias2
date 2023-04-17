package com.bit.newnewcc.backend.asm;

import com.bit.newnewcc.ir.value.Constant;
import com.bit.newnewcc.ir.value.GlobalVariable;
import com.bit.newnewcc.ir.value.constant.ConstArray;
import com.bit.newnewcc.ir.value.constant.ConstFloat;
import com.bit.newnewcc.ir.value.constant.ConstInt;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于存储汇编格式的全局（静态）变量
 */
public class AsmGlobalVariable {
    private final String globalVariableName;
    private final boolean isConstant, initialized;
    private final long size;
    //存储了该全局变量的全部数据
    private final List<ValueTag> valueList;
    private int align;

    public AsmGlobalVariable(GlobalVariable globalVariable) {
        this.globalVariableName = globalVariable.getValueName();
        this.isConstant = globalVariable.isConstant();

        Constant initialValue = globalVariable.getInitialValue();
        this.initialized = (!initialValue.isFilledWithZero());
        this.size = initialValue.getType().getSize();
        this.valueList = new ArrayList<>();
        if (initialValue instanceof ConstArray arrayValue) {
            this.align = 3;
            if (this.initialized) {
                getArrayValues(arrayValue);
            } else {
                this.valueList.add(new ValueTag(ValueTag.Tag.ZERO, this.size));
            }
        } else if (initialValue instanceof ConstInt intValue) {
            this.align = 2;
            this.valueList.add(new ValueTag(intValue.getValue()));
        } else if (initialValue instanceof ConstFloat floatValue) {
            this.align = 2;
            this.valueList.add(new ValueTag(floatValue.getValue()));
        }
    }

    private int getArrayValues(ConstArray arrayValue) {
        int length = arrayValue.getLength();
        Constant firstValue = arrayValue.getValueAt(0);
        if (firstValue instanceof ConstInt) {
            for (int i = 0; i < arrayValue.getInitializedLength(); i++) {
                ConstInt arrayItem = (ConstInt) arrayValue.getValueAt(i);
                this.valueList.add(new ValueTag(arrayItem.getValue()));
            }
            this.valueList.add(new ValueTag(ValueTag.Tag.ZERO, 4L * (length - arrayValue.getInitializedLength())));
            return 4 * length;
        } else if (firstValue instanceof ConstFloat) {
            for (int i = 0; i < arrayValue.getInitializedLength(); i++) {
                ConstFloat arrayItem = (ConstFloat) arrayValue.getValueAt(i);
                this.valueList.add(new ValueTag(arrayItem.getValue()));
            }
            this.valueList.add(new ValueTag(ValueTag.Tag.ZERO, 4L * (length - arrayValue.getInitializedLength())));
            return 4 * length;
        } else {
            int arrayItemSize = 0;
            for (int i = 0; i < arrayValue.getInitializedLength(); i++) {
                ConstArray arrayItem = (ConstArray) arrayValue.getValueAt(i);
                int sonArraySize = getArrayValues(arrayItem);
                if (arrayItemSize == 0) {
                    arrayItemSize = sonArraySize;
                }
            }
            this.valueList.add(new ValueTag(ValueTag.Tag.ZERO, (long) arrayItemSize * (length - arrayValue.getInitializedLength())));
            return arrayItemSize * length;
        }
    }

    /**
     * 用于标注数据格式，WORD时value存储的为数据值，ZERO则存储数据长度（字节数）
     */
    public class ValueTag {
        private final Tag tag;
        private final int value;
        private final long length;

        public ValueTag(Tag tag, long value) {
            this.tag = tag;
            if (tag == Tag.WORD) {
                this.value = (int) value;
                this.length = 0;
            } else {
                this.length = value;
                this.value = 0;
            }
        }

        public ValueTag(int value) {
            this.tag = Tag.WORD;
            this.length = 0;
            this.value = value;
        }

        public ValueTag(float value) {
            this.tag = Tag.WORD;
            this.length = 0;
            this.value = Float.floatToIntBits(value);
        }

        public String emit() {
            if (tag == Tag.WORD) {
                return String.format(".word %d", value);
            } else {
                return String.format(".zero %d", length);
            }
        }

        public enum Tag {
            WORD, ZERO
        }
    }

    public boolean isConstant() {
        return isConstant;
    }

}
