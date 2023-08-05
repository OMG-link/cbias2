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
     * 创建一个汇编加载指令，将source中的内容加载到寄存器dest中
     * 注意，load一个地址的时候默认是将地址中的内容读出（一个字节），而非将地址的值读入
     *
     * @param dest   目标寄存器
     * @param source 加载内容源
     */
    public AsmLoad(Register dest, AsmOperand source) {
        super("lw", dest, source, null);
        if (dest.isInt()) {
            if (source instanceof Immediate) {
                setInstructionName("li");
            } else if (source instanceof StackVar) {
                StackVar stackVar = (StackVar) source;
                if (stackVar.getSize() == 8) {
                    setInstructionName("ld");
                } else if (stackVar.getSize() == 4) {
                    setInstructionName("lw");
                }
            } else if (source instanceof Label label) {
                if (label.isHighSegment()) {
                    setInstructionName("lui");
                } else if (label.isLowSegment()) {
                    setInstructionName("li");
                } else {
                    setInstructionName("la");
                }
            } else if (source instanceof Register && ((Register) source).isInt()) {
                setInstructionName("mv");
            } else if (source instanceof AddressDirective) {
                throw new RuntimeException("cannot load address to register by one instruction");
            }
        } else {
            setInstructionName("flw");
            if (source instanceof Register && ((Register) source).isFloat()) {
                setInstructionName("fmv.s");
            }
        }
    }
    public AsmLoad(Register dest, AsmOperand source, int bitLength) {
        this(dest, source);
        if (bitLength == 64 && getInstructionName().equals("lw")) {
            setInstructionName("ld");
        }
    }
}
