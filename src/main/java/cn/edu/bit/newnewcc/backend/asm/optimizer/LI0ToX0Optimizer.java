package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LI0ToX0Optimizer implements Optimizer {
    private boolean isLI0(AsmInstruction instr) {
        return (instr instanceof AsmLoad loadInstr && loadInstr.getOpcode() == AsmLoad.Opcode.LI && ((Immediate) instr.getOperand(2)).getValue() == 0);
    }

    public boolean runOn(AsmFunction function) {
        List<AsmInstruction> instrList = function.getInstrList();

        int count = 0;

        Set<Register> zeroRegs = new HashSet<>();

        for (AsmInstruction instr : instrList) {
            if (instr instanceof AsmBlockEnd) continue;

            if (instr instanceof AsmLabel) {
                zeroRegs.clear();
            } else if (isLI0(instr)) {
                zeroRegs.add((Register) instr.getOperand(1));
            } else {
                for (int i = 1; i <= 3; ++i) {
                    if (instr.getOperand(i) instanceof Register reg && instr.getUse().contains(reg) && zeroRegs.contains(reg)) {
                        instr.setOperand(i, IntRegister.ZERO);
                        ++count;
                    }
                }
                for (Register reg : instr.getDef()) {
                    zeroRegs.remove(reg);
                }
            }
        }

        return count > 0;
    }
}
