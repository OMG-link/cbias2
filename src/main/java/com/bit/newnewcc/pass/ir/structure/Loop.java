package com.bit.newnewcc.pass.ir.structure;

import com.bit.newnewcc.ir.value.BasicBlock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    private final Set<Loop> subLoops;

    /**
     * 循环包含的<b>不属于subLoops中循环的</b>基本块
     */
    private final Set<BasicBlock> basicBlocks;

    /**
     * 循环嵌套的深度，最外层循环的深度为0
     */
    private int loopDepth;

    /**
     * 构建一个循环
     *
     * @param headerBasicBlock 循环的入口块
     * @param subLoops         循环的子循环
     * @param basicBlocks      循环包含的<b>不属于subLoops中循环的</b>基本块
     */
    public Loop(BasicBlock headerBasicBlock, Set<Loop> subLoops, Set<BasicBlock> basicBlocks) {
        this.headerBasicBlock = headerBasicBlock;
        this.subLoops = new HashSet<>(subLoops);
        this.basicBlocks = new HashSet<>(basicBlocks);
        this.loopDepth = -1;
    }

    /**
     * 设置父循环 <br>
     * 该函数仅限构建LoopTree时使用 <br>
     *
     * @param parentLoop 父循环
     */
    public void __setParentLoop__(Loop parentLoop) {
        this.parentLoop = parentLoop;
    }

    /**
     * 设置循环深度 <br>
     * 该函数仅限构建LoopTree时使用 <br>
     *
     * @param loopDepth 循环深度
     */
    public void __setLoopDepth__(int loopDepth) {
        this.loopDepth = loopDepth;
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
    public Set<Loop> getSubLoops() {
        return Collections.unmodifiableSet(subLoops);
    }

    /**
     * @return 循环深度
     */
    public int getLoopDepth() {
        return loopDepth;
    }

    /**
     * @return 循环内不在任何subLoops中的基本块列表（只读）
     */
    public Set<BasicBlock> getBasicBlocks() {
        return Collections.unmodifiableSet(basicBlocks);
    }

}
