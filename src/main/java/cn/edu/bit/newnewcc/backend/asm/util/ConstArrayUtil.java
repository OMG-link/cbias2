package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.ir.value.Constant;
import cn.edu.bit.newnewcc.ir.value.constant.ConstArray;

import java.util.function.BiConsumer;

public class ConstArrayUtil {
    /**
     * 处理ConstArray相关的操作
     * @param arrayValue ConstArray值
     * @param offset 当前处理到数组的偏移量，通常传入0
     * @param workItem 对数组中单个元素进行的操作，传入参数为(offset, item)
     * @param workZeroSegment 对数组中一段0值进行的操作，传入参数为(offset, length)
     */
    public static void workOnArray(ConstArray arrayValue, long offset, BiConsumer<Long, Constant> workItem, BiConsumer<Long, Long> workZeroSegment) {
        int length = arrayValue.getLength();
        int filledLength = arrayValue.getInitializedLength();
        if (filledLength == 0) {
            long totalLength = arrayValue.getType().getSize();
            workZeroSegment.accept(offset, totalLength);
            return;
        }
        Constant firstValue = arrayValue.getValueAt(0);
        if (!(firstValue instanceof ConstArray)) {
            for (int i = 0; i < arrayValue.getInitializedLength(); i++) {
                workItem.accept(offset, arrayValue.getValueAt(i));
            }
            if (filledLength < length) {
                workZeroSegment.accept(offset + 4L * filledLength, 4L * (length - filledLength));
            }
        } else {
            long arrayItemSize = firstValue.getType().getSize();
            for (int i = 0; i < arrayValue.getInitializedLength(); i++) {
                ConstArray arrayItem = (ConstArray) arrayValue.getValueAt(i);
                workOnArray(arrayItem, offset, workItem, workZeroSegment);
                offset += arrayItemSize;
            }
            if (filledLength < length) {
                workZeroSegment.accept(offset, arrayItemSize * (length - filledLength));
            }
        }
    }

}
