package cn.edu.bit.newnewcc.ir.value.constant;

import cn.edu.bit.newnewcc.ir.type.VoidType;
import cn.edu.bit.newnewcc.ir.value.Constant;

/**
 * void值 <br>
 * ReturnInst在返回void时会用到该值 <br>
 */
public class VoidValue extends Constant {
    private VoidValue() {
        super(VoidType.getInstance());
    }

    @Override
    public String getValueName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFilledWithZero() {
        return true;
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