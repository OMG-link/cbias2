package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.ir.value.instruction.IntegerCompareInst;

public class AsmIntegerCompare extends AsmInstruction {
    public enum Condition {
        SEQZ, SNEZ, SLT
    }
    Condition condition;
    public AsmIntegerCompare(IntRegister dest, IntRegister source1, AsmOperand source2, Condition condition) {
        super(null, dest, source1, source2);
        this.condition = condition;
        switch (condition) {
            case SEQZ:
                setInstructionName("seqz");
                break;
            case SNEZ:
                setInstructionName("snez");
                break;
            case SLT:
                if (source2 instanceof Immediate) {
                    setInstructionName("slti");
                } else {
                    setInstructionName("slt");
                }
                break;
        }
    }
}
