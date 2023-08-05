package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;

import java.util.List;

public class Others {
    public static String deleteCharString(String sourceString, String charTable) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < sourceString.length(); i++) {
            char ch = sourceString.charAt(i);
            if (!charTable.contains(String.valueOf(ch))) {
                res.append(sourceString.charAt(i));
            }
        }
        return res.toString();
    }

    public static void check(List<AsmInstruction> instructionList) {
        for (var inst : instructionList) {
            for (var x : inst.getReadVRegSet()) {
                for (var y : inst.getWriteVRegSet()) {
                    if (x.equals(y)) {
                        throw new RuntimeException("assertion failed");
                    }
                }
            }
        }
    }

    public static boolean isPowerOf2(int x) {
        return x > 0 && ((x & (x - 1)) == 0);
    }

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
