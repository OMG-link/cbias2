package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.util.Utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrengthReductionOptimizer implements Optimizer {
    @Override
    public boolean runOn(List<AsmInstruction> instrList) {
        boolean madeChange = false;
        Map<Register, Integer> values = new HashMap<>();

        for (int i = 0; i < instrList.size(); ++i) {
            AsmInstruction instr = instrList.get(i);

            if (instr instanceof AsmBlockEnd) continue;

            if (instr instanceof AsmLabel) {
                values.clear();
            } else if (instr instanceof AsmLoad loadInstr && loadInstr.getOpcode() == AsmLoad.Opcode.LI) {
                values.put((Register) loadInstr.getOperand(1), ((Immediate) loadInstr.getOperand(2)).getValue());
            } else {
                if (instr instanceof AsmMul mulInstr) {
                    var opcode = mulInstr.getOpcode();
                    if (opcode == AsmMul.Opcode.MUL || opcode == AsmMul.Opcode.MULW) {
                        IntRegister dest = (IntRegister) mulInstr.getOperand(1);
                        IntRegister source1 = (IntRegister) mulInstr.getOperand(2);
                        IntRegister source2 = (IntRegister) mulInstr.getOperand(3);
                        int bitLength = opcode == AsmMul.Opcode.MUL ? 64 : 32;

                        if (values.containsKey(source1)) {
                            int value = values.get(source1);
                            if (Utility.isPowerOf2(value)) {
                                instrList.set(i, new AsmShiftLeft(dest, source2, new Immediate(Utility.log2(value)), bitLength));
                                madeChange = true;
                            }
                        } else if (values.containsKey(source2)) {
                            int value = values.get(source2);
                            if (Utility.isPowerOf2(value)) {
                                instrList.set(i, new AsmShiftLeft(dest, source1, new Immediate(Utility.log2(value)), bitLength));
                                madeChange = true;
                            }
                        }
                    }
                }

                for (Register reg : instr.getDef()) {
                    values.remove(reg);
                }
            }
        }

        return madeChange;
    }
}
