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

                // 循环展开并执行GCM
                LoopUnrollPass.runOnModule(module);
                GlobalCodeMotionPass.runOnModule(module);
                runOptimizePasses(module);

                // 内存访问优化运行单次即可
                MemoryAccessOptimizePass.runOnModule(module);
                runOptimizePasses(module);

                // 加法合并无法确定是否产生了优化
                AddToMulPass.runOnModule(module);
                GlobalCodeMotionPass.runOnModule(module);
                runOptimizePasses(module);

                // 最后调整一下指令顺序，以得到更好的寄存器分配结果
                InstructionSchedulePass.runOnModule(module);

            }
        }
        IrSemanticCheckPass.verify(module);
        IrNamingPass.runOnModule(module);
    }

    private static void runOptimizePasses(Module module) {
        while (true) {
            boolean changed;
            changed = InstructionCombinePass.runOnModule(module);
            changed |= PatternReplacementPass.runOnModule(module);
            changed |= TailRecursionEliminationPass.runOnModule(module);
            changed |= FunctionInline.runOnModule(module);
            changed |= GvToLvPass.runOnModule(module);
            changed |= ConstLoopUnrollPass.runOnModule(module);
            changed |= ConstantFoldingPass.runOnModule(module);
            changed |= LocalArrayPromotionPass.runOnModule(module);
            changed |= ConstArrayInlinePass.runOnModule(module);
            changed |= ArrayOffsetCompressPass.runOnModule(module);
            changed |= BranchSimplifyPass.runOnModule(module);
            changed |= BasicBlockMergePass.runOnModule(module);
            changed |= DeadCodeEliminationPass.runOnModule(module);
            if (!changed) break;
        }
    }

}
