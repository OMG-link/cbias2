package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.StackVar;
import cn.edu.bit.newnewcc.backend.asm.util.ImmediateTools;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.*;

import static java.lang.Integer.max;

/**
 * 栈空间分配器
 */
public class StackAllocator {
    private int top = 16, maxSize = 16, backSize = 0, backMaxSize = 0;
    private boolean savedRa = false;
    private Map<Instruction, StackVar> stackVarMap = new HashMap<>();

    public StackVar allocate(Instruction instruction, int size) {
        StackVar res = push(size);
        stackVarMap.put(instruction, res);
        return res;
    }

    public StackVar get(Instruction instruction) {
        return stackVarMap.get(instruction);
    }

    public boolean contain(Instruction instruction) {
        return stackVarMap.containsKey(instruction);
    }

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
     * 直接在栈帧顶部分配空间，用于寄存器分配时存储额外寄存器
     */
    int exSize = 0;
    public int getExSize() {
        return exSize;
    }
    public StackVar push_ex() {
        maxSize += 8;
        exSize += 8;
        return new StackVar(-exSize - 16, 8, true);
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
        IntRegister t0 = new IntRegister("t0");
        if (savedRa) {
            res.add(new AsmStore(ra, new StackVar(-8, 8, false)));
        }
        res.add(new AsmStore(s0, new StackVar(-16, 8, false)));
        if (ImmediateTools.bitlengthNotInLimit(maxSize)) {
            res.add(new AsmLoad(t0, new Immediate(maxSize)));
            res.add(new AsmSub(sp, sp, t0, 64));
            res.add(new AsmAdd(s0, sp, t0));
        } else {
            res.add(new AsmAdd(sp, sp, new Immediate(-maxSize)));
            res.add(new AsmAdd(s0, sp, new Immediate(maxSize)));
        }
        return res;
    }

    public Collection<AsmInstruction> emitTail() {
        List<AsmInstruction> res = new ArrayList<>();
        IntRegister sp = new IntRegister("sp");
        IntRegister ra = new IntRegister("ra");
        IntRegister s0 = new IntRegister("s0");
        IntRegister t0 = new IntRegister("t0");
        if (ImmediateTools.bitlengthNotInLimit(maxSize)) {
            res.add(new AsmLoad(t0, new Immediate(maxSize)));
            res.add(new AsmAdd(sp, sp, t0));
        } else {
            res.add(new AsmAdd(sp, sp, new Immediate(maxSize)));
        }
        if (savedRa) {
            res.add(new AsmLoad(ra, new StackVar(-8, 8, false)));
        }
        res.add(new AsmLoad(s0, new StackVar(-16, 8, false)));
        res.add(new AsmJump(new IntRegister("ra")));
        return res;
    }
}
