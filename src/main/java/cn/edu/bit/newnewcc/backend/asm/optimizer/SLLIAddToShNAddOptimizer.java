package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAdd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmShiftLeft;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmShiftLeftAdd;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

import java.util.ArrayList;
import java.util.List;

public class SLLIAddToShNAddOptimizer implements Optimizer {
    @Override
    public boolean runOn(AsmFunction function) {
        List<AsmInstruction> instrList = function.getInstrList();

        List<AsmInstruction> newInstrList = new ArrayList<>();
        int count = 0;

        for (int i = 0; i < instrList.size(); ++i) {
            boolean replaced = false;

            if (i + 1 < instrList.size()) {
                AsmInstruction curInstr = instrList.get(i);
                AsmInstruction nextInstr = instrList.get(i + 1);
                if (curInstr instanceof AsmShiftLeft
                    && ((AsmShiftLeft) curInstr).getOpcode() == AsmShiftLeft.Opcode.SLLI
                    && nextInstr instanceof AsmAdd
                    && ((AsmAdd) nextInstr).getOpcode() == AsmAdd.Opcode.ADD
                    && curInstr.getOperand(1).equals(nextInstr.getOperand(1))
                    && (
                        curInstr.getOperand(1).equals(nextInstr.getOperand(2))
                        || curInstr.getOperand(1).equals(nextInstr.getOperand(3))
                    )) {

                    int n = ((Immediate) curInstr.getOperand(3)).getValue();
                    if (1 <= n && n <= 3) {
                        AsmInstruction newInstr;

                        if (curInstr.getOperand(1).equals(nextInstr.getOperand(2))) {
                            newInstr = new AsmShiftLeftAdd(
                                n,
                                (IntRegister) nextInstr.getOperand(1),
                                (IntRegister) curInstr.getOperand(2),
                                (IntRegister) nextInstr.getOperand(3)
                            );
                        } else {
                            newInstr = new AsmShiftLeftAdd(
                                n,
                                (IntRegister) nextInstr.getOperand(1),
                                (IntRegister) curInstr.getOperand(2),
                                (IntRegister) nextInstr.getOperand(2)
                            );
                        }

                        newInstrList.add(newInstr);
                        ++i;
                        replaced = true;

                        ++count;
                    }
                }
            }
            if (!replaced) {
                newInstrList.add(instrList.get(i));
            }
        }

        instrList.clear();
        instrList.addAll(newInstrList);

        return count > 0;
    }
}
