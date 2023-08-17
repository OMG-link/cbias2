package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.optimizer.ssabased.ISSABasedOptimizer;
import cn.edu.bit.newnewcc.backend.asm.optimizer.ssabased.OptimizeResult;
import cn.edu.bit.newnewcc.backend.asm.util.AsmInstructions;

public class LI0ToX0Optimizer implements ISSABasedOptimizer {

    @Override
    public OptimizeResult getReplacement(SSABasedOptimizer helper, AsmInstruction instruction) {
        for (int id : AsmInstructions.getReadRegId(instruction)) {
            var operand = instruction.getOperand(id);
            if (!(operand instanceof Register)) continue;
            if (helper.isConstIntOperand(operand) && helper.getConstIntValueFromOperand(operand) == 0) {
                instruction.setOperand(id, IntRegister.ZERO);
            }
        }
        return null;
    }
}
