package cn.edu.bit.newnewcc.backend.asm.optimizer.ssabased;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.optimizer.SSABasedOptimizer;

import java.util.HashMap;
import java.util.Map;

public class MergeRecentlyUsedLa implements ISSABasedOptimizer {
    Map<Label, IntRegister> addressSavedMap = new HashMap<>();
    Map<Label, Integer> addressTimeStamp = new HashMap<>();
    final int TimeLimit = 10;
    int instCounter;

    @Override
    public void setBlockBegins() {
        ISSABasedOptimizer.super.setBlockBegins();
        instCounter = 0;
        addressSavedMap.clear();
        addressTimeStamp.clear();
    }

    @Override
    public OptimizeResult getReplacement(SSABasedOptimizer ssaBasedOptimizer, AsmInstruction instruction) {
        instCounter++;
        if (instruction instanceof AsmLoad asmLoad && asmLoad.getOpcode().equals(AsmLoad.Opcode.LA)) {
            IntRegister resultReg = (IntRegister) instruction.getOperand(1);
            Label addressLabel = (Label) asmLoad.getOperand(2);
            if (addressTimeStamp.containsKey(addressLabel) && instCounter <= addressTimeStamp.get(addressLabel) + TimeLimit) {
                IntRegister previousReg = addressSavedMap.get(addressLabel);
                if (!previousReg.equals(resultReg)) {
                    OptimizeResult result = OptimizeResult.getNew();
                    result.addRegisterMapping(resultReg, previousReg);
                    return result;
                }
            } else {
                addressSavedMap.put(addressLabel, resultReg);
            }
            addressTimeStamp.put(addressLabel, instCounter);
            return null;
        }
        return null;
    }
}
