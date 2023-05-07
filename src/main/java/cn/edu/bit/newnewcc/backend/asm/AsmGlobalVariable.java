package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.ir.value.Constant;
import cn.edu.bit.newnewcc.ir.value.GlobalVariable;
import cn.edu.bit.newnewcc.ir.value.constant.ConstArray;
import cn.edu.bit.newnewcc.ir.value.constant.ConstFloat;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于存储汇编格式的全局（静态）变量
 */
public class AsmGlobalVariable {
    private final String globalVariableName;
    private final boolean isConstant, isInitialized, isSmallSection;
    private final long size;
    //存储了该全局变量的全部数据
    private final List<ValueTag> valueList;
    private final int align;

    public AsmGlobalVariable(GlobalVariable globalVariable) {
        this.globalVariableName = globalVariable.getValueName();
        this.isConstant = globalVariable.isConstant();

        Constant initialValue = globalVariable.getInitialValue();
        this.isInitialized = (!initialValue.isFilledWithZero());
        this.size = initialValue.getType().getSize();
        this.valueList = new ArrayList<>();
        if (initialValue instanceof ConstInt intValue) {
            this.align = 2;
            this.valueList.add(new ValueTag(intValue.getValue()));
            this.isSmallSection = true;
        } else if (initialValue instanceof ConstFloat floatValue) {
            this.align = 2;
            this.valueList.add(new ValueTag(floatValue.getValue()));
            this.isSmallSection = true;
        } else if (initialValue instanceof ConstArray arrayValue) {
            this.align = 3;
            this.isSmallSection = false;
            if (this.isInitialized) {
                this.getArrayValues(arrayValue);
            } else {
                this.valueList.add(new ValueTag(ValueTag.Tag.ZERO, this.size));
            }
        } else {
            this.align = 2;
            this.isSmallSection = true;
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

    private boolean isConstant() {
        return isConstant;
    }

    private String getSectionStr() {
        if (this.isConstant) {
            if (this.isSmallSection) {
                return ".section .srodata,\"a\"";
            } else {
                return ".section .rodata";
            }
        } else if (this.isInitialized) {
            if (this.isSmallSection) {
                return ".section .sdata,\"aw\"";
            } else {
                return ".data";
            }
        } else {
            if (this.isSmallSection) {
                return ".section .sbss,\"aw\",@nobits";
            } else {
                return ".bss";
            }
        }
    }

    /**
     * 输出汇编格式的全局变量数据
     */
    public String emit() {
        StringBuilder output = new StringBuilder();
        output.append(String.format(".globl %s\n", this.globalVariableName));
        output.append(this.getSectionStr()).append('\n');
        output.append(String.format(".align %d\n", this.align));
        output.append(String.format(".type %s, @object\n", this.globalVariableName));
        output.append(String.format(".size %s, %d\n", this.globalVariableName, this.size));
        output.append(String.format("%s:\n", this.globalVariableName));
        for (var i : valueList) {
            output.append(i.emit()).append('\n');
        }
        return output.toString();
    }

    /**
     * 用于标注数据格式，WORD时value存储的为数据值，ZERO则存储数据长度（字节数）
     */
    public static class ValueTag {
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

}
