package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmBlockEnd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.*;

public class DeadInstructionEliminationOptimizer implements Optimizer {
    @Override
    public void runOn(List<AsmInstruction> instrList) {
        Map<Register, Integer> useCount = new HashMap<>();

        for (AsmInstruction instr : instrList) {
            if (instr instanceof AsmLabel || instr instanceof AsmBlockEnd) continue;
            for (Register reg : instr.getUse()) {
                if (reg.isVirtual()) {
                    useCount.put(reg, useCount.getOrDefault(reg, 0) + 1);
                }
            }
        }

        boolean madeChange;
        do {
            madeChange = false;
            Iterator<AsmInstruction> iterator = instrList.iterator();
            while (iterator.hasNext()) {
                AsmInstruction instr = iterator.next();
                if (instr instanceof AsmLabel || instr instanceof AsmBlockEnd) continue;
                if (instr.willReturn() && !instr.mayHaveSideEffects()) {
                    boolean dead = true;
                    for (Register reg : instr.getDef()) {
                        if (!reg.isVirtual() || useCount.getOrDefault(reg, 0) > 0) {
                            dead = false;
                            break;
                        }
                    }
                    if (dead) {
                        for (Register reg : instr.getUse()) {
                            if (useCount.getOrDefault(reg, 0) > 0) {
                                useCount.put(reg, useCount.get(reg) - 1);
                            }
                        }
                        iterator.remove();
                        madeChange = true;
                    }
                }
            }
        } while (madeChange);
    }
}
