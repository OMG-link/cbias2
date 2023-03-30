package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.VoidType;

// Emm，我也不知道为什么有这个类，似乎也没有地方用得到它
public class VoidValue extends Value {
    private VoidValue() {
        super(VoidType.getInstance());
        setValueName("");
    }

    private static VoidValue voidValue = null;

    public static VoidValue getInstance() {
        if(voidValue==null){
            voidValue = new VoidValue();
        }
        return voidValue;
    }

}
