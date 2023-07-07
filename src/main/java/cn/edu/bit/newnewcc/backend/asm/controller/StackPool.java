package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.operand.StackVar;

import java.util.ArrayDeque;
import java.util.Queue;

class StackPool {
    StackAllocator allocator;
    Queue<StackVar> queue = new ArrayDeque<>();

    public StackPool(StackAllocator allocator) {
        this.allocator = allocator;
    }

    void push(StackVar stackVar) {
        queue.add(stackVar);
    }

    public void clear() {
        queue.clear();
    }

    StackVar pop() {
        if (queue.isEmpty()) {
            return allocator.push_ex();
        }
        return queue.remove();
    }
}
