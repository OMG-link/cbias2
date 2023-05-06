package cn.edu.bit.newnewcc.pass;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.pass.ir.MemoryToRegisterPass;

public class IrPassManager {
    private final int optimizeLevel;

    public IrPassManager(int optimizeLevel) {
        this.optimizeLevel = optimizeLevel;
    }

    public void optimize(Module module) {
        switch (optimizeLevel) {
            case 0 -> {
            }
            case 1 -> {
                MemoryToRegisterPass.optimize(module);
            }
        }
    }

}
