package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmJump;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.operand.Label;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedundantLabelEliminationOptimizer implements Optimizer {
    @Override
    public void runOn(AsmFunction function) {
        List<AsmInstruction> instrList = function.getInstrList();

        Set<Label> usefulLabels = new HashSet<>();
        for (AsmInstruction instr : instrList) {
            if (instr instanceof AsmJump) {
                for (int i = 1; i <= 3; ++i) {
                    if (instr.getOperand(i) instanceof Label label) {
                        usefulLabels.add(label);
                    }
                }
            }
        }

        instrList.removeIf(instr -> instr instanceof AsmLabel labelInstr && !usefulLabels.contains((Label) labelInstr.getLabel()));
    }
}
