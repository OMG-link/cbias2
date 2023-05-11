package cn.edu.bit.newnewcc.pass;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.pass.ir.IrSemanticAnalysisPass;
import cn.edu.bit.newnewcc.pass.ir.MemoryToRegisterPass;

public class IrPassManager {

    public static void optimize(Module module, int optimizeLevel) {
        switch (optimizeLevel) {
            case 0 -> {
            }
            case 1 -> {
                MemoryToRegisterPass.optimize(module);
            }
        }
        IrSemanticAnalysisPass.verify(module);
    }

}
