package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAdd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.Immediates;
import cn.edu.bit.newnewcc.backend.asm.util.Registers;

import java.util.*;

public abstract class RegisterControl {
    protected final AsmFunction function;
    protected final Map<Register, StackVar> preservedRegisterSaved = new HashMap<>();
    protected final IntRegister s1 = IntRegister.S1;
    protected final StackVar s1saved;
    protected final StackPool stackPool;

    public List<AsmInstruction> emitPrologue() {
        List<AsmInstruction> instrList = new ArrayList<>();
        for (var register : preservedRegisterSaved.keySet()) {
            var x = preservedRegisterSaved.get(register);
            if (!Immediates.bitLengthNotInLimit(x.getAddress().getOffset())) {
                preservedRegisterSaved.put(s1, s1saved);
                break;
            }
        }
        for (var register : preservedRegisterSaved.keySet()) {
            saveToStackVar(instrList, register, preservedRegisterSaved.get(register));
        }
        return Collections.unmodifiableList(instrList);
    }

    public List<AsmInstruction> emitEpilogue() {
        List<AsmInstruction> instrList = new ArrayList<>();
        for (var register : preservedRegisterSaved.keySet()) {
            loadFromStackVar(instrList, register, preservedRegisterSaved.get(register));
        }
        return Collections.unmodifiableList(instrList);
    }

    public void updateRegisterPreserve(Register register) {
        if (Registers.isPreservedAcrossCalls(register) && !preservedRegisterSaved.containsKey(register)) {
            preservedRegisterSaved.put(register, stackPool.pop());
        }
    }

    public RegisterControl(AsmFunction function, StackAllocator allocator) {
        this.function = function;
        stackPool = new StackPool(allocator);
        s1saved = stackPool.pop();
    }

    public void loadFromStackVar(List<AsmInstruction> instList, Register register, StackVar stackVar) {
        if (Immediates.bitLengthNotInLimit(stackVar.getAddress().getOffset())) {
            preservedRegisterSaved.put(s1, s1saved);
            instList.add(new AsmLoad(s1, new Immediate(Math.toIntExact(stackVar.getAddress().getOffset()))));
            instList.add(new AsmAdd(s1, s1, stackVar.getAddress().getRegister(), 64));
            stackVar = new StackVar(0, stackVar.getSize(), true);
            stackVar = stackVar.replaceRegister(s1);
        }
        instList.add(new AsmLoad(register, stackVar));
    }

    public void saveToStackVar(List<AsmInstruction> instList, Register register, StackVar stackVar) {
        if (Immediates.bitLengthNotInLimit(stackVar.getAddress().getOffset())) {
            preservedRegisterSaved.put(s1, s1saved);
            instList.add(new AsmLoad(s1, new Immediate(Math.toIntExact(stackVar.getAddress().getOffset()))));
            instList.add(new AsmAdd(s1, s1, stackVar.getAddress().getRegister(), 64));
            stackVar = new StackVar(0, stackVar.getSize(), true);
            stackVar = stackVar.replaceRegister(s1);
        }
        instList.add(new AsmStore(register, stackVar));
    }

    public abstract List<AsmInstruction> work(List<AsmInstruction> instructionList);
}
