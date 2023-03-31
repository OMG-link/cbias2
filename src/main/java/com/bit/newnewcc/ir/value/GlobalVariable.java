package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.type.PointerType;

/**
 * 全局变量 <br>
 * 此类型本质上是指向全局变量的指针 <br>
 */
public class GlobalVariable extends Value {

    private final boolean isConstant;
    private final Constant initialValue;

    /**
     * 未指定初始值的全局变量
     * @param type 全局变量的类型
     */
    public GlobalVariable(Type type){
        this(false,type.getDefaultInitialization());
    }

    /**
     * 指定初始值的全局变量
     * @param isConstant 该全局变量是否被定义为常量
     * @param initialValue 全局变量的初始值
     */
    public GlobalVariable(boolean isConstant, Constant initialValue) {
        super(PointerType.getInstance(initialValue.getType()));
        this.isConstant = isConstant;
        this.initialValue = initialValue;
    }

    /**
     * 判断该全局变量是否被<b>定义</b>为常量
     * @return 若该全局变量被<b>定义</b>为常量，则返回true；否则返回false
     */
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * 获取全局变量的初始值
     * @return 全局变量的初始值
     */
    public Constant getInitialValue() {
        return initialValue;
    }

    /**
     * 获取全局变量的值的类型 <br>
     * 此类型与全局变量的类型不同，全局变量的类型是该类型的指针 <br>
     * @return 全局变量的值的类型
     */
    public Type getStoredValueType() {
        return getType().getPointedType();
    }

    /**
     * 注意：全局变量的类型是其存储的类型的<b style="color:red">指针</b> <br>
     * 要获取其存储的值的类型，请使用 globalVariable.getType().getPointedType() 或 globalVariable.getStoredValueType()
     * @return 全局变量存储的类型的<b>指针</b>
     */
    @Override
    public PointerType getType() {
        return (PointerType) super.getType();
    }

}
