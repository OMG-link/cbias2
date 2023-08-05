package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmRTZ;
import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmTransFloatInt extends AsmInstruction {
    public AsmTransFloatInt(IntRegister dest, FloatRegister source) {
        super("fcvt.w.s", dest, source, new AsmRTZ());
    }
    public AsmTransFloatInt(FloatRegister dest, IntRegister source) {
        super("fcvt.s.w", dest, source, null);
    }
}
