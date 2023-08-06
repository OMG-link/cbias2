package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.Misc;

/**
 * 汇编指令基类
 */
public abstract class AsmInstruction {
    private String instructionName;
    private AsmOperand operand1, operand2, operand3;

    protected AsmInstruction(AsmOperand operand1, AsmOperand operand2, AsmOperand operand3) {
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
    }

    protected AsmInstruction(String instructionName, AsmOperand operand1, AsmOperand operand2, AsmOperand operand3) {
        this.instructionName = instructionName;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
    }

    protected void setInstructionName(String name) {
        this.instructionName = name;
    }

    protected String getInstructionName() {
        return instructionName;
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

    public void replaceOperand(int index, AsmOperand operand) {
        if (!(1 <= index && index <= 3)) {
            throw new IndexOutOfBoundsException();
        }
        if (index == 1) {
            this.operand1 = operand;
        } else if (index == 2) {
            this.operand2 = operand;
        } else {
            this.operand3 = operand;
        }
    }

    /**
     * 获取第index个参数
     * @param index 下标（1 <= index <= 3）
     * @return 参数值
     */
    public AsmOperand getOperand(int index) {
        if (index == 1) {
            return this.operand1;
        } else if (index == 2) {
            return this.operand2;
        } else if (index == 3) {
            return this.operand3;
        } else {
            throw new IndexOutOfBoundsException();
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
        if (!(this instanceof AsmLabel)) {
            res = '\t' + res;
        }
        return res + "\n";
    }

    @Override
    public String toString() {
        return Misc.deleteCharString(emit(), "\t\n");
    }
}
