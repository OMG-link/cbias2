package com.bit.newnewcc.ir.value;

import com.bit.newnewcc.ir.Type;
import com.bit.newnewcc.ir.Value;
import com.bit.newnewcc.ir.exception.IllegalArgumentException;
import com.bit.newnewcc.ir.type.FunctionType;
import com.bit.newnewcc.util.NameAllocator;

import java.util.*;

/**
 * 函数
 */
public class Function extends AbstractFunction {

    private class FormalParameter extends Value {
        public FormalParameter(Type type) {
            super(type);
        }

        private String valueName;

        @Override
        public String getValueName() {
            if(valueName==null){
                valueName = NameAllocator.getLvName(Function.this);
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
    }

    private final List<FormalParameter> formalParameters;
    private final Set<BasicBlock> basicBlocks;
    private final BasicBlock entryBasicBlock;

    public Function(FunctionType functionType) {
        super(functionType);
        this.formalParameters = new ArrayList<>();
        for (Type parameterType : functionType.getParameterTypes()) {
            this.formalParameters.add(new FormalParameter(parameterType));
        }
        this.basicBlocks = new HashSet<>();
        this.entryBasicBlock = new BasicBlock();
        this.addBasicBlock_(this.entryBasicBlock, true);
    }

    private String functionName;

    @Override
    public String getValueName() {
        if(functionName==null){
            functionName = NameAllocator.getGvName();
        }
        return functionName;
    }

    @Override
    public String getValueNameIR() {
        return '@'+functionName;
    }

    @Override
    public void setValueName(String valueName) {
        functionName = valueName;
    }

    /**
     * 将基本块添加到函数中
     *
     * @param basicBlock 待添加的基本块
     */
    public void addBasicBlock(BasicBlock basicBlock) {
        this.addBasicBlock_(basicBlock, false);
    }

    /**
     * 添加基本块 <br>
     * 锁定功能只允许Function类使用，用于锁定函数入口块 <br>
     *
     * @param basicBlock        待添加的基本块
     * @param shouldFixFunction 是否锁定该基本块
     */
    private void addBasicBlock_(BasicBlock basicBlock, boolean shouldFixFunction) {
        basicBlock.__setFunction__(this, shouldFixFunction);
        this.basicBlocks.add(basicBlock);
    }


    /**
     * 将基本块从函数中移除
     *
     * @param basicBlock 待移除的基本块
     */
    public void removeBasicBlock(BasicBlock basicBlock) {
        if (basicBlock.getFunction() != this) {
            throw new IllegalArgumentException("Specified basic block does not belong to this function.");
        }
        basicBlock.__clearFunction__();
        this.basicBlocks.remove(basicBlock);
    }

    public BasicBlock getEntryBasicBlock() {
        return entryBasicBlock;
    }

    public Collection<BasicBlock> getBasicBlocks() {
        return Collections.unmodifiableCollection(basicBlocks);
    }

    /**
     * @return 形参列表（只读）
     */
    public List<Value> getFormalParameters() {
        return Collections.unmodifiableList(formalParameters);
    }

}