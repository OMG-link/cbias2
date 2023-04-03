package com.bit.newnewcc.ir.value.instruction;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.type.DummyType;
import com.bit.newnewcc.ir.value.Instruction;

import java.util.ArrayList;
import java.util.List;

public class DummyInstruction extends Instruction {
    public DummyInstruction() {
        super(DummyType.getInstance());
    }

    @Override
    public List<Operand> getOperandList() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }
}
