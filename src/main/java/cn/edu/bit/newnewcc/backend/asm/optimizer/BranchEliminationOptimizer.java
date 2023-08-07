package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmJump;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.operand.Label;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BranchEliminationOptimizer implements Optimizer {
    @Override
    public boolean runOn(List<AsmInstruction> instrList) {
        boolean madeChange = false;
        Map<Label, Label> labelMap = new HashMap<>();

        for (int i = 0; i < instrList.size() - 1; ++i) {
            AsmInstruction curInstr = instrList.get(i);
            AsmInstruction nextInstr = instrList.get(i + 1);

            if (curInstr instanceof AsmLabel && nextInstr instanceof AsmJump && ((AsmJump) nextInstr).getCondition() == AsmJump.Condition.UNCONDITIONAL) {
                labelMap.put(((AsmLabel) curInstr).getLabel(), (Label) nextInstr.getOperand(1));
            }
        }

        for (AsmInstruction instr : instrList) {
            if (instr instanceof AsmJump) {
                for (int i = 1; i <= 3; ++i) {
                    if (instr.getOperand(i) instanceof Label label && labelMap.containsKey(label)) {
                        instr.setOperand(i, labelMap.get(label));
                        madeChange = true;
                    }
                }
            }
        }

        return madeChange;
    }
}
