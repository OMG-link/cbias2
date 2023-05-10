package cn.edu.bit.newnewcc.ir.value.instruction;

import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.exception.IllegalArgumentException;
import cn.edu.bit.newnewcc.ir.exception.IndexOutOfBoundsException;
import cn.edu.bit.newnewcc.ir.type.FunctionType;
import cn.edu.bit.newnewcc.ir.value.AbstractFunction;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数调用指令
 */
public class CallInst extends Instruction {
    /**
     * 被调用函数操作数
     */
    private final Operand calleeOperand;

    /**
     * 实参操作数列表
     */
    private final List<Operand> argumentOperands;

    /**
     * 根据函数类型创建一条call指令，不填入被调用函数和实参
     *
     * @param functionType 函数类型
     */
    public CallInst(FunctionType functionType) {
        super(functionType);
        this.calleeOperand = new Operand(this, functionType, null);
        this.argumentOperands = new ArrayList<>();
        for (var type : functionType.getParameterTypes()) {
            argumentOperands.add(new Operand(this, type, null));
        }
    }

    /**
     * 创建一条call指令，并填入被调用函数和实参
     *
     * @param function  被调用函数
     * @param arguments 实参列表
     */
    public CallInst(AbstractFunction function, List<Value> arguments) {
        super(function.getType());
        this.calleeOperand = new Operand(this, function.getType(), function);
        if (arguments.size() != function.getParameterTypes().size()) {
            throw new IllegalArgumentException("Size of provided argument does not match the one required by function.");
        }
        this.argumentOperands = new ArrayList<>();
        var iArguments = arguments.iterator();
        var iTypes = function.getParameterTypes().iterator();
        while (iArguments.hasNext() && iTypes.hasNext()) {
            var argument = iArguments.next();
            var type = iTypes.next();
            this.argumentOperands.add(new Operand(this, type, argument));
        }
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(calleeOperand);
        list.addAll(argumentOperands);
        return list;
    }

    /**
     * 获取被调用的函数
     *
     * @return 被调用的函数
     */
    public AbstractFunction getCallee() {
        return (AbstractFunction) calleeOperand.getValue();
    }

    /**
     * 设置被调用的函数
     *
     * @param function 被调用的函数
     */
    public void setCallee(AbstractFunction function) {
        calleeOperand.setValue(function);
    }

    /**
     * 获取call指令中填入的实参
     *
     * @param index 实参在参数列表中的下标(0-index)
     * @return 第index个实参
     */
    public Value getArgumentAt(int index) {
        if (index < 0 || index >= argumentOperands.size()) {
            throw new IndexOutOfBoundsException(index, 0, argumentOperands.size());
        }
        return argumentOperands.get(index).getValue();
    }

    /**
     * 设置call指令的实参
     *
     * @param index 要设置的实参在参数列表中的下标(0-index)
     * @param value 要设置的值
     */
    public void setArgumentAt(int index, Value value) {
        if (index < 0 || index >= argumentOperands.size()) {
            throw new IndexOutOfBoundsException(index, 0, argumentOperands.size());
        }
        argumentOperands.get(index).setValue(value);
    }

    @Override
    public String toString() {
        // e.g. %1 = call double @sum(i32 1, float 2.000000e+00)
        var builder = new StringBuilder();
        builder.append(String.format(
                "%s = call %s %s",
                this.getValueNameIR(),
                this.getTypeName(),
                getCallee().getValueNameIR()
        ));
        builder.append('(');
        for(var i=0;i<argumentOperands.size();i++){
            if(i!=0){
                builder.append(", ");
            }
            var argument = argumentOperands.get(i).getValue();
            builder.append(String.format("%s %s",argument.getTypeName(),argument.getValueNameIR()));
        }
        builder.append(')');
        return builder.toString();
    }
}