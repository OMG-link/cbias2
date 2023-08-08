package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import java.util.List;

public class OptimizerManager {
    public void runBeforeRegisterAllocation(List<AsmInstruction> instrList) {
        new LI0ToX0Optimizer().runOn(instrList);
        new AddX0ToMvOptimizer().runOn(instrList);
        new StrengthReductionOptimizer().runOn(instrList);
        new DeadInstructionEliminationOptimizer().runOn(instrList);
    }

    public void runAfterRegisterAllocation(List<AsmInstruction> instrList) {
        new BranchEliminationOptimizer().runOn(instrList);
    }
}
