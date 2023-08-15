package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAdd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmShiftLeft;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmShiftLeftAdd;
import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SLLIAddToShNAddOptimizer implements ISSABasedOptimizer {

    public Pair<List<AsmInstruction>, Map<Register, Register>> tryOptimize(
            SSABasedOptimizer ssaBasedOptimizer, IntRegister prevFinalRegister, AsmOperand potentialShiftOperand, AsmOperand addend) {
        if (!(potentialShiftOperand instanceof Register potentialShiftResultRegister)) return null;
        var valueSource = ssaBasedOptimizer.getValueSource(potentialShiftResultRegister);
        if (valueSource == null) return null;
        if (!(valueSource instanceof AsmShiftLeft asmShiftLeft)) return null;
        var immediateOperand = asmShiftLeft.getOperand(3);
        if (!ssaBasedOptimizer.isConstIntOperand(immediateOperand)) return null;
        var immediateValue = ssaBasedOptimizer.getConstIntValueFromOperand(immediateOperand);
        if (immediateValue < 1 || immediateValue > 3) return null;
        IntRegister finalRegister = ssaBasedOptimizer.functionContext.getRegisterAllocator().allocateInt();
        AsmShiftLeftAdd asmShiftLeftAdd = new AsmShiftLeftAdd(
                immediateValue,
                finalRegister,
                (IntRegister) asmShiftLeft.getOperand(2),
                (IntRegister) addend
        );
        var list = new ArrayList<AsmInstruction>();
        list.add(asmShiftLeftAdd);
        var mapping = new HashMap<Register, Register>();
        mapping.put(prevFinalRegister, finalRegister);
        return new Pair<>(list, mapping);
    }

    @Override
    public Pair<List<AsmInstruction>, Map<Register, Register>> getReplacement(
            SSABasedOptimizer ssaBasedOptimizer, AsmInstruction instruction) {
        if (instruction instanceof AsmAdd asmAdd) {
            int bitWidth = switch (asmAdd.getOpcode()) {
                case ADD, ADDI -> 64;
                case ADDW, ADDIW -> 32;
                case FADDS -> -1; // 标记为不可优化
            };
            if (bitWidth == -1) return null;
            // 检查每个加法操作数是否是由位移指令得来
            var prevFinalRegister = asmAdd.getOperand(1);
            if (!(prevFinalRegister instanceof IntRegister)) return null;
            var tryResult1 = tryOptimize(ssaBasedOptimizer, (IntRegister) prevFinalRegister, asmAdd.getOperand(2), asmAdd.getOperand(3));
            if (tryResult1 != null) return tryResult1;
            var tryResult2 = tryOptimize(ssaBasedOptimizer, (IntRegister) prevFinalRegister, asmAdd.getOperand(3), asmAdd.getOperand(2));
            if (tryResult2 != null) return tryResult2;
        }
        return null;
    }

}
