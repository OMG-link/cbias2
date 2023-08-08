package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmBlockEnd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DeadInstructionEliminationOptimizer implements Optimizer {
    @Override
    public void runOn(List<AsmInstruction> instrList) {
        Set<Register> usedVRegs = new HashSet<>();
        for (AsmInstruction instr : instrList) {
            if (instr instanceof AsmLabel || instr instanceof AsmBlockEnd) continue;
            for (Register reg : instr.getUse()) {
                if (reg.isVirtual()) usedVRegs.add(reg);
            }
        }

        Iterator<AsmInstruction> iterator = instrList.iterator();
        while (iterator.hasNext()) {
            AsmInstruction instr = iterator.next();

            if (instr instanceof AsmLabel || instr instanceof AsmBlockEnd) continue;

            if (instr.willReturn() && !instr.mayWriteToMemory()) {
                boolean dead = true;
                for (Register reg : instr.getDef()) {
                    if (!reg.isVirtual() || usedVRegs.contains(reg)) {
                        dead = false;
                        break;
                    }
                }

                if (dead) {
                    iterator.remove();
                }
            }
        }

    }
}
