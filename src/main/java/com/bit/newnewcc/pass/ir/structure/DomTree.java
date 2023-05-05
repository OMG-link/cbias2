package com.bit.newnewcc.pass.ir.structure;

import com.bit.newnewcc.ir.exception.IllegalArgumentException;
import com.bit.newnewcc.ir.value.BasicBlock;
import com.bit.newnewcc.ir.value.Function;
import com.bit.newnewcc.util.DomTreeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支配树
 */
public class DomTree {

    private static class Node {

        /**
         * 节点对应的基本块
         */
        private final BasicBlock basicBlock;

        /**
         * 父节点列表 <br>
         * 下标为i的元素代表当前节点的第2^i级父亲 <br>
         */
        // bexp = binary exponentiation
        private final List<Node> bexpParents = new ArrayList<>();

        private int depth;

        public Node(BasicBlock basicBlock) {
            this.basicBlock = basicBlock;
        }

    }

    private final Map<BasicBlock, Node> nodeMap = new HashMap<>();

    private DomTree(Function function) {
        var builder = new DomTreeBuilder<BasicBlock>();
        builder.setRoot(function.getEntryBasicBlock());
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            for (BasicBlock exitBlock : basicBlock.getExitBlocks()) {
                builder.addEdge(basicBlock, exitBlock);
            }
        }
        builder.build();
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            var node = new Node(basicBlock);
            nodeMap.put(basicBlock, node);
        }
        nodeMap.get(function.getEntryBasicBlock()).depth = 0;
        buildBexpArray(builder, nodeMap.get(function.getEntryBasicBlock()));
    }

    private void buildBexpArray(DomTreeBuilder<BasicBlock> builder, Node u) {
        for (int i = 0; i < u.bexpParents.size(); i++) {
            var f = u.bexpParents.get(i);
            if (i < f.bexpParents.size()) {
                u.bexpParents.add(f.bexpParents.get(i));
            }
        }
        for (BasicBlock domSon : builder.getDomSons(u.basicBlock)) {
            var sonNode = nodeMap.get(domSon);
            sonNode.depth = u.depth + 1;
            sonNode.bexpParents.add(u);
            buildBexpArray(builder, sonNode);
        }
    }

    /**
     * 检查A在支配树上是否为B的祖先
     *
     * @param alpha A基本块
     * @param beta  B基本块
     * @return 若A在支配树上是B的祖先，返回true；否则返回false。
     */
    public boolean doesAlphaDominatesBeta(BasicBlock alpha, BasicBlock beta) {
        var a = nodeMap.get(alpha);
        var b = nodeMap.get(beta);
        if (a == null || b == null) {
            throw new IllegalArgumentException();
        }
        if (a.depth > b.depth) return false;
        int delta = b.depth - a.depth;
        for (int i = 0; delta != 0; i++) {
            if ((delta & (1 << i)) != 0) {
                a = a.bexpParents.get(i);
                delta ^= (1 << i);
            }
        }
        return a == b;
    }

    /**
     * 由函数构造一棵支配树
     *
     * @param function 函数
     * @return 支配树
     */
    public static DomTree fromFunction(Function function) {
        return new DomTree(function);
    }
}
