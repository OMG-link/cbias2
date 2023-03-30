package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Operand;
import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.exception.UseRelationCheckFailException;

import java.util.List;

public abstract class Instruction extends Value {

    /**
     * @param type 语句返回值的类型
     */
    protected Instruction(Type type) {
        super(type);
        this.node = new BasicBlock.InstructionList.Node(this);
    }

    /// Operands

    /**
     * @return 当前语句用到的操作数的列表
     */
    public abstract List<Operand> getOperandList();

    /// BasicBlock & InstructionList

    private final BasicBlock.InstructionList.Node node;

    public BasicBlock getBasicBlock() {
        return node.getBasicBlock();
    }

    /**
     * 将当前节点插入到乙节点后方 <br>
     * 在插入前，需保证当前节点不属于任何链表 <br>
     * @param beta 乙节点
     */
    public void insertAfter(Instruction beta) {
        BasicBlock.InstructionList.insertAlphaAfterBeta(this.node, beta.node);
    }

    /**
     * 将当前节点插入到乙节点前方 <br>
     * 在插入前，需保证当前节点不属于任何链表 <br>
     * @param beta 乙节点
     */
    public void insertBefore(Instruction beta) {
        BasicBlock.InstructionList.insertAlphaBeforeBeta(this.node, beta.node);
    }

    /**
     * 获取当前指令所处的链表节点 <br>
     * <b style="color:red">【不要在InstructionList类以外的任何地方调用该函数！！！】</b> <br>
     *
     * @return 当前指令所处的链表节点
     */
    public BasicBlock.InstructionList.Node __getInstructionListNode__() {
        return node;
    }

    /// Compile process check

    public void checkOperandValidity() {
        for (Operand operand : getOperandList()) {
            if (!operand.hasValueBound()) {
                throw new UseRelationCheckFailException("No value was bound to this operand.");
            }
        }
    }


}
