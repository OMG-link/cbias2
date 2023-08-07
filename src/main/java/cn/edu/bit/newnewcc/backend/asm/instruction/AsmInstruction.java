package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.*;

import java.util.Set;

/**
 * 汇编指令基类
 */
public abstract class AsmInstruction {
    private AsmOperand operand1, operand2, operand3;

    protected AsmInstruction(AsmOperand operand1, AsmOperand operand2, AsmOperand operand3) {
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
    }

    public void setOperand(int index, AsmOperand operand) {
        if (index == 1) {
            this.operand1 = operand;
        } else if (index == 2) {
            this.operand2 = operand;
        } else if (index == 3) {
            this.operand3 = operand;
        } else {
            throw new IndexOutOfBoundsException();
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

    public abstract String emit();

    public abstract Set<Register> getDef();

    public abstract Set<Register> getUse();

    public abstract boolean willReturn();

    public abstract boolean mayWriteToMemory();
}
