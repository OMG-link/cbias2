package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.Address;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.StackVar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.Integer.max;

public class StackAllocator {
    private int top = 16, maxSize = 16, backSize = 0, backMaxSize = 0;

    private boolean savedRa = false;

    /**
     * 进行函数调用前的准备，目前仅设置保存返回寄存器
     */
    public void callFunction() {
        savedRa = true;
        backSize = 0;
    }

    /**
     * 在栈上申请一段长度为size的内存作为栈变量
     *
     * @param size 内存大小
     * @return 指向栈上对应的地址
     */
    public StackVar push(int size) {
        if (size >= 8 && top % 8 != 0) {
            top += 4;
        }
        top += size;
        maxSize = max(maxSize, top);
        return new StackVar(-top, size, true);
    }

    /**
     * 在当前栈空间的顶部再push入内容，仅用于寄存器分配时的寄存器临时保存功能
     *
     * @param size 空间大小
     * @return 栈变量
     */
    public StackVar push_top(int size) {
        if (size >= 8 && maxSize % 8 != 0) {
            maxSize += 4;
        }
        maxSize += size;
        return new StackVar(-maxSize, size, true);
    }

    public void push_back(StackVar stackVar) {
        backSize += stackVar.getSize();
        backMaxSize = max(backMaxSize, backSize);
    }

    /**
     * 释放大小为size的栈空间
     *
     * @param size 内存大小
     */
    public void pop(int size) {
        top -= size;
    }

    public void add_padding() {
        maxSize += backMaxSize;
        maxSize += (16 - maxSize % 16) % 16;
    }

    /**
     * 输出函数初始化栈帧的汇编代码
     * <p>
     * 注意，emit操作仅当maxSize初始化完成后进行，避免栈帧大小分配错误
     */
    public Collection<AsmInstruction> emitHead() {
        add_padding();
        List<AsmInstruction> res = new ArrayList<>();
        IntRegister sp = new IntRegister("sp");
        IntRegister ra = new IntRegister("ra");
        IntRegister s0 = new IntRegister("s0");
        res.add(new AsmAdd(sp, sp, new Immediate(-maxSize)));
        if (savedRa) {
            res.add(new AsmStore(ra, new Address(maxSize - 8, sp)));
        }
        res.add(new AsmStore(s0, new Address(maxSize - 16, sp)));
        return res;
    }

    public Collection<AsmInstruction> emitTail() {
        List<AsmInstruction> res = new ArrayList<>();
        IntRegister sp = new IntRegister("sp");
        IntRegister ra = new IntRegister("ra");
        IntRegister s0 = new IntRegister("s0");
        if (savedRa) {
            res.add(new AsmLoad(ra, new Address(maxSize - 8, sp)));
        }
        res.add(new AsmLoad(s0, new Address(maxSize - 16, sp)));
        res.add(new AsmAdd(sp, sp, new Immediate(maxSize)));
        res.add(new AsmJump(new IntRegister("ra")));
        return res;
    }
}