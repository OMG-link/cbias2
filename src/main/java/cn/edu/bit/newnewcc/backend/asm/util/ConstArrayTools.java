package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.ir.value.Constant;
import cn.edu.bit.newnewcc.ir.value.constant.ConstArray;

import java.util.function.BiConsumer;

public class ConstArrayTools {
    static int getArraySize(ConstArray arrayValue) {
        if (arrayValue.getLength() == 0) {
            return 0;
        }
        Constant firstValue = arrayValue.getValueAt(0);
        if (firstValue instanceof ConstArray firstArray) {
            return getArraySize(firstArray) * arrayValue.getLength();
        } else {
            return 4 * arrayValue.getLength();
        }
    }

    /**
     * 处理ConstArray相关的操作
     * @param arrayValue ConstArray值
     * @param offset 当前处理到数组的偏移量，通常传入0
     * @param workItem 对数组中单个元素进行的操作，传入参数为(offset, item)
     * @param workZeroSegment 对数组中一段0值进行的操作，传入参数为(offset, length)
     * @return 返回访问数组的总大小
     */
    public static int workOnArray(ConstArray arrayValue, int offset, BiConsumer<Integer, Constant> workItem, BiConsumer<Integer, Integer> workZeroSegment) {
        int length = arrayValue.getLength();
        int filledLength = arrayValue.getInitializedLength();
        if (filledLength == 0) {
            int totalLength = getArraySize(arrayValue);
            workZeroSegment.accept(offset, totalLength);
            return totalLength;
        }
        Constant firstValue = arrayValue.getValueAt(0);
        if (!(firstValue instanceof ConstArray)) {
            for (int i = 0; i < arrayValue.getInitializedLength(); i++) {
                workItem.accept(offset, arrayValue.getValueAt(i));
            }
            if (filledLength < length) {
                workZeroSegment.accept(offset + 4 * filledLength, 4 * (length - filledLength));
            }
            return 4 * length;
        } else {
            int arrayItemSize = 0;
            for (int i = 0; i < arrayValue.getInitializedLength(); i++) {
                ConstArray arrayItem = (ConstArray) arrayValue.getValueAt(i);
                int sonArraySize = workOnArray(arrayItem, offset, workItem, workZeroSegment);
                if (arrayItemSize == 0) {
                    arrayItemSize = sonArraySize;
                }
                offset += arrayItemSize;
            }
            if (filledLength < length) {
                workZeroSegment.accept(offset, arrayItemSize * (length - filledLength));
            }
            return arrayItemSize * length;
        }
    }

}
