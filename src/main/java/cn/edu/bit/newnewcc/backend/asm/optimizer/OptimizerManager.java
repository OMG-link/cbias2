package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;

public class OptimizerManager {
    public void runBeforeRegisterAllocation(AsmFunction function) {
        new LI0ToX0Optimizer().runOn(function);
        new AddX0ToMvOptimizer().runOn(function);
        new StrengthReductionOptimizer().runOn(function);
        new DeadInstructionEliminationOptimizer().runOn(function);
    }

    public void runAfterRegisterAllocation(AsmFunction function) {
        new MoveEliminationOptimizer().runOn(function);
        new BranchEliminationOptimizer().runOn(function);
        new DeadBlockEliminationOptimizer().runOn(function);
        for (int i = 0; i < 6; ++i) {
            new BlockInlineOptimizer(20).runOn(function);
        }
        new DeadBlockEliminationOptimizer().runOn(function);
    }
}
