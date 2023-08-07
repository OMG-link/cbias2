package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmJump;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.operand.Label;

import java.util.*;

public class DeadBlockEliminationOptimizer implements Optimizer {
    @Override
    public boolean runOn(List<AsmInstruction> instrList) {
        if (instrList.isEmpty()) return false;

        boolean madeChange = false;
        Set<Label> reachableLabels = new HashSet<>();
        reachableLabels.add(((AsmLabel) instrList.get(0)).getLabel());

        for (int i = 1; i < instrList.size(); ++i) {
            AsmInstruction instr = instrList.get(i);
            if (instr instanceof AsmJump) {
                for (int j = 1; j <= 3; ++j) {
                    if (instr.getOperand(j) instanceof Label label) {
                        reachableLabels.add(label);
                    }
                }
            }
        }

        List<AsmInstruction> newInstrList = new ArrayList<>();
        for (int i = 0; i < instrList.size(); ++i) {
            AsmInstruction instr = instrList.get(i);
            if (instr instanceof AsmLabel labelInstr && !reachableLabels.contains(labelInstr.getLabel())) {
                while (i + 1 < instrList.size() && !(instrList.get(i + 1) instanceof AsmLabel)) ++i;
            } else {
                newInstrList.add(instr);
            }
        }

        instrList.clear();
        instrList.addAll(newInstrList);

        return madeChange;
    }
}
