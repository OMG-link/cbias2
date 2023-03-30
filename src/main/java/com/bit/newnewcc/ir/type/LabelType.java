package com.bit.newnewcc.ir.type;

import com.bit.newnewcc.ir.Type;

/**
 * Label类型 <br>
 * 该类型是单例的 <br>
 */
public class LabelType extends Type {
    private LabelType() {
    }

    @Override
    protected String getTypeName_() {
        return "label";
    }

    private static LabelType instance = null;

    /**
     * 获取Label类型的实例
     * @return Label类型的唯一实例
     */
    public static LabelType getInstance() {
        if (instance == null) {
            instance = new LabelType();
        }
        return instance;
    }
}
