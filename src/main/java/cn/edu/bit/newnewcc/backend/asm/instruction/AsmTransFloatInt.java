package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmRTZ;
import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmTransFloatInt extends AsmInstruction {
    public AsmTransFloatInt(IntRegister result, FloatRegister source) {
        super("fcvt.w.s", result, source, new AsmRTZ());
    }
    public AsmTransFloatInt(FloatRegister result, IntRegister source) {
        super("fcvt.s.w", result, source, null);
    }
}
