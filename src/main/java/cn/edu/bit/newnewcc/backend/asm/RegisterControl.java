package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmCall;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

public abstract class RegisterControl {
    protected final AsmFunction function;
    protected StackPool stackPool;

    public abstract List<AsmInstruction> emitHead();
    public abstract List<AsmInstruction> emitTail();

    public RegisterControl(AsmFunction function, StackAllocator allocator) {
        this.function = function;
        stackPool = new StackPool(allocator);
    }

    public abstract void VritualRegAllocateToPhysics();

    public abstract List<AsmInstruction> spillRegisters(List<AsmInstruction> instructionList);
}
