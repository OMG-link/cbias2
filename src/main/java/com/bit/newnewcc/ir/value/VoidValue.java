package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.VoidType;

/**
 * void值 <br>
 * ReturnInst在返回void时会用到该值 <br>
 */
public class VoidValue extends Value {
    private VoidValue() {
        super(VoidType.getInstance());
        setValueName("");
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

    private static VoidValue voidValue = null;

    public static VoidValue getInstance() {
        if(voidValue==null){
            voidValue = new VoidValue();
        }
        return voidValue;
    }

}
