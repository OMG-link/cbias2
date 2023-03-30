package com.bit.newnewcc.ir.type;

import com.bit.newnewcc.ir.Type;

/**
 * Void类型 <br>
 * 该类型是单例的 <br>
 */
public class VoidType extends Type {
    private VoidType() {
    }

    @Override
    protected String getTypeName_() {
        return "void";
    }

    private static VoidType instance = null;

    /**
     * 获取Void类型的实例
     * @return Void类型的唯一实例
     */
    public static VoidType getInstance() {
        if (instance == null) {
            instance = new VoidType();
        }
        return instance;
    }
}
