package cn.edu.bit.newnewcc.pass;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.pass.ir.*;

public class IrPassManager {

    public static void optimize(Module module, int optimizeLevel) {
        DeadCodeEliminationPass.runOnModule(module);
        LocalArrayInitializePass.runOnModule(module);
        switch (optimizeLevel) {
            case 0 -> {
            }
            case 1 -> {
                // 基本的结构为：运行一个无法确定优化优劣性的Pass，而后反复运行所有可以确定优化优劣性的Pass

                // 特别地，mem2reg只需要运行一次，因此也被放在了外面
                MemoryToRegisterPass.runOnModule(module);
                runOptimizePasses(module);

                // 内存访问优化需要搭配GCM运行，以确保各寄存器生命周期最小化，而GCM是无法确定是否产生优化的
                MemoryAccessOptimizePass.runOnModule(module);
                // todo: 添加语句重排，缩短活跃区间
                runOptimizePasses(module);

                // 加法合并无法确定是否产生了优化
                AddToMulPass.runOnModule(module);
                runOptimizePasses(module);

                // 加法合并无法确定是否产生了优化
                GlobalCodeMotionPass.runOnModule(module);
                runOptimizePasses(module);

            }
        }
        IrSemanticCheckPass.verify(module);
        IrNamingPass.runOnModule(module);
    }

    private static void runOptimizePasses(Module module) {
        while (true) {
            boolean changed = false;
            changed |= InstructionCombinePass.runOnModule(module);
            changed |= PatternReplacementPass.runOnModule(module);
            changed |= TailRecursionEliminationPass.runOnModule(module);
            changed |= FunctionInline.runOnModule(module);
            changed |= ConstantFoldingPass.runOnModule(module);
            changed |= BranchSimplifyPass.runOnModule(module);
            changed |= BasicBlockMergePass.runOnModule(module);
            changed |= DeadCodeEliminationPass.runOnModule(module);
            if (!changed) break;
        }
    }

}
