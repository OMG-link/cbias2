package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmJump;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmReturn;
import cn.edu.bit.newnewcc.backend.asm.operand.Label;

import java.util.*;

public class DeadBlockEliminationOptimizer implements Optimizer {
    @Override
    public void runOn(List<AsmInstruction> instrList) {
        Map<Label, Set<Label>> graph = new HashMap<>();
        graph.put(null, new HashSet<>());

        for (AsmInstruction instr : instrList) {
            if (instr instanceof AsmLabel labelInstr) {
                graph.put(labelInstr.getLabel(), new HashSet<>());
            }
        }

        {
            Label label = null;
            boolean fallThrough = true;

            for (AsmInstruction instr : instrList) {
                if (instr instanceof AsmLabel labelInstr) {
                    Label newLabel = labelInstr.getLabel();

                    if (fallThrough) {
                        graph.get(label).add(newLabel);
                    }

                    label = newLabel;
                    fallThrough = true;
                } else if (instr instanceof AsmJump jumpInstr) {
                    for (int i = 1; i <= 3; ++i) {
                        if (jumpInstr.getOperand(i) instanceof Label newLabel) {
                            graph.get(label).add(newLabel);
                        }

                        if (jumpInstr.getCondition() == AsmJump.Condition.UNCONDITIONAL) {
                            fallThrough = false;
                        }
                    }
                } else if (instr instanceof AsmReturn) {
                    fallThrough = false;
                }
            }
        }


        Set<Label> visited = new HashSet<>();
        visited.add(null);
        Queue<Label> queue = new LinkedList<>();
        queue.add(null);

        while (!queue.isEmpty()) {
            Label from = queue.remove();
            for (Label to : graph.get(from)) {
                if (!visited.contains(to)) {
                    queue.add(to);
                    visited.add(to);
                }
            }
        }

        List<AsmInstruction> newInstrList = new ArrayList<>();
        for (int i = 0; i < instrList.size(); ++i) {
            if (instrList.get(i) instanceof AsmLabel labelInstr && !visited.contains(labelInstr.getLabel())) {
                while (i + 1 < instrList.size() && !(instrList.get(i + 1) instanceof AsmLabel)) ++i;
            } else {
                newInstrList.add(instrList.get(i));
            }
        }

        instrList.clear();
        instrList.addAll(newInstrList);
    }
}
