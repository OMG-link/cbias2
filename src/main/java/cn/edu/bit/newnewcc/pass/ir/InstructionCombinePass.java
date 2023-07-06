package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.pass.ir.structure.DomTree;
import cn.edu.bit.newnewcc.pass.ir.structure.InstructionSet;

/**
 * 指令合并 <br>
 * 根据支配关系删去多余指令 <br>
 */
public class InstructionCombinePass {
    private InstructionCombinePass() {
    }

    private final InstructionSet instructionSet = new InstructionSet();
    private DomTree domTree;

    private boolean runOnBasicBlock(BasicBlock basicBlock) {
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

    private boolean dfsDomTree(BasicBlock basicBlock) {
        boolean changed = false;
        for (Instruction instruction : basicBlock.getInstructions()) {
            if (instructionSet.contains(instruction)) {
                instruction.replaceAllUsageTo(instructionSet.get(instruction));
                instruction.waste();
                changed = true;
            } else {
                instructionSet.add(instruction);
            }
        }
        for (BasicBlock domSon : domTree.getDomSons(basicBlock)) {
            changed |= dfsDomTree(domSon);
        }
        for (Instruction instruction : basicBlock.getInstructions()) {
            instructionSet.remove(instruction);
        }
        return changed;
    }

    private boolean runOnFunction(Function function) {
        domTree = new DomTree(function);
        return dfsDomTree(function.getEntryBasicBlock());
    }

    public static boolean runOnModule(Module module) {
        boolean changed = false;
        for (Function function : module.getFunctions()) {
            var passInstance = new InstructionCombinePass();
            changed |= passInstance.runOnFunction(function);
        }
        return changed;
    }
}
