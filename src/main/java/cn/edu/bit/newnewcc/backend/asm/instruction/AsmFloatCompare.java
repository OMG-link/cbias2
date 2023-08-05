package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmFloatCompare extends AsmInstruction{
    public enum Condition {
        OEQ, OLT, OLE
    }
    public AsmFloatCompare(IntRegister dest, FloatRegister source1, FloatRegister source2, Condition condition) {
        super(null, dest, source1, source2);
        switch (condition) {
            case OEQ -> setInstructionName("feq.s");
            case OLT -> setInstructionName("flt.s");
            case OLE -> setInstructionName("fle.s");
        }
    }
}
