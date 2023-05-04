package com.bit.newnewcc.ir.structure;

import com.bit.newnewcc.ir.value.BasicBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 循环结构 <br>
 * 存储了父循环、子循环、以及循环包含的不在子循环内的基本块 <br>
 */
public class Loop {

    /**
     * 循环的入口块
     */
    private final BasicBlock headerBasicBlock;

    /**
     * 父循环
     */
    private Loop parentLoop;

    /**
     * 子循环列表
     */
    private final List<Loop> subLoops;

    /**
     * 循环包含的<b>不属于subLoops中循环的</b>基本块
     */
    private final List<BasicBlock> basicBlocks;

    /**
     * 构建一个循环
     *
     * @param headerBasicBlock 循环的入口块
     * @param subLoops         循环的子循环
     * @param basicBlocks      循环包含的<b>不属于subLoops中循环的</b>基本块
     */
    public Loop(BasicBlock headerBasicBlock, List<Loop> subLoops, List<BasicBlock> basicBlocks) {
        this.headerBasicBlock = headerBasicBlock;
        this.subLoops = new ArrayList<>(subLoops);
        this.basicBlocks = new ArrayList<>(basicBlocks);
    }

    /**
     * @return 循环的入口块
     */
    public BasicBlock getHeaderBasicBlock() {
        return headerBasicBlock;
    }

    /**
     * @return 循环的父循环
     */
    public Loop getParentLoop() {
        return parentLoop;
    }

    /**
     * @return 循环的子循环列表（只读）
     */
    public List<Loop> getSubLoops() {
        return Collections.unmodifiableList(subLoops);
    }

    /**
     * @return 循环内不在任何subLoops中的基本块列表（只读）
     */
    public List<BasicBlock> getBasicBlocks() {
        return Collections.unmodifiableList(basicBlocks);
    }

}
