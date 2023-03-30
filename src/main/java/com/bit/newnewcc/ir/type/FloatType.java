package com.bit.newnewcc.ir.type;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.exception.IllegalBitWidthException;

import java.util.HashMap;
import java.util.Map;

/**
 * 浮点类型 <br>
 * 在SysY中目前只有float类型，但是我们还是预留了double,float128等类型的接口 <br>
 */
public class FloatType extends Type {
    private final int bitWidth;

    private FloatType(int bitWidth) {
        this.bitWidth = bitWidth;
    }

    @Override
    protected String getTypeName_() {
        return switch (bitWidth){
            case 32 -> "float";
            default -> throw new IllegalStateException("Invalid bit width of floating number: " + bitWidth);
        };
    }

    private static boolean isBitWidthLegal(int bitWidth){
        return switch (bitWidth){
            case 32 -> true;
            default -> false;
        };
    }

    private static Map<Integer,FloatType> instanceMap = null;

    private static FloatType getInstance(int bitWidth) {
        if(instanceMap==null){
            instanceMap = new HashMap<>();
        }
        if(!instanceMap.containsKey(bitWidth)){
            if(!isBitWidthLegal(bitWidth)){
                throw new IllegalBitWidthException();
            }
            instanceMap.put(bitWidth, new FloatType(bitWidth));
        }
        return instanceMap.get(bitWidth);
    }

    public static FloatType getFloat(){
        return getInstance(32);
    }

}
