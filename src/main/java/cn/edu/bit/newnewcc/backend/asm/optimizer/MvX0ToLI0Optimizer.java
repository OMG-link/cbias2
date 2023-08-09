package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmMove;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.List;

public class MvX0ToLI0Optimizer implements Optimizer {
    @Override
    public boolean runOn(AsmFunction function) {
        List<AsmInstruction> instrList = function.getInstrList();

        int count = 0;

        for (int i = 0; i < instrList.size(); ++i) {
            AsmInstruction instr = instrList.get(i);
            if (instr instanceof AsmMove && instr.getOperand(2).equals(IntRegister.ZERO)) {
                instrList.set(i, new AsmLoad((Register) instr.getOperand(1), new Immediate(0)));
                ++count;
            }
        }

        return count > 0;
    }
}
