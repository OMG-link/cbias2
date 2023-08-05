package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAdd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.ImmediateTools;

import java.util.*;

public abstract class RegisterControl {
    protected final AsmFunction function;
    protected final Map<Register, StackVar> preservedRegisterSaved = new HashMap<>();
    protected final IntRegister s1 = IntRegister.s1;
    protected final StackVar s1saved;
    protected StackPool stackPool;

    public List<AsmInstruction> emitHead() {
        List<AsmInstruction> res = new ArrayList<>();
        for (var register : preservedRegisterSaved.keySet()) {
            var x = preservedRegisterSaved.get(register);
            if (!ImmediateTools.bitlengthNotInLimit(x.getAddress().getOffset())) {
                preservedRegisterSaved.put(s1, s1saved);
                break;
            }
        }
        for (var register : preservedRegisterSaved.keySet()) {
            saveToStackVar(res, register, preservedRegisterSaved.get(register));
        }
        return res;
    }

    public List<AsmInstruction> emitTail() {
        List<AsmInstruction> res = new ArrayList<>();
        for (var register : preservedRegisterSaved.keySet()) {
            loadFromStackVar(res, register, preservedRegisterSaved.get(register));
        }
        return res;
    }
    
    void updateRegisterPreserve(Register register) {
        if (register.isPreserved() && !preservedRegisterSaved.containsKey(register)) {
            preservedRegisterSaved.put(register, stackPool.pop());
        }
    }

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

    public abstract void virtualRegAllocateToPhysics();
    public abstract List<AsmInstruction> spillRegisters(List<AsmInstruction> instructionList);
}

