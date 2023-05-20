package cn.edu.bit.newnewcc.pass;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.pass.ir.*;

public class IrPassManager {

    public static void optimize(Module module, int optimizeLevel) {
        switch (optimizeLevel) {
            case 0 -> {
            }
            case 1 -> {
                MemoryToRegisterPass.runOnModule(module);
                while (true) {
                    boolean changed = false;
                    changed = changed | PatternReplacementPass.runOnModule(module);
                    changed = changed | ConstantFoldingPass.runOnModule(module);
                    changed = changed | BranchSimplifyPass.runOnModule(module);
                    changed = changed | DeadCodeEliminationPass.runOnModule(module);
                    if (!changed) break;
                }
            }
        }
        IrSemanticCheckPass.verify(module);
    }

}
