package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;

import java.util.*;

public abstract class RegisterControl {
    protected final AsmFunction function;
    protected StackPool stackPool;

    public abstract List<AsmInstruction> emitHead();
    public abstract List<AsmInstruction> emitTail();
    public abstract void virtualRegAllocateToPhysics();

    public RegisterControl(AsmFunction function, StackAllocator allocator) {
        this.function = function;
        stackPool = new StackPool(allocator);
    }

    public abstract List<AsmInstruction> spillRegisters(List<AsmInstruction> instructionList);
}
