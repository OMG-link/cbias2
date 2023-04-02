package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;

public abstract class Constant extends Value {

    /**
     *
     * @param type 常量的类型
     */
    protected Constant(Type type) {
        super(type);
    }

    @Override
    public String getValueNameIR() {
        return getValueName();
    }

    @Override
    public void setValueName(String valueName) {
        // 常量拥有固定的名字，不能被外界设置
        throw new UnsupportedOperationException();
    }
}
