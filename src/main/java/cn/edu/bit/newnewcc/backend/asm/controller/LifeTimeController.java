package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmJump;
import cn.edu.bit.newnewcc.backend.asm.operand.Label;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.util.AsmInstructions;
import cn.edu.bit.newnewcc.backend.asm.util.ComparablePair;

import java.util.*;

/**
 * 生命周期控制器，
 */
public class LifeTimeController {
    //虚拟寄存器生命周期的设置过程
    private final AsmFunction function;
    private final Map<Register, ComparablePair<LifeTimeIndex, LifeTimeIndex>> lifeTimeRange = new HashMap<>();
    private final Map<Register, List<LifeTimeInterval>> lifeTimeIntervals = new HashMap<>();
    private final Map<Register, List<LifeTimePoint>> lifeTimePoints = new HashMap<>();
    private final Map<AsmInstruction, Integer> instIDMap = new HashMap<>();

    public Register getVReg(int index) {
        return function.getRegisterAllocator().get(index);
    }

    public LifeTimeController(AsmFunction function) {
        this.function = function;
    }

    private void init() {
        lifeTimeRange.clear();
        lifeTimeIntervals.clear();
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

    public Set<Integer> getVRegKeySet() {
        Set<Integer> res = new HashSet<>();
        for (var x : lifeTimePoints.keySet()) {
            if (x.isVirtual()) {
                res.add(x.getAbsoluteIndex());
            }
        }
        return res;
    }

    public Set<Register> getRegKeySet() {
        return lifeTimePoints.keySet();
    }

    public ComparablePair<LifeTimeIndex, LifeTimeIndex> getLifeTimeRange(Integer index) {
        return lifeTimeRange.get(getVReg(index));
    }

    public ComparablePair<LifeTimeIndex, LifeTimeIndex> getLifeTimeRange(Register reg) {
        return lifeTimeRange.get(reg);
    }

    private static class Block {
        String blockName;
        int l, r;
        Set<Register> in = new HashSet<>();
        Set<Register> out = new HashSet<>();
        Set<Register> def = new HashSet<>();
        Set<String> nextBlockName = new HashSet<>();
        Block(AsmLabel label) {
            blockName = label.getLabel().getLabelName();
        }
    }

    private Set<Register> difference(Set<Register> a, Set<Register> b) {
        Set<Register> result = new HashSet<>();
        for (var x : a) {
            if (!b.contains(x)) {
                result.add(x);
            }
        }
        return result;
    }

    private void insertLifeTimePoint(Register reg, LifeTimePoint p) {
        if (!lifeTimePoints.containsKey(reg)) {
            lifeTimePoints.put(reg, new ArrayList<>());
        }
        lifeTimePoints.get(reg).add(p);
    }

    /**
     * 把y的引用点集合合并入x
     * 这里进行的是直接合并，生命周期还需要使用constructInterval函数进行重构
     * @param x 第一个虚拟寄存器的下标
     * @param y 第二个虚拟寄存器的下标
     */
    public void mergePoints(Register x, Register y) {
        if (x == y || !lifeTimePoints.containsKey(y)) {
            return;
        }
        if (lifeTimePoints.containsKey(x)) {
            lifeTimePoints.get(x).addAll(lifeTimePoints.get(y));
            lifeTimePoints.get(x).sort(Comparator.comparing(LifeTimePoint::getIndex));
        } else {
            lifeTimePoints.put(x, lifeTimePoints.get(y));
        }
        lifeTimeIntervals.remove(y);
        lifeTimeRange.remove(y);
        lifeTimePoints.remove(y);
    }

    public void mergePoints(int x, int y) {
        mergePoints(getVReg(x), getVReg(y));
    }

    public boolean constructInterval(Register x) {
        if (!lifeTimePoints.containsKey(x)) {
            throw new RuntimeException("construct null interval");
        }
        lifeTimePoints.get(x).removeIf((point) -> {
            if (!instIDMap.containsKey(point.getIndex().getSourceInst())) {
                return true;
            }
            var instr = point.getIndex().getSourceInst();
            if (instr instanceof AsmLabel || instr instanceof AsmBlockEnd) {
                return false;
            }
            return AsmInstructions.getRegIndexInInst(point.getIndex().getSourceInst(), x) == -1;
        });
        if (lifeTimePoints.get(x).isEmpty()) {
            return false;
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
        lifeTimeIntervals.put(x, intervals);
        return true;
    }

    public List<LifeTimeInterval> getInterval(int x) {
        return lifeTimeIntervals.get(getVReg(x));
    }

    public List<LifeTimeInterval> getInterval(Register reg) {
        return lifeTimeIntervals.get(reg);
    }

    public List<LifeTimePoint> getPoints(int x) {
        return lifeTimePoints.get(getVReg(x));
    }

    public List<LifeTimePoint> getPoints(Register reg) {
        return lifeTimePoints.get(reg);
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
            for (var reg : AsmInstructions.getReadRegSet(inst)) {
                if (!now.def.contains(reg)) {
                    now.in.add(reg);
                }
            }
            now.def.addAll(AsmInstructions.getWriteRegSet(inst));
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
                for (var x : AsmInstructions.getReadRegSet(inst)) {
                    LifeTimeIndex index = LifeTimeIndex.getInstIn(this, instructionList.get(i));
                    insertLifeTimePoint(x, LifeTimePoint.getUse(index));
                }
                for (var x : AsmInstructions.getWriteRegSet(inst)) {
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
        List<Register> removeList = new ArrayList<>();
        for (var x : getRegKeySet()) {
            if (!constructInterval(x)) {
                removeList.add(x);
            }
        }
        for (var x : removeList) {
            lifeTimePoints.remove(x);
            lifeTimeRange.remove(x);
            lifeTimeIntervals.remove(x);
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

        List<Register> removeList = new ArrayList<>();
        for (var x : getRegKeySet()) {
            if (!constructInterval(x)) {
                removeList.add(x);
            }
        }
        for (var x : removeList) {
            lifeTimePoints.remove(x);
            lifeTimeRange.remove(x);
            lifeTimeIntervals.remove(x);
        }
    }
}
