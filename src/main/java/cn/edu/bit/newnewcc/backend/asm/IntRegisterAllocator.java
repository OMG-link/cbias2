package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.HashMap;
import java.util.Map;

public class IntRegisterAllocator {
    Map<Instruction, IntRegister> registerMap;
    int total;

    IntRegisterAllocator() {
        total = 0;
        registerMap = new HashMap<>();
    }

    IntRegister allocate(Instruction instruction) {
        total -= 1;
        IntRegister reg = new IntRegister(total);
        registerMap.put(instruction, reg);
        return reg;
    }

    IntRegister allocate() {
        total -= 1;
        return new IntRegister(total);
    }

    IntRegister get(Instruction instruction) {
        return registerMap.get(instruction);
    }

    boolean contain(Instruction instruction) {
        return registerMap.containsKey(instruction);
    }
}
