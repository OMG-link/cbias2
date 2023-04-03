package com.bit.newnewcc.ir.type;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.exception.IllegalBitWidthException;
import com.bit.newnewcc.ir.value.Constant;
import com.bit.newnewcc.ir.value.constant.ConstInt;

import java.util.HashMap;
import java.util.Map;

/**
 * 整数类型
 */
public class IntegerType extends Type {
    private final int bitWidth;

    private IntegerType(int bitWidth) {
        this.bitWidth = bitWidth;
    }

    public int getBitWidth() {
        return bitWidth;
    }

    @Override
    protected String getTypeName_() {
        return String.format("i%d",bitWidth);
    }

    @Override
    public Constant getDefaultInitialization() {
        return switch (bitWidth){
            case 32 -> ConstInt.getInstance(0);
            default -> throw new UnsupportedOperationException();
        };
    }

    private static boolean isBitWidthLegal(int bitWidth){
        return switch (bitWidth){
            case 1,32 -> true;
            default -> false;
        };
    }

    private static Map<Integer, IntegerType> instanceMap = null;

    private static IntegerType getInstance(int bitWidth) {
        if(instanceMap == null){
            instanceMap = new HashMap<>();
        }
        if (!instanceMap.containsKey(bitWidth)) {
            if(!isBitWidthLegal(bitWidth)){
                throw new IllegalBitWidthException();
            }
            instanceMap.put(bitWidth, new IntegerType(bitWidth));
        }
        return instanceMap.get(bitWidth);
    }

    public static IntegerType getI1(){
        return getInstance(1);
    }

    public static IntegerType getI32(){
        return getInstance(32);
    }

}