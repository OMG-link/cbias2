package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAdd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.max;

/**
 * 函数以函数名作为唯一标识符加以区分
 */
public class AsmFunction {
    String functionName;
    List<AsmOperand> parameters = new ArrayList<>();
    List<AsmBasicBlock> basicBlocks = new ArrayList<>();

    public class RegisterAllocator {
        Map<String, Register> registerMap;
        int total;
        RegisterAllocator() {
            total = 0;
            registerMap = new HashMap<>();
        }
        Register allocate(String name) {
            total -= 1;
            Register reg = new Register(total);
            registerMap.put(name, reg);
            return reg;
        }
        Register get(String name) {
            return registerMap.get(name);
        }
        boolean contain(String name) {
            return registerMap.containsKey(name);
        }
    }

    private class StackAllocator {
        private int top = 16, maxSize = 16;
        private boolean savedRa = false;

        /**
         * 进行函数调用前的准备，目前仅设置保存返回寄存器
         * @param originFunction 原函数
         */
        public void callFunction(AsmFunction originFunction) {
            savedRa = true;
        }

        /**
         * 在栈上申请一段长度为size的内存作为栈变量
         *
         * @param size 内存大小
         * @return 指向栈上对应的地址
         */
        public StackVar push(int size) {
            top += size;
            maxSize = max(maxSize, top);
            return new StackVar(-top, size);
        }

        /**
         * 释放大小为size的栈空间
         *
         * @param size 内存大小
         */
        public void pop(int size) {
            top -= size;
        }

        /**
         * 输出函数初始化栈帧的汇编代码
         * 注意，emit操作仅当maxSize初始化完成后进行，避免栈帧大小分配错误
         */
        public List<AsmInstruction> emitHead() {
            List<AsmInstruction> res = new ArrayList<>();
            Register sp = new Register("sp");
            Register ra = new Register("ra");
            Register s0 = new Register("s0");
            res.add(new AsmAdd(sp, sp, new Immediate(-maxSize)));
            if (savedRa) {
                res.add(new AsmStore(ra, new Address(maxSize - 8, sp)));
            }
            res.add(new AsmStore(s0, new Address(maxSize - 16, sp)));
            return res;
        }
        public List<AsmInstruction> emitTail() {
            List<AsmInstruction> res = new ArrayList<>();
            Register sp = new Register("sp");
            Register ra = new Register("ra");
            Register s0 = new Register("s0");
            if (savedRa) {
                res.add(new AsmLoad(ra, new Address(maxSize - 8, sp)));
            }
            res.add(new AsmLoad(s0, new Address(maxSize - 16, sp)));
            res.add(new AsmAdd(sp, sp, new Immediate(maxSize)));
            return res;
        }
    }
}
