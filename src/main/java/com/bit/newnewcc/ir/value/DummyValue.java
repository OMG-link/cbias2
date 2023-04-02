package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.DummyType;

public class DummyValue extends Value {
    public DummyValue() {
        super(DummyType.getInstance());
    }

    @Override
    public String getValueName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getValueNameIR() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValueName(String valueName) {
        throw new UnsupportedOperationException();
    }
}
