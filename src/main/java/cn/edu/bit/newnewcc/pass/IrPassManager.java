package cn.edu.bit.newnewcc.pass;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.pass.ir.*;

public class IrPassManager {

    public static void optimize(Module module, int optimizeLevel) {
        switch (optimizeLevel) {
            case 0 -> {
            }
            case 1 -> {
                MemoryToRegisterPass.optimizeModule(module);
                while (true) {
                    boolean changed = false;
                    changed = changed | PatternReplacementPass.optimizeModule(module);
                    changed = changed | ConstantFoldingPass.optimizeModule(module);
                    changed = changed | BranchSimplifyPass.optimizeModule(module);
                    changed = changed | DeadCodeEliminationPass.optimizeModule(module);
                    if (!changed) break;
                }
            }
        }
        IrSemanticCheckPass.verify(module);
    }

}
