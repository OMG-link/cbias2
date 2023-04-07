package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.exception.IllegalArgumentException;
import com.bit.newnewcc.ir.exception.UsageRelationshipCheckFailedException;
import com.bit.newnewcc.ir.type.LabelType;
import com.bit.newnewcc.ir.util.InstructionList;
import com.bit.newnewcc.ir.util.NameAllocator;
import com.bit.newnewcc.ir.value.instruction.AllocateInst;
import com.bit.newnewcc.ir.value.instruction.PhiInst;
import com.bit.newnewcc.ir.value.instruction.TerminateInst;

import java.util.*;

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

    /// 名称

    private String valueName;

    @Override
    public String getValueName() {
        if (valueName == null) {
            if (function == null) {
                throw new UnsupportedOperationException("Cannot get the name of a basic block outside a function.");
            }
            valueName = NameAllocator.getLvName(function);
        }
        return valueName;
    }

    @Override
    public String getValueNameIR() {
        return '%' + getValueName();
    }

    @Override
    public void setValueName(String valueName) {
        this.valueName = valueName;
    }

    private final InstructionList instructionList = new InstructionList(this);

    /**
     * 向基本块中加入一条指令 <br>
     * 对于一般的指令，其会被添加到主指令体的末尾 <br>
     * 对于前导指令，其会被添加到基本块开头 <br>
     * 对于结束指令，其会替换现有的结束指令 <br>
     *
     * @param instruction 待插入的指令
     */
    public void addInstruction(Instruction instruction) {
        if (instruction instanceof AllocateInst || instruction instanceof PhiInst) {
            instructionList.addLeadingInst(instruction);
            return;
        }
        if (instruction instanceof TerminateInst terminateInst) {
            setTerminateInstruction(terminateInst);
            return;
        }
        instructionList.appendMainInstruction(instruction);
    }

    public void setTerminateInstruction(TerminateInst terminateInstruction) {
        if (getTerminateInstruction() != null) {
            getTerminateInstruction().getExits().forEach(exit -> exit.removeEntryBlock(this));
        }
        instructionList.setTerminateInstruction(terminateInstruction);
        getTerminateInstruction().getExits().forEach(exit -> exit.addEntryBlock(this));
    }

    /**
     * 获取所有前导指令
     *
     * @return 前导指令集合的迭代器
     */
    public Iterator<Instruction> getLeadingInstructions() {
        return instructionList.getLeadingInstructions();
    }

    /**
     * 获取所有主体指令
     * @return 主体指令集合的迭代器
     */
    public Iterator<Instruction> getMainInstructions() {
        return instructionList.getMainInstructions();
    }

    /**
     * 获取结束指令 <br>
     * 没有设置结束指令时，返回 null <br>
     * @return 结束指令或 null
     */
    public TerminateInst getTerminateInstruction() {
        return instructionList.getTerminateInstruction();
    }

    /**
     * 获取所有指令
     *
     * @return 所有指令的迭代器
     */
    public Iterator<Instruction> getInstructions() {
        return instructionList.iterator();
    }

    public Collection<BasicBlock> getExitBlocks() {
        return getTerminateInstruction().getExits();
    }

    /// 基本块入口维护

    private final Set<BasicBlock> entryBlockSet = new HashSet<>();

    private void addEntryBlock(BasicBlock basicBlock) {
        entryBlockSet.add(basicBlock);
    }

    private void removeEntryBlock(BasicBlock basicBlock) {
        if (!entryBlockSet.contains(basicBlock)) {
            throw new IllegalArgumentException("Specified basic block is not an entry of this block.");
        }
        entryBlockSet.remove(basicBlock);
    }

    /**
     * 获取该基本块的所有入口块
     *
     * @return 基本块的所有入口块的只读副本
     */
    public Collection<BasicBlock> getEntryBlocks() {
        return Collections.unmodifiableCollection(entryBlockSet);
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
