package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.backend.asm.controller.LifeTimeController;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;

import java.util.List;
import java.util.Objects;

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
            for (var x : LifeTimeController.getReadVregSet(inst)) {
                for (var y : LifeTimeController.getWriteVregSet(inst)) {
                    assert !x.equals(y);
                }
            }
        }
    }
}
