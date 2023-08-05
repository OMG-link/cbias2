package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerMultiplyInst;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerSignedDivideInst;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerSignedRemainderInst;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerSubInst;

/**
 * 常数取模强度削减
 */
// 将常数取模展开为除法，以便后端展开常数除法指令
public class ConstIntRemainderReducePass {

    public static boolean runOnInstruction(Instruction instruction) {
        if (!(instruction instanceof IntegerSignedRemainderInst integerSignedRemainderInst &&
                integerSignedRemainderInst.getOperand2() instanceof ConstInt)) return false;
        var a = integerSignedRemainderInst.getOperand1();
        var b = integerSignedRemainderInst.getOperand2();
        var inst1 = new IntegerSignedDivideInst(integerSignedRemainderInst.getType(), a, b);
        var inst2 = new IntegerMultiplyInst(integerSignedRemainderInst.getType(), inst1, b);
        var inst3 = new IntegerSubInst(integerSignedRemainderInst.getType(), a, inst2);
        inst1.insertBefore(integerSignedRemainderInst);
        inst2.insertBefore(integerSignedRemainderInst);
        inst3.insertBefore(integerSignedRemainderInst);
        integerSignedRemainderInst.replaceAllUsageTo(inst3);
        integerSignedRemainderInst.waste();
        return true;
    }

    public static boolean runOnBasicBlock(BasicBlock basicBlock) {
        boolean changed = false;
        for (Instruction instruction : basicBlock.getInstructions()) {
            changed |= runOnInstruction(instruction);
        }
        return changed;
    }

    public static boolean runOnFunction(Function function) {
        boolean changed = false;
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            changed |= runOnBasicBlock(basicBlock);
        }
        return changed;
    }

    public static boolean runOnModule(Module module) {
        boolean changed = false;
        for (Function function : module.getFunctions()) {
            changed |= runOnFunction(function);
        }
        return changed;
    }
}
