package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmMove;

import java.util.List;

public class MoveEliminationOptimizer implements Optimizer {
    @Override
    public void runOn(AsmFunction function) {
        List<AsmInstruction> instrList = function.getInstrList();
        instrList.removeIf(instr -> instr instanceof AsmMove moveInstr && moveInstr.getOperand(1).equals(moveInstr.getOperand(2)));
    }
}
