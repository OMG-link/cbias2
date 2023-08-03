package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.allocator.RegisterAllocator;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAdd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.Immediate;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.StackVar;
import cn.edu.bit.newnewcc.backend.asm.util.ImmediateTools;

import java.util.*;

public abstract class RegisterControl {
    protected final AsmFunction function;
    //寄存器在调用过程中保留与否，保留的寄存器需要在函数头尾额外保存
    protected final Map<Register, LinearScanRegisterControl.TYPE> registerPreservedType = new HashMap<>();
    protected final Map<Register, StackVar> preservedRegisterSaved = new HashMap<>();
    protected final IntRegister s1 = RegisterAllocator.s1;
    protected final StackVar s1saved;
    protected StackPool stackPool;

    public abstract List<AsmInstruction> emitHead();
    public abstract List<AsmInstruction> emitTail();
    public abstract void virtualRegAllocateToPhysics();

    public RegisterControl(AsmFunction function, StackAllocator allocator) {
        this.function = function;
        stackPool = new StackPool(allocator);
        s1saved = stackPool.pop();
    }

    void loadFromStackVar(List<AsmInstruction> instList, Register register, StackVar stk) {
        if (ImmediateTools.bitlengthNotInLimit(stk.getAddress().getOffset())) {
            preservedRegisterSaved.put(s1, s1saved);
            instList.add(new AsmLoad(s1, new Immediate(Math.toIntExact(stk.getAddress().getOffset()))));
            instList.add(new AsmAdd(s1, s1, stk.getAddress().getRegister()));
            stk = new StackVar(0, stk.getSize(), true);
            stk = stk.replaceRegister(s1);
        }
        instList.add(new AsmLoad(register, stk));
    }

    void saveToStackVar(List<AsmInstruction> instList, Register register, StackVar stk) {
        if (ImmediateTools.bitlengthNotInLimit(stk.getAddress().getOffset())) {
            preservedRegisterSaved.put(s1, s1saved);
            instList.add(new AsmLoad(s1, new Immediate(Math.toIntExact(stk.getAddress().getOffset()))));
            instList.add(new AsmAdd(s1, s1, stk.getAddress().getRegister()));
            stk = new StackVar(0, stk.getSize(), true);
            stk = stk.replaceRegister(s1);
        }
        instList.add(new AsmStore(register, stk));
    }

    public abstract List<AsmInstruction> spillRegisters(List<AsmInstruction> instructionList);
}
