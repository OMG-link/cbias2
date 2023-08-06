package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.List;

public class OptimizerManager {
    public void runOn(List<AsmInstruction> instrList) {
        //instrList.remove(118);
        //instrList.get(118).replaceOperand(2, IntRegister.ZERO);
    }
}
