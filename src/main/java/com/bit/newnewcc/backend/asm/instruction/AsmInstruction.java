package com.bit.newnewcc.backend.asm.instruction;
import com.bit.newnewcc.backend.asm.operand.AsmOperand;

// 汇编指令基类
public abstract class AsmInstruction {
    private final String instructionName;
    AsmOperand operand1, operand2, operand3;

    AsmInstruction(String instructionName, AsmOperand operand1, AsmOperand operand2, AsmOperand operand3) {
        this.instructionName = instructionName;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
    }

    abstract public String emit();
}
