package com.bit.newnewcc.backend.asm.instruction;
import com.bit.newnewcc.backend.asm.operand.AsmOperand;

/**
 * 汇编指令基类
 */
public class AsmInstruction {
    private String instructionName;
    AsmOperand operand1, operand2, operand3;

    public AsmInstruction() {
        this.instructionName = null;
        this.operand1 = null;
        this.operand2 = null;
        this.operand3 = null;
    }

    public AsmInstruction(String instructionName, AsmOperand operand1, AsmOperand operand2, AsmOperand operand3) {
        this.instructionName = instructionName;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
    }

    protected void setInstructionName(String name) {
        this.instructionName = name;
    }

    protected void setOperand1(AsmOperand operand1) {
        this.operand1 = operand1;
    }

    protected void setOperand2(AsmOperand operand2) {
        this.operand2 = operand2;
    }

    protected void setOperand3(AsmOperand operand3) {
        this.operand3 = operand3;
    }

    /**
     * 指令输出函数，依次输出指令名称及参数
     */
    public String emit() {
        String res = instructionName;
        if (operand1 != null) {
            res += " " + operand1.emit();
            if (operand2 != null) {
                res += ", " + operand2.emit();
                if (operand3 != null) {
                    res += ", " + operand3.emit();
                }
            }
        }
        return res;
    }
}
