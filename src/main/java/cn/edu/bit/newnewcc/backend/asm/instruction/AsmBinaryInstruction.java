package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;

public class AsmBinaryInstruction extends AsmInstruction {
    AsmBinaryInstruction(String instructionName, AsmOperand op1, AsmOperand op2, AsmOperand op3) {
        super(instructionName, op1, op2, op3);
    }
}
