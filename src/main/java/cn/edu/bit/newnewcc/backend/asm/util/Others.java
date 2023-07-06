package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmTag;

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
    public static String getTagName(AsmTag tag) {
        return deleteCharString(tag.emit(), ":.\n\t ");
    }
}
