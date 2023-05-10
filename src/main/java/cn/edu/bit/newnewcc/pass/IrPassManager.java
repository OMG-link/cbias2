package cn.edu.bit.newnewcc.pass;

import cn.edu.bit.newnewcc.ir.Module;

public class IrPassManager {

    public static void optimize(Module module, int optimizeLevel) {
        switch (optimizeLevel) {
            case 0 -> {
            }
            case 1 -> {
                //MemoryToRegisterPass.optimize(module);
            }
        }
    }

}
