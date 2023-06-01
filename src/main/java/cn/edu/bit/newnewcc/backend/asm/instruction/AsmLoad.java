package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.*;

/**
 * 汇编部分中的load指令，在本语言中分为
 * <ul>
 *     <li>ld 双字加载，仅当加载64位的栈变量时使用</li>
 *     <li>lw 字加载，在加载任何变量时使用，默认指令均为lw</li>
 *     <li>lui 高位立即数加载，仅当加载全局符号地址中的高位时使用</li>
 *     <li>li 立即数加载</li>
 * </ul>
 */
public class AsmLoad extends AsmInstruction {
    /**
     * 创建一个汇编加载指令，将source中的内容加载到寄存器goal中
     * 注意，load一个地址的时候默认是将地址中的内容读出（一个字节），而非将地址的值读入
     *
     * @param goal   目标寄存器
     * @param source 加载内容源
     */
    public AsmLoad(Register goal, AsmOperand source) {
        super("lw", goal, source, null);
        if (goal.isInt()) {
            if (source.isImmediate()) {
                setInstructionName("li");
            } else if (source.isStackVar()) {
                StackVar stackVar = (StackVar) source;
                if (stackVar.getSize() == 8) {
                    setInstructionName("ld");
                } else if (stackVar.getSize() == 4) {
                    setInstructionName("lw");
                }
            } else if (source.isGlobalTag()) {
                GlobalTag globalTag = (GlobalTag) source;
                if (globalTag.isHighSegment()) {
                    setInstructionName("lui");
                } else {
                    setInstructionName("li");
                }
            } else if (source.isRegister() && ((Register) source).isInt()) {
                setInstructionName("c.mv");
            }
        } else {
            setInstructionName("flw");
            if (source.isImmediate()) {
                setInstructionName("fli");
            } else if (source.isStackVar()) {
                StackVar stackVar = (StackVar) source;
                if (stackVar.getSize() == 8) {
                    setInstructionName("fld");
                } else if (stackVar.getSize() == 4) {
                    setInstructionName("flw");
                }
            } else if (source.isRegister() && ((Register) source).isFloat()) {
                setInstructionName("fmv.d.x");
            }
        }
    }
}
