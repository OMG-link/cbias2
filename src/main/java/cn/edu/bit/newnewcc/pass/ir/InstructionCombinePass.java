package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.pass.ir.structure.InstructionSet;

/**
 * 指令合并 <br>
 * 合并同一基本块内的等效指令 <br>
 */
public class InstructionCombinePass {
    private static boolean runOnBasicBlock(BasicBlock basicBlock) {
        boolean changed = false;
        var instructionSet = new InstructionSet();
        for (Instruction instruction : basicBlock.getInstructions()) {
            if (instructionSet.contains(instruction)) {
                instruction.replaceAllUsageTo(instructionSet.get(instruction));
                instruction.waste();
                changed = true;
            } else {
                instructionSet.add(instruction);
            }
        }
        return changed;
    }

    public static boolean runOnModule(Module module) {
        boolean changed = false;
        for (Function function : module.getFunctions()) {
            for (BasicBlock basicBlock : function.getBasicBlocks()) {
                changed |= runOnBasicBlock(basicBlock);
            }
        }
        return changed;
    }
}
