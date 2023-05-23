package cn.edu.bit.newnewcc.pass;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.pass.ir.*;

public class IrPassManager {

    public static void optimize(Module module, int optimizeLevel) {
        DeadCodeEliminationPass.runOnModule(module);
        switch (optimizeLevel) {
            case 0 -> {
            }
            case 1 -> {
                MemoryToRegisterPass.runOnModule(module);
                while (true) {
                    boolean changed = false;
                    changed |= PatternReplacementPass.runOnModule(module);
                    changed |= ConstantFoldingPass.runOnModule(module);
                    changed |= BranchSimplifyPass.runOnModule(module);
                    changed |= BasicBlockMergePass.runOnModule(module);
                    changed |= DeadCodeEliminationPass.runOnModule(module);
                    if (!changed) break;
                }
            }
        }
        IrSemanticCheckPass.verify(module);
    }

}
