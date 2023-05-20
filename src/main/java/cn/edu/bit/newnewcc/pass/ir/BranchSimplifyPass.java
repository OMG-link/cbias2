package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.constant.ConstBool;
import cn.edu.bit.newnewcc.ir.value.instruction.BranchInst;
import cn.edu.bit.newnewcc.ir.value.instruction.JumpInst;

/**
 * 分支简化 <br>
 * 对于分支条件为常量的分支语句，删除其无效的分支 <br>
 */
public class BranchSimplifyPass {

    private static void optimizeBasicBlock(BasicBlock basicBlock) {
        if (basicBlock.getTerminateInstruction() instanceof BranchInst branchInst && branchInst.getCondition() instanceof ConstBool condition) {
            BasicBlock savedBlock, removedBlock;
            if (condition.getValue()) {
                savedBlock = branchInst.getTrueExit();
                removedBlock = branchInst.getFalseExit();
            } else {
                savedBlock = branchInst.getFalseExit();
                removedBlock = branchInst.getTrueExit();
            }
            var jumpInst = new JumpInst(savedBlock);
            basicBlock.setTerminateInstruction(jumpInst);
            removedBlock.removeEntryFromPhi(basicBlock);
            branchInst.waste();
        }
    }

    private static void optimizeFunction(Function function) {
        function.getBasicBlocks().forEach(BranchSimplifyPass::optimizeBasicBlock);
    }

    public static void optimizeModule(Module module) {
        module.getFunctions().forEach(BranchSimplifyPass::optimizeFunction);
    }

}
