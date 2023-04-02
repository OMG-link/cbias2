package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.value.Instruction;

/**
 * 涉及内存的指令 <br>
 * 此类仅用于分类，无实际含义 <br>
 */
public abstract class MemoryInst extends Instruction {
    public MemoryInst(Type type) {
        super(type);
    }
}
