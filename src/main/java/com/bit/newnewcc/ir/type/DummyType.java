package com.bit.newnewcc.ir.type;

import com.bit.newnewcc.ir.Type;

public class DummyType extends Type {

    private DummyType() {
    }

    @Override
    protected String getTypeName_() {
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
