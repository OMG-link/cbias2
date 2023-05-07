package cn.edu.bit.newnewcc.ir.type;

import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.value.Constant;

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

    @Override
    public long getSize() {
        return 0;
    }

    private static DummyType instance;

    public static DummyType getInstance() {
        if (instance == null) {
            instance = new DummyType();
        }
        return instance;
    }
}
