package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.AddressContent;
import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;

/**
 * 汇编指令基类
 */
public class AsmInstruction {
    private String instructionName;
    AsmOperand operand1, operand2, operand3;

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

    public void setOperandRegister(int j, Register register) {
        if (j == 1) {
            if (operand1.isRegister()) {
                operand1 = register;
            } else if (operand1 instanceof AddressContent addressContent) {
                operand1 = new AddressContent(addressContent.getOffset(), (IntRegister) register);
            }
        } else if (j == 2) {
            if (operand2.isRegister()) {
                operand2 = register;
            } else if (operand2 instanceof AddressContent addressContent) {
                operand2 = new AddressContent(addressContent.getOffset(), (IntRegister) register);
            }
        } else if (j == 3) {
            if (operand3.isRegister()) {
                operand3 = register;
            } else if (operand3 instanceof AddressContent addressContent) {
                operand3 = new AddressContent(addressContent.getOffset(), (IntRegister) register);
            }
        } else {
            throw new RuntimeException("get error operand index");
        }
    }

    public AsmOperand getOperand(int index) {
        if (index == 1) {
            return this.operand1;
        } else if (index == 2) {
            return this.operand2;
        } else if (index == 3) {
            return this.operand3;
        } else {
            throw new RuntimeException("Asm Operand index error : " + index);
        }
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
        return res + "\n";
    }
}
