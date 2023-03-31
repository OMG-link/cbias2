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



}
