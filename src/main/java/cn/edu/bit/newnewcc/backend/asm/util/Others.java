package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.backend.asm.controller.LifeTimeController;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

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
            for (var x : LifeTimeController.getReadVRegSet(inst)) {
                for (var y : LifeTimeController.getWriteVRegSet(inst)) {
                    if (x.equals(y)) {
                        throw new RuntimeException("assertion failed");
                    }
                }
            }
        }
    }
}
