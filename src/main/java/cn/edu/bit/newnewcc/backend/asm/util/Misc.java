package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstructions;

import java.util.List;
public class Misc {
    public static String deleteCharString(String sourceString, String charTable) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < sourceString.length(); i++) {
            char ch = sourceString.charAt(i);
            if (!charTable.contains(String.valueOf(ch))) {
                result.append(sourceString.charAt(i));
            }
        }
        return result.toString();
    }
    public static void check(List<AsmInstruction> instructionList) {
        for (var inst : instructionList) {
            for (var x : AsmInstructions.getReadVRegSet(inst)) {
                for (var y : AsmInstructions.getWriteVRegSet(inst)) {
                    if (x.equals(y)) {
                        throw new AssertionError();
                    }
                }
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
