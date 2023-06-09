package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.HashMap;
import java.util.Map;

public class IntRegisterAllocator {
    Map<Instruction, IntRegister> registerMap;

    public IntRegisterAllocator() {
        registerMap = new HashMap<>();
    }

    public IntRegister allocate(Instruction instruction, int index) {
        IntRegister reg = new IntRegister(index);
        registerMap.put(instruction, reg);
        return reg;
    }

    public IntRegister allocate(int index) {
        return new IntRegister(index);
    }

    public IntRegister get(Instruction instruction) {
        return registerMap.get(instruction);
    }

    public boolean contain(Instruction instruction) {
        return registerMap.containsKey(instruction);
    }
}
