package com.bit.newnewcc.backend.asm.instruction;

import com.bit.newnewcc.backend.asm.operand.AsmOperand;
import com.bit.newnewcc.backend.asm.operand.GlobalTag;
import com.bit.newnewcc.backend.asm.operand.Register;
import com.bit.newnewcc.backend.asm.operand.StackVar;

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
     *
     * @param goal   目标寄存器
     * @param source 加载内容源
     */
    public AsmLoad(Register goal, AsmOperand source) {
        super("lw", goal, source, null);
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
        }
    }
}
