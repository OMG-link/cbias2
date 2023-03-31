package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.FunctionType;

import java.util.List;

public class ExternalFunction extends Value {
    public ExternalFunction(FunctionType type) {
        super(type);
    }

    /**
     * @return 返回值类型
     */
    public Type getReturnType() {
        return getType().getReturnType();
    }

    /**
     * @return 形参类型列表（只读）
     */
    public List<Type> getParameterTypes() {
        // FunctionType.getParameterTypes()中已经包装了unmodifiableList，故此处无需再次包装
        return getType().getParameterTypes();
    }

    @Override
    public FunctionType getType() {
        return (FunctionType) super.getType();
    }

}
