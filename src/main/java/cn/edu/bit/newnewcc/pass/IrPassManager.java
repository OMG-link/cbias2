package cn.edu.bit.newnewcc.pass;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.pass.ir.*;

public class IrPassManager {

    public static void optimize(Module module, int optimizeLevel) {
        switch (optimizeLevel) {
            case 0 -> {
            }
            case 1 -> {
                MemoryToRegisterPass.optimize(module);
                PatternReplacementPass.optimize(module);
                BranchSimplifyPass.optimize(module);
                DeadCodeEliminationPass.optimize(module);
            }
        }
        IrSemanticCheckPass.verify(module);
    }

}
