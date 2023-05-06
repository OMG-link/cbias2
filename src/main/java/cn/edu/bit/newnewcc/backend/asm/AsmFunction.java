package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.StackVar;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.max;

public class AsmFunction {
    List<AsmOperand> parameters = new ArrayList<>();
    List<AsmBasicBlock> basicBlocks = new ArrayList<>();

    private class StackAllocator {
        private int top = 16, maxSize = 16;
        private boolean savedRa = false;

        /**
         * 进行函数调用前的准备，目前仅设置保存返回寄存器
         *
         * @param originFunction 原函数
         */
        public void callFunction(AsmFunction originFunction) {
            savedRa = true;
        }

        /**
         * 在栈上申请一段长度为size的内存作为栈变量
         *
         * @param size 内存大小
         * @return 栈变量
         */
        public StackVar push(int size) {
            top += size;
            maxSize = max(maxSize, top);
            return new StackVar(-top, new Register("s0"));
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
        public String emitHead() {
            String res = "";
            res += String.format("addi sp, sp, %d", -maxSize);
            if (savedRa) {
                res += String.format("sd ra, %d(sp)", maxSize - 8);
            }
            res += String.format("sd s0, %d(sp)", maxSize - 8);
            return res;
        }
    }
}
