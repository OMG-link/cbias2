package com.bit.newnewcc.ir.type;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.value.Constant;

public class DummyType extends Type {

    private DummyType() {
    }

    @Override
    protected String getTypeName_() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Constant getDefaultInitialization() {
        throw new UnsupportedOperationException();
    }

    private static DummyType instance;

    public static DummyType getInstance() {
        if(instance==null){
            instance = new DummyType();
        }
        return instance;
    }
}
