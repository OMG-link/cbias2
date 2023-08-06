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
     * 创建一个保存指令，将寄存器source中的内容保存至地址dest中，dest可能为栈变量或内存地址
     *
     * @param source 需要保存的寄存器
     * @param dest   保存到的目标地址
     */
    public AsmStore(Register source, AsmOperand dest) {
        super("sw", source, dest, null);
        if (source.isInt()) {
            if (dest instanceof StackVar stackVar) {
                if (stackVar.getSize() == 8) {
                    setInstructionName("sd");
                }
            } else if (dest instanceof Register) {
                setInstructionName("mv");
            }
        } else {
            setInstructionName("fsw");
            if (dest instanceof Register) {
                setInstructionName("fmv.s");
            }
        }
    }

    public AsmStore(Register source, AsmOperand dest, int bitLength) {
        this(source, dest);
        if (bitLength == 64 && getInstructionName().equals("sw")) {
            setInstructionName("sd");
        }
    }

    @Override
    public String emit() {
        String res = getInstructionName();
        if (AsmInstructions.isMove(this)) {
            res += " " + getOperand(2).emit() + ", " + getOperand(1).emit();
        } else {
            res += " " + getOperand(1).emit() + ", " + getOperand(2).emit();
        }
        return "\t" + res + "\n";
    }
}
