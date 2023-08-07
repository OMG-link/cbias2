package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import java.util.List;

public class OptimizerManager {
    public void runOn(List<AsmInstruction> instrList) {
        new LI0ToX0Optimizer().runOn(instrList);
        new AddX0ToMvOptimizer().runOn(instrList);
        new StrengthReductionOptimizer().runOn(instrList);
        new BranchEliminationOptimizer().runOn(instrList);
        new DeadInstructionEliminationOptimizer().runOn(instrList);
        new DeadBlockEliminationOptimizer().runOn(instrList);
    }
}
