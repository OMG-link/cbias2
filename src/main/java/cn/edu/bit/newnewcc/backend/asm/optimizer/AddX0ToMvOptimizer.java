package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAdd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmMove;
import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

public class AddX0ToMvOptimizer implements ISSABasedOptimizer {

    private SSABasedOptimizer.Result tryOptimize(SSABasedOptimizer ssaBasedOptimizer, IntRegister prevFinalRegister,
                                                 AsmOperand addend1, AsmOperand addend2) {
        if (ssaBasedOptimizer.isConstIntOperand(addend2)) {
            var immediateValue = ssaBasedOptimizer.getConstIntValueFromOperand(addend2);
            if (immediateValue == 0) {
                var result = SSABasedOptimizer.Result.getNew();
                var newFinalRegister = ssaBasedOptimizer.functionContext.getRegisterAllocator().allocateInt();
                result.addInstruction(new AsmMove(newFinalRegister, (Register) addend1));
                result.addRegisterMapping(prevFinalRegister, newFinalRegister);
                return result;
            }
        }
        return null;
    }

    @Override
    public SSABasedOptimizer.Result getReplacement(SSABasedOptimizer ssaBasedOptimizer, AsmInstruction instruction) {
        if (instruction instanceof AsmAdd asmAdd) {
            var tryResult1 = tryOptimize(
                    ssaBasedOptimizer,
                    (IntRegister) asmAdd.getOperand(1),
                    asmAdd.getOperand(2),
                    asmAdd.getOperand(3)
            );
            if (tryResult1 != null) return tryResult1;
            var tryResult2 = tryOptimize(
                    ssaBasedOptimizer,
                    (IntRegister) asmAdd.getOperand(1),
                    asmAdd.getOperand(3),
                    asmAdd.getOperand(2)
            );
            if (tryResult2 != null) return tryResult2;
        }
        return null;
    }

}
