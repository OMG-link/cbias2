package cn.edu.bit.newnewcc.backend.asm.optimizer;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAdd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmShiftLeft;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmShiftLeftAdd;
import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SLLIAddToShNAddOptimizer extends SSABasedOptimizer {

    private Pair<List<AsmInstruction>, Map<Register, Register>> tryOptimize(
            int bitWidth, IntRegister prevFinalRegister, AsmOperand potentialShiftOperand, AsmOperand addend) {
        if (!(potentialShiftOperand instanceof Register potentialShiftResultRegister)) return null;
        var valueSource = getValueSource(potentialShiftResultRegister);
        if (valueSource == null) return null;
        if (!(valueSource instanceof AsmShiftLeft asmShiftLeft)) return null;
        Immediate immediate = null;
        switch (asmShiftLeft.getOpcode()) {
            case SLL, SLLW -> {
                return null;
            }
            case SLLI -> {
                if (bitWidth != 64) return null;
                immediate = (Immediate) asmShiftLeft.getOperand(3);
            }
            case SLLIW -> {
                if (bitWidth != 32) return null;
                immediate = (Immediate) asmShiftLeft.getOperand(3);
            }
        }
        assert immediate != null;
        if (immediate.getValue() < 1 || immediate.getValue() > 3) return null;
        if (!(addend instanceof IntRegister) || !(asmShiftLeft.getOperand(2) instanceof IntRegister)) return null;
        IntRegister finalRegister = functionContext.getRegisterAllocator().allocateInt();
        AsmShiftLeftAdd asmShiftLeftAdd = new AsmShiftLeftAdd(
                immediate.getValue(),
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
    protected Pair<List<AsmInstruction>, Map<Register, Register>> getReplacement(AsmInstruction instruction) {
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
            var tryResult1 = tryOptimize(bitWidth, (IntRegister) prevFinalRegister, asmAdd.getOperand(2), asmAdd.getOperand(3));
            if (tryResult1 != null) return tryResult1;
            var tryResult2 = tryOptimize(bitWidth, (IntRegister) prevFinalRegister, asmAdd.getOperand(3), asmAdd.getOperand(2));
            if (tryResult2 != null) return tryResult2;
        }
        return null;
    }
}
