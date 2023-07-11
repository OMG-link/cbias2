package cn.edu.bit.newnewcc.pass.ir.structure;

import cn.edu.bit.newnewcc.ir.exception.IllegalArgumentException;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.util.DomTreeBuilder;

import java.util.*;

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
         * 父节点列表
         * <p>
         * 下标为i的元素代表当前节点的第2^i级父亲
         */
        // bexp = binary exponentiation
        private final List<BasicBlock> bexpParents = new ArrayList<>();

        private final List<BasicBlock> domSons = new ArrayList<>();

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
            node.domSons.addAll(builder.getDomSons(basicBlock));
        }
        nodeMap.get(function.getEntryBasicBlock()).depth = 0;
        buildBexpArray(nodeMap.get(function.getEntryBasicBlock()));
    }

    private void buildBexpArray(Node u) {
        for (int i = 0; i < u.bexpParents.size(); i++) {
            var f = nodeMap.get(u.bexpParents.get(i));
            if (i < f.bexpParents.size()) {
                u.bexpParents.add(f.bexpParents.get(i));
            }
        }
        for (BasicBlock domSon : u.domSons) {
            var sonNode = nodeMap.get(domSon);
            sonNode.depth = u.depth + 1;
            sonNode.bexpParents.add(u.basicBlock);
            buildBexpArray(sonNode);
        }
    }

    /**
     * 获取基本块在支配树上的所有直接孩子
     *
     * @param basicBlock 基本块
     * @return 基本块在支配树上的所有直接孩子列表（只读）
     */
    public Collection<BasicBlock> getDomSons(BasicBlock basicBlock) {
        var node = nodeMap.get(basicBlock);
        if (node == null) {
            throw new IllegalArgumentException("Basic block not in this dom tree");
        }
        return Collections.unmodifiableList(node.domSons);
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
                b = nodeMap.get(b.bexpParents.get(i));
                delta ^= (1 << i);
            }
        }
        return a == b;
    }

    /**
     * 获取某个基本块在支配树中的深度
     *
     * @param basicBlock 基本块
     * @return 基本块在支配树中的深度
     */
    public int getDomDepth(BasicBlock basicBlock) {
        return nodeMap.get(basicBlock).depth;
    }

    public static DomTree buildOver(Function function) {
        return new DomTree(function);
    }

}
