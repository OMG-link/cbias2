package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAdd;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLoad;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.ImmediateValues;
import cn.edu.bit.newnewcc.backend.asm.util.Registers;

import java.util.*;

public abstract class RegisterControl {
    protected final AsmFunction function;
    protected final Map<Register, StackVar> preservedRegisterSaved = new HashMap<>();
    protected final StackPool stackPool;

    public List<AsmInstruction> emitPrologue() {
        List<AsmInstruction> instrList = new ArrayList<>();
        for (var register : preservedRegisterSaved.keySet()) {
            saveToStackVar(instrList, register, preservedRegisterSaved.get(register), IntRegister.getPhysical(5));
        }
        return Collections.unmodifiableList(instrList);
    }

    public List<AsmInstruction> emitEpilogue() {
        List<AsmInstruction> instrList = new ArrayList<>();
        for (var register : preservedRegisterSaved.keySet()) {
            loadFromStackVar(instrList, register, preservedRegisterSaved.get(register), IntRegister.getPhysical(5));
        }
        return Collections.unmodifiableList(instrList);
    }

    public void updateRegisterPreserve(Register register, StackVar saved) {
        if (Registers.isPreservedAcrossCalls(register) && !preservedRegisterSaved.containsKey(register)) {
            preservedRegisterSaved.put(register, saved);
        }
    }

    public RegisterControl(AsmFunction function, StackAllocator allocator) {
        this.function = function;
        stackPool = new StackPool(allocator);
    }

    public boolean loadFromStackVar(List<AsmInstruction> instList, Register register, StackVar stackVar, IntRegister tmpReg) {
        boolean tmpRegUsed = false;
        if (ImmediateValues.bitLengthNotInLimit(stackVar.getAddress().getOffset())) {
            instList.add(new AsmLoad(tmpReg, new Immediate(Math.toIntExact(stackVar.getAddress().getOffset()))));
            instList.add(new AsmAdd(tmpReg, tmpReg, stackVar.getAddress().getRegister(), 64));
            stackVar = new StackVar(0, stackVar.getSize(), true);
            stackVar = stackVar.replaceRegister(tmpReg);
            tmpRegUsed = true;
        }
        instList.add(new AsmLoad(register, stackVar));
        return tmpRegUsed;
    }

    public boolean saveToStackVar(List<AsmInstruction> instList, Register register, StackVar stackVar, IntRegister tmpReg) {
        boolean tmpRegUsed = false;
        if (ImmediateValues.bitLengthNotInLimit(stackVar.getAddress().getOffset())) {
            instList.add(new AsmLoad(tmpReg, new Immediate(Math.toIntExact(stackVar.getAddress().getOffset()))));
            instList.add(new AsmAdd(tmpReg, tmpReg, stackVar.getAddress().getRegister(), 64));
            stackVar = new StackVar(0, stackVar.getSize(), true);
            stackVar = stackVar.replaceRegister(tmpReg);
            tmpRegUsed = true;
        }
        instList.add(new AsmStore(register, stackVar));
        return tmpRegUsed;
    }

    public abstract List<AsmInstruction> work(List<AsmInstruction> instructionList);
}
