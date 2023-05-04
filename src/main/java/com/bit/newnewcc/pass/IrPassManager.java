package com.bit.newnewcc.pass;

import com.bit.newnewcc.ir.Module;
import com.bit.newnewcc.pass.ir.MemoryToRegisterPass;

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
