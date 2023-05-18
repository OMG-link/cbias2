package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.HashMap;
import java.util.Map;

public class IntRegisterAllocator {
    Map<Instruction, IntRegister> registerMap;
    int total;

    public IntRegisterAllocator() {
        total = 0;
        registerMap = new HashMap<>();
    }

    public IntRegister allocate(Instruction instruction) {
        total -= 1;
        IntRegister reg = new IntRegister(total);
        registerMap.put(instruction, reg);
        return reg;
    }

    public IntRegister allocate() {
        total -= 1;
        return new IntRegister(total);
    }

    public IntRegister get(Instruction instruction) {
        return registerMap.get(instruction);
    }

    public boolean contain(Instruction instruction) {
        return registerMap.containsKey(instruction);
    }
}
