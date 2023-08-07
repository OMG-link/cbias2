package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAdd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmMove;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

import java.util.List;

public class AddX0ToMvOptimizer implements Optimizer {
    @Override
    public boolean runOn(List<AsmInstruction> instrList) {
        boolean madeChange = false;

        for (int i = 0; i < instrList.size(); ++i) {
            if (instrList.get(i) instanceof AsmAdd addInstr) {
                var opcode = addInstr.getOpcode();
                if (opcode == AsmAdd.Opcode.ADD || opcode == AsmAdd.Opcode.ADDW) {
                    if (addInstr.getOperand(2).equals(IntRegister.ZERO)) {
                        instrList.set(i, new AsmMove((Register) addInstr.getOperand(1), (Register) addInstr.getOperand(3)));
                        madeChange = true;
                    }
                    if (addInstr.getOperand(3).equals(IntRegister.ZERO)) {
                        instrList.set(i, new AsmMove((Register) addInstr.getOperand(1), (Register) addInstr.getOperand(2)));
                        madeChange = true;
                    }
                }
            }
        }

        return madeChange;
    }
}
