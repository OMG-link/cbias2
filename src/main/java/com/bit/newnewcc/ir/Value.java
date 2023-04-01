package com.bit.newnewcc.ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;

/**
 * Value是一种具有类型、名字，并且可以被作为Operand使用的东西。 <br>
 * 例如： <br>
 * <ul>
 *     <li>BasicBlock是一个Value，因为其可以作为跳转指令的操作数</li>
 *     <li>Function是一个Value，因为其可以作为Call指令的操作数</li>
 *     <li>Instruction是一个Value，因为表达式通常都有一个结果值，并可以被用作操作数</li>
 *     <li>Constant是一个Value，原因显然</li>
 * </ul>
 */
// 理论上来说这应该是一个interface，因为它的子类和它并没有太强的联系
// 但这个类型需要存储一些必要的信息，而Java并不支持多继承，因而采取了这种写法
public abstract class Value {
    private final Type type;

    protected Value(Type type) {
        this.type = type;
    }

    /**
     * @return 当前value的类型
     */
    public Type getType() {
        return type;
    }

    public String getTypeName() {
        return type.getTypeName_();
    }

    private String valueName = null;

    public String getValueName() {
        return valueName;
    }

    public void setValueName(String valueName) {
        this.valueName = valueName;
    }

    /// 使用-被使用关系

    /**
     * 记录当前Value被哪些Operand所使用
     */
    private final HashSet<Operand> usages = new HashSet<>();

    /**
     * 添加一个当前Value被作为操作数的记录 <br>
     * <b style="color:red">【不要在Operand类以外的任何地方调用该函数！！！】</b> <br>
     * @param operand 操作数
     */
    public void __addUsage__(Operand operand){
        usages.add(operand);
    }

    /**
     * 移除一个当前Value被作为操作数的记录 <br>
     * <b style="color:red">【不要在Operand类以外的任何地方调用该函数！！！】</b> <br>
     * @param operand 操作数
     */
    public void __removeUsage__(Operand operand){
        boolean success = usages.remove(operand);
        if(!success){
            throw new NoSuchElementException("Trying to remove a usage that does not exist");
        }
    }

    /**
     * @return 使用当前Value的Operand的列表的副本
     */
    public ArrayList<Operand> getUsages() {
        return new ArrayList<>(usages);
    }

}
