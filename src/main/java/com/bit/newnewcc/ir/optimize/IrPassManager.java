package com.bit.newnewcc.ir.optimize;

import com.bit.newnewcc.ir.Module;

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
            }
        }
    }

}
