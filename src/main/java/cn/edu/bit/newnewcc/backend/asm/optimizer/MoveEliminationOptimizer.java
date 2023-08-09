package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmMove;

import java.util.List;

public class MoveEliminationOptimizer implements Optimizer {
    @Override
    public void runOn(List<AsmInstruction> instrList) {
        instrList.removeIf(instr -> instr instanceof AsmMove moveInstr && moveInstr.getOperand(1).equals(moveInstr.getOperand(2)));
    }
}
