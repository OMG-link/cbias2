package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.ir.value.Constant;
import cn.edu.bit.newnewcc.ir.value.constant.ConstArray;

import java.util.function.BiConsumer;

public class Utility {
    private Utility() {
    }

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

    public static boolean isPowerOf2(int x) { return x > 0 && ((x & (x - 1)) == 0);}

    public static int log2(int x) {
        if (x <= 0) return -1;
        int count = 0;
        while (x > 1) {
            count++;
            x >>= 1;
        }
        return count;
    }
}