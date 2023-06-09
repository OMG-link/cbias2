package cn.edu.bit.newnewcc.backend.asm.instruction;


import cn.edu.bit.newnewcc.backend.asm.operand.*;

/**
 * 汇编部分中的save指令，在本语言中分为
 * <ul>
 *     <li>sd 双字保存，仅当保存一个64位的栈变量时使用</li>
 *     <li>sw 字保存，在保存任何变量时使用，默认指令均为sw</li>
 * </ul>
 */
public class AsmStore extends AsmInstruction {
    /**
     * 创建一个保存指令，将寄存器source中的内容保存至地址goal中，goal可能为栈变量或内存地址
     *
     * @param source 需要保存的寄存器
     * @param goal   保存到的目标地址
     */
    public AsmStore(Register source, AsmOperand goal) {
        super("sw", source, goal, null);
        if (source.isInt()) {
            if (goal.isStackVar()) {
                StackVar stackVar = (StackVar) goal;
                if (stackVar.getSize() == 8) {
                    setInstructionName("sd");
                }
            } else if (goal.isRegister()) {
                setInstructionName("c.mv");
                setOperand1(goal);
                setOperand2(source);
            }
        } else {
            setInstructionName("fsw");
            if (goal.isRegister()) {
                setInstructionName("fmv.s");
                setOperand1(goal);
                setOperand2(source);
            }
        }
    }
}
