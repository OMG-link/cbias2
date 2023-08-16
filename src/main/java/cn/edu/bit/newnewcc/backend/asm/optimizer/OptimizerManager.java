package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;

public class OptimizerManager {
    public void runBeforeRegisterAllocation(AsmFunction function) {
        while (new MvX0ToLI0Optimizer().runOn(function)
                || new LI0ToX0Optimizer().runOn(function)
                || new SSABasedOptimizer().runOn(function)) ;
        new StrengthReductionOptimizer().runOn(function);
        new DeadInstructionEliminationOptimizer().runOn(function);
    }

    public void runAfterRegisterAllocation(AsmFunction function) {
        new MemoryAccessEliminationOptimizer().runOn(function);
        new LAEliminationOptimizer().runOn(function);
        new MoveEliminationOptimizer().runOn(function);
        new BranchEliminationOptimizer().runOn(function);
        new DeadBlockEliminationOptimizer().runOn(function);
        new BlockInlineOptimizer(20).runOn(function);
        new DeadInstructionEliminationOptimizer().runOn(function);
        new DeadBlockEliminationOptimizer().runOn(function);
        new RedundantLabelEliminationOptimizer().runOn(function);
    }
}
