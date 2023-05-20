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
                PatternReplacementPass.optimizeModule(module);
                ConstantFoldingPass.optimizeModule(module);
                BranchSimplifyPass.optimizeModule(module);
                DeadCodeEliminationPass.optimizeModule(module);
            }
        }
        IrSemanticCheckPass.verify(module);
    }

}
