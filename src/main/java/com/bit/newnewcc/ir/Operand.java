package com.bit.newnewcc.ir;

import com.bit.newnewcc.ir.exception.OperandTypeMismatchException;
import com.bit.newnewcc.ir.value.Instruction;

/**
 * 指令的操作数
 */
public class Operand {
    /**
     * 使用该操作数的指令
     */
    private final Instruction instruction;
    /**
     * 操作数类型的限制 <br>
     * 值为null时表示不限制类型
     */
    private final Type typeLimit;
    /**
     * 操作数所使用的值
     */
    private Value value;

    /**
     * 构建一个操作数
     * @param instruction 操作数所属的指令
     * @param typeLimit 操作数的类型限制，null表示无限制
     * @param value 操作数的初始值，null表示不绑定初始值
     */
    public Operand(Instruction instruction, Type typeLimit, Value value){
        this.instruction = instruction;
        this.typeLimit = typeLimit;
        this.value = null;
        this.setValue(value);
    }

    /**
     * @return 操作数所属的指令
     */
    public Instruction getInstruction() {
        return instruction;
    }

    /**
     * 判断当前操作数是否绑定了值
     * @return 若绑定了值，返回True；否则返回False
     */
    public boolean hasValueBound() {
        return value!=null;
    }

    /**
     * @return 操作数的值
     */
    public Value getValue() {
        return value;
    }

    /**
     * 修改操作数的值
     * @param value 操作数的新值
     */
    public void setValue(Value value) {
        if(value!=null){
            if(typeLimit!=null&&value.getType()!=typeLimit){
                throw new OperandTypeMismatchException(instruction,typeLimit,value.getType());
            }
        }
        if(this.value!=null){
            this.value.__removeUsedByRecord__(this);
        }
        this.value = value;
        if(this.value!=null){
            this.value.__addUsedByRecord__(this);
        }
    }

    /**
     * 移除当前操作数绑定的值
     */
    public void removeValue(){
        this.setValue(null);
    }

}
