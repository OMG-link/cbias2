package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.util.InstructionList;
import com.bit.newnewcc.ir.util.NameAllocator;

import java.util.List;

public abstract class Instruction extends Value {

    /**
     * @param type 语句返回值的类型
     */
    protected Instruction(Type type) {
        super(type);
        this.node = new InstructionList.Node(this);
    }

    /// Name

    private String valueName;

    @Override
    public String getValueName() {
        if(valueName==null){
            var bb = getBasicBlock();
            if(bb==null){
                throw new UnsupportedOperationException("Cannot get the name of an instruction outside a basic block.");
            }
            var fun = bb.getFunction();
            if(fun==null){
                throw new UnsupportedOperationException("Cannot get the name of an instruction outside a function.");
            }
            valueName = NameAllocator.getLvName(fun);
        }
        return valueName;
    }

    @Override
    public String getValueNameIR() {
        return '%'+getValueName();
    }

    @Override
    public void setValueName(String valueName) {
        this.valueName = valueName;
    }

    @Override
    public abstract String toString();


    /// Operands

    /**
     * @return 当前语句用到的操作数的列表
     */
    public abstract List<Operand> getOperandList();

    /// BasicBlock & InstructionList

    private final InstructionList.Node node;

    public BasicBlock getBasicBlock() {
        return node.getBasicBlock();
    }

    /**
     * 将当前节点插入到乙节点后方 <br>
     * 在插入前，需保证当前节点不属于任何链表 <br>
     * @param beta 乙节点
     */
    public void insertAfter(Instruction beta) {
        InstructionList.insertAlphaAfterBeta(this.node, beta.node);
    }

    /**
     * 将当前节点插入到乙节点前方 <br>
     * 在插入前，需保证当前节点不属于任何链表 <br>
     *
     * @param beta 乙节点
     */
    public void insertBefore(Instruction beta) {
        InstructionList.insertAlphaBeforeBeta(this.node, beta.node);
    }

    /**
     * 将该语句从当前基本块中移除
     */
    public void removeFromBasicBlock() {
        InstructionList.removeNodeFromList(node);
    }

    /**
     * 获取当前指令所处的链表节点 <br>
     * <b style="color:red">【不要在InstructionList类以外的任何地方调用该函数！！！】</b> <br>
     *
     * @return 当前指令所处的链表节点
     */
    public InstructionList.Node __getInstructionListNode__() {
        return node;
    }


}
