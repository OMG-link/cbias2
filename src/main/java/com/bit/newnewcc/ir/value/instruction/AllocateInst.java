package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.type.PointerType;

import java.util.ArrayList;
import java.util.List;

/**
 * 栈空间分配指令
 */
public class AllocateInst extends MemoryInst{
    public AllocateInst(Type allocatedType){
        super(PointerType.getInstance(allocatedType));
    }

    @Override
    public PointerType getType() {
        return (PointerType) super.getType();
    }

    @Override
    public List<Operand> getOperandList() {
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format(
                "%s = alloca %s",
                this.getValueNameIR(),
                this.getType().getBaseType()
        );
    }
}
