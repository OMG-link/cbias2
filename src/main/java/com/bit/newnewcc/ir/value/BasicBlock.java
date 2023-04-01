package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.exception.IllegalArgumentException;
import com.bit.newnewcc.ir.exception.UsageRelationshipCheckFailedException;
import com.bit.newnewcc.ir.type.LabelType;
import com.bit.newnewcc.ir.value.instruction.DummyInstruction;

import java.util.Iterator;

/**
 * 基本块
 */
public class BasicBlock extends Value {

    public BasicBlock() {
        super(LabelType.getInstance());
    }

    @Override
    public LabelType getType() {
        return (LabelType) super.getType();
    }

    /// 基本块与指令的关系
    public class InstructionList implements Iterable<Instruction> {

        public static class Node {
            private Node prev, next;
            private final Instruction instruction;

            private InstructionList list;

            public Node(Instruction instruction) {
                this.instruction = instruction;
            }

            public BasicBlock getBasicBlock() {
                return instruction.getBasicBlock();
            }

        }

        private class InstructionIterator implements Iterator<Instruction>{

            private Node node;

            private InstructionIterator(Node node){
                this.node = node;
            }

            @Override
            public boolean hasNext() {
                return node.next!=tail;
            }

            @Override
            public Instruction next() {
                node = node.next;
                return node.instruction;
            }
        }

        @Override
        public Iterator<Instruction> iterator() {
            return new InstructionIterator(head);
        }

        /**
         * 链表的头/尾哨兵节点
         */
        private final Node head, tail;

        public InstructionList() {
            var headInst = new DummyInstruction();
            var tailInst = new DummyInstruction();
            this.head = headInst.__getInstructionListNode__();
            this.tail = tailInst.__getInstructionListNode__();
            this.head.next = this.tail;
            this.tail.prev = this.head;
        }

        public BasicBlock getBasicBlock() {
            return BasicBlock.this;
        }

        /**
         * 将节点甲插入到节点乙之前
         *
         * @param alpha 节点甲
         * @param beta  节点乙
         */
        // 关于alpha和beta：
        // 本来想叫A和B的
        // 但是由于A和B在小驼峰中不美观，就改成了alpha和beta
        public static void insertAlphaBeforeBeta(Node alpha, Node beta) {
            assertNodeFree(alpha);
            alpha.prev = beta.prev;
            if(beta.prev!=null) // 事实上为null的情况不会发生，因为设置了哨兵节点
                beta.prev.next = alpha;
            alpha.next = beta;
            beta.prev = alpha;
            alpha.list = beta.list;
        }

        /**
         * 将节点甲插入到节点乙之后
         *
         * @param alpha 节点甲
         * @param beta  节点乙
         */
        public static void insertAlphaAfterBeta(Node alpha, Node beta) {
            assertNodeFree(alpha);
            alpha.next = beta.next;
            if(beta.next!=null)
                beta.next.prev = alpha;
            alpha.prev = beta;
            beta.next = alpha;
            alpha.list = beta.list;
        }

        private static void assertNodeFree(Node node) {
            if (!(node.prev == null && node.next == null && node.list == null)) {
                throw new UsageRelationshipCheckFailedException("This node has been inserted to some list, you must remove it before inserting to a new one");
            }
        }

    }

    private final InstructionList instructionList = new InstructionList();

    public InstructionList getInstructionList() {
        return instructionList;
    }

    /**
     * 在基本块的开头插入一条指令
     * @param instruction 待插入的指令
     */
    public void insertInstructionAtStart(Instruction instruction){
        instruction.insertAfter(instructionList.head.instruction);
    }

    /**
     * 在基本块的结尾插入一条指令
     * @param instruction 待插入的指令
     */
    public void insertInstructionAtEnd(Instruction instruction){
        instruction.insertBefore(instructionList.tail.instruction);
    }

    /**
     * 在基本块的结尾插入一条指令
     * @param instruction 待插入的指令
     */
    // 为了使得使用处代码美观，提供了该函数的两种名称
    public void appendInstruction(Instruction instruction){
        insertInstructionAtEnd(instruction);
    }

    /// 基本块与函数的关系

    /**
     * 该基本块所在的函数
     */
    private Function function = null;

    private boolean isFunctionFixed = false;

    public Function getFunction() {
        return function;
    }

    /**
     * 设置该基本块所在的函数 <br>
     * <b style="color:red">【不要在Function类以外的任何地方调用该函数！！！】</b> <br>
     *
     * @param function 基本块所在的函数
     */
    public void __setFunction__(Function function, boolean shouldFixFunction) {
        // 检查所属函数是否被锁定，锁定则不能被修改
        if (this.isFunctionFixed) {
            throw new UsageRelationshipCheckFailedException("Belonging of this basic block has been fixed.");
        }
        // 检查是否直接修改所属函数，必须先从其他函数中移除该基本块
        if (this.function != null && function != null) {
            throw new UsageRelationshipCheckFailedException("A basic block is being added to another function without removing from the original function");
        }
        // 设置函数锁定
        if (shouldFixFunction) {
            // 不可以锁定到null上
            if (function == null) {
                throw new IllegalArgumentException("Cannot fix null as the belonging of this basic block.");
            }
            this.isFunctionFixed = true;
        }
        this.function = function;
    }

    /**
     * 设置该基本块不在任何函数内 <br>
     * <b style="color:red">【不要在Function类以外的任何地方调用该函数！！！】</b> <br>
     */
    public void __clearFunction__() {
        __setFunction__(null, false);
    }
}
