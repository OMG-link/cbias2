package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.FloatRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;

public class AsmFloatCompare extends AsmInstruction{
    public enum Condition {
        OEQ, OLT, OLE
    }
    public AsmFloatCompare(IntRegister result, FloatRegister operand1, FloatRegister operand2, Condition condition) {
        super(null, result, operand1, operand2);
        switch (condition) {
            case OEQ -> setInstructionName("feq.s");
            case OLT -> setInstructionName("flt.s");
            case OLE -> setInstructionName("fle.s");
        }
    }
}
