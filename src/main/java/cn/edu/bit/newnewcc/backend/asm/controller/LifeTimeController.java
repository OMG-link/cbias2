package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmJump;
import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.util.AsmInstructions;
import cn.edu.bit.newnewcc.backend.asm.util.ComparablePair;

import java.util.*;

/**
 * 生命周期控制器，
 */
public class LifeTimeController {
    //虚拟寄存器生命周期的设置过程
    private final Map<Integer, ComparablePair<LifeTimeIndex, LifeTimeIndex>> lifeTimeRange = new HashMap<>();
    private final Map<Integer, List<LifeTimeInterval>> lifeTimeInterval = new HashMap<>();
    private final Map<Integer, List<LifeTimePoint>> lifeTimePoints = new HashMap<>();
    private final Map<AsmInstruction, Integer> instIDMap = new HashMap<>();

    private void init() {
        lifeTimeRange.clear();
        lifeTimeInterval.clear();
        lifeTimePoints.clear();
        blocks.clear();
        blockMap.clear();
        instIDMap.clear();
    }

    public void buildInstID(List<AsmInstruction> instructionList) {
        instIDMap.clear();
        for (int i = 0; i < instructionList.size(); i++) {
            instIDMap.put(instructionList.get(i), i);
        }
    }

    public int getInstID(AsmInstruction inst) {
        return instIDMap.get(inst);
    }

    public Set<Integer> getKeySet() {
        return lifeTimePoints.keySet();
    }

    public ComparablePair<LifeTimeIndex, LifeTimeIndex> getLifeTimeRange(Integer index) {
        return lifeTimeRange.get(index);
    }

    private int upperBound(List<LifeTimePoint> p, LifeTimeIndex index) {
        int l = 0, r = p.size() - 1, ans = p.size();
        while (l <= r) {
            int mid = (l + r) / 2;
            if (p.get(mid).getIndex().compareTo(index) > 0) {
                ans = mid;
                r = mid - 1;
            } else {
                l = mid + 1;
            }
        }
        return ans;
    }

    private static class Block {
        String blockName;
        int l, r;
        Set<Integer> in = new HashSet<>();
        Set<Integer> out = new HashSet<>();
        Set<Integer> def = new HashSet<>();
        Set<String> nextBlockName = new HashSet<>();
        Block(AsmLabel label) {
            blockName = label.getLabel().getLabelName();
        }
    }

    private Set<Integer> difference(Set<Integer> a, Set<Integer> b) {
        Set<Integer> result = new HashSet<>();
        for (var x : a) {
            if (!b.contains(x)) {
                result.add(x);
            }
        }
        return result;
    }

    private void insertLifeTimePoint(int index, LifeTimePoint p) {
        if (!lifeTimePoints.containsKey(index)) {
            lifeTimePoints.put(index, new ArrayList<>());
        }
        lifeTimePoints.get(index).add(p);
    }

    /**
     * 把y的引用点集合合并入x
     * 这里进行的是直接合并，生命周期还需要使用constructInterval函数进行重构
     * @param x 第一个虚拟寄存器的下标
     * @param y 第二个虚拟寄存器的下标
     */
    public void mergePoints(int x, int y) {
        if (x == y || !lifeTimePoints.containsKey(y)) {
            return;
        }
        if (lifeTimePoints.containsKey(x)) {
            lifeTimePoints.get(x).addAll(lifeTimePoints.get(y));
            lifeTimePoints.get(x).sort(Comparator.comparing(LifeTimePoint::getIndex));
        } else {
            lifeTimePoints.put(x, lifeTimePoints.get(y));
        }
        lifeTimeInterval.remove(y);
        lifeTimeRange.remove(y);
        lifeTimePoints.remove(y);
    }

    public void constructInterval(int x) {
        if (!lifeTimePoints.containsKey(x)) {
            throw new RuntimeException("construct null interval");
        }
        lifeTimePoints.get(x).removeIf((point) -> !instIDMap.containsKey(point.getIndex().getSourceInst()));
        if (lifeTimePoints.get(x).isEmpty()) {
            throw new RuntimeException("no points");
        }
        lifeTimePoints.get(x).sort(Comparator.comparing(LifeTimePoint::getIndex));
        var points = lifeTimePoints.get(x);
        lifeTimeRange.put(x, new ComparablePair<>(points.get(0).getIndex(), points.get(points.size() - 1).getIndex()));
        List<LifeTimeInterval> intervals = new ArrayList<>();
        for (var p : points) {
            if (p.isDef()) {
                intervals.add(new LifeTimeInterval(x, new ComparablePair<>(p.getIndex(), null)));
            }
            intervals.get(intervals.size() - 1).range.b = p.getIndex();
        }
        lifeTimeInterval.put(x, intervals);
    }

    public List<LifeTimeInterval> getInterval(int x) {
        return lifeTimeInterval.get(x);
    }

    private final List<Block> blocks = new ArrayList<>();
    private final Map<String, Block> blockMap = new HashMap<>();
    void buildBlocks(List<AsmInstruction> instructionList) {
        Block now = null;
        for (int i = 0; i < instructionList.size(); i++) {
            var inst = instructionList.get(i);
            if (inst instanceof AsmLabel label) {
                now = new Block(label);
                now.l = i;
                blocks.add(now);
                blockMap.put(now.blockName, now);
            }
            if (now == null) {
                throw new RuntimeException("function with no basic blocks");
            }
            if (inst instanceof AsmJump) {
                for (int j = 1; j <= 3; j++) {
                    if (inst.getOperand(j) instanceof Label label) {
                        now.nextBlockName.add(label.getLabelName());
                    }
                }
            }
            for (var reg : AsmInstructions.getReadVRegSet(inst)) {
                if (!now.def.contains(reg)) {
                    now.in.add(reg);
                }
            }
            now.def.addAll(AsmInstructions.getWriteVRegSet(inst));
            blocks.get(blocks.size() - 1).r = i;
        }
    }
    private void iterateActiveReg() {
        while (true) {
            boolean changeLabel = false;
            for (var b : blocks) {
                for (var nextName : b.nextBlockName) {
                    var next = blockMap.get(nextName);
                    if (next != null) {
                        changeLabel |= b.out.addAll(next.in);
                    }
                }
                changeLabel |= b.in.addAll(difference(b.out, b.def));
            }
            if (!changeLabel) {
                break;
            }
        }
    }
    private void buildLifeTimePoints(List<AsmInstruction> instructionList) {
        for (var b : blocks) {
            for (var x : b.in) {
                LifeTimeIndex index = LifeTimeIndex.getInstOut(this, instructionList.get(b.l));
                insertLifeTimePoint(x, LifeTimePoint.getDef(index));
            }
            for (int i = b.l; i <= b.r; i++) {
                var inst = instructionList.get(i);
                for (var x : AsmInstructions.getReadVRegSet(inst)) {
                    LifeTimeIndex index = LifeTimeIndex.getInstIn(this, instructionList.get(i));
                    insertLifeTimePoint(x, LifeTimePoint.getUse(index));
                }
                for (var x : AsmInstructions.getWriteVRegSet(inst)) {
                    LifeTimeIndex index = LifeTimeIndex.getInstOut(this, instructionList.get(i));
                    insertLifeTimePoint(x, LifeTimePoint.getDef(index));
                }
            }
            for (var x : b.out) {
                LifeTimeIndex index = LifeTimeIndex.getInstIn(this, instructionList.get(b.r));
                insertLifeTimePoint(x, LifeTimePoint.getUse(index));
            }
        }
    }
    private void buildLifeTimeMessage(List<AsmInstruction> instructionList) {
        buildLifeTimePoints(instructionList);
        for (var x : getKeySet()) {
            constructInterval(x);
        }
    }

    public void getAllVRegLifeTime(List<AsmInstruction> instructionList) {
        init();
        buildInstID(instructionList);
        buildBlocks(instructionList);
        iterateActiveReg();
        buildLifeTimeMessage(instructionList);
    }

    /**
     * 在寄存器引用点信息保持维护的情况下重新构建每个寄存器的生存区间集合，用于指令删除后重新构建
     * 也可在维护好添加的指令中变量引用点的情况下进行维护
     * @param instructionList 新指令列表
     */
    public void rebuildLifeTimeInterval(List<AsmInstruction> instructionList) {
        buildInstID(instructionList);
        for (var x : getKeySet()) {
            constructInterval(x);
        }
    }
}
