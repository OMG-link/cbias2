package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmAbstractTag;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmJump;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmStore;
import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.RegisterReplaceable;
import cn.edu.bit.newnewcc.backend.asm.util.ComparablePair;

import java.util.*;

public class LifeTimeController {

    public LifeTimeIndex min(LifeTimeIndex a, LifeTimeIndex b) {
        return a.compareTo(b) < 0 ? a : b;
    }
    public LifeTimeIndex max(LifeTimeIndex a, LifeTimeIndex b) {
        return a.compareTo(b) > 0 ? a : b;
    }

    //虚拟寄存器生命周期的设置过程
    private final Map<Integer, ComparablePair<LifeTimeIndex, LifeTimeIndex>> lifeTimeRange = new HashMap<>();
    private final Map<Integer, List<ComparablePair<LifeTimeIndex, LifeTimeIndex>>> lifeTimeInterval = new HashMap<>();
    private final Map<Integer, List<LifeTimePoint>> lifeTimePoints = new HashMap<>();
    private final Map<AsmInstruction, Integer> instIDMap = new HashMap<>();

    void init() {
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
        return lifeTimeRange.keySet();
    }

    public ComparablePair<LifeTimeIndex, LifeTimeIndex> getLifeTimeRange(Integer index) {
        return lifeTimeRange.get(index);
    }

    int upper_bound(List<LifeTimePoint> p, LifeTimeIndex index) {
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
    public LifeTimePoint getNextUsePoint(int vRegId, LifeTimeIndex index) {
        var points = lifeTimePoints.get(vRegId);
        int x = upper_bound(points, index);
        if (x >= points.size() || points.get(x).isDef()) {
            return null;
        }
        return points.get(x);
    }

    public static Collection<Integer> getWriteRegId(AsmInstruction inst) {
        if (inst instanceof AsmStore) {
            if (inst.getOperand(2) instanceof Register) {
                return Collections.singleton(2);
            }
            return new HashSet<>();
        }
        if (inst instanceof AsmJump) {
            return new HashSet<>();
        }
        if (inst.getOperand(1) instanceof RegisterReplaceable) {
            return Collections.singleton(1);
        }
        return new HashSet<>();
    }

    public static Collection<Integer> getWriteVRegId(AsmInstruction inst) {
        var res = new HashSet<Integer>();
        var regId = getWriteRegId(inst);
        for (var x : regId) {
            var reg = ((RegisterReplaceable)inst.getOperand(x)).getRegister();
            if (reg.isVirtual()) {
                res.add(x);
            }
        }
        return res;
    }
    public static Collection<Integer> getReadRegId(AsmInstruction inst) {
        var res = new HashSet<Integer>();
        for (int i = 1; i <= 3; i++) {
            if (inst.getOperand(i) instanceof RegisterReplaceable) {
                boolean tag = (inst instanceof AsmStore) ? (i == 1 || (i == 2 && !(inst.getOperand(i) instanceof Register))) :
                        (inst instanceof AsmJump || (i > 1));
                if (tag) {
                    res.add(i);
                }
            }
        }
        return res;
    }

    public static Collection<Integer> getReadVRegId(AsmInstruction inst) {
        var res = new HashSet<Integer>();
        var regId = getReadRegId(inst);
        for (var x : regId) {
            var reg = ((RegisterReplaceable)inst.getOperand(x)).getRegister();
            if (reg.isVirtual()) {
                res.add(x);
            }
        }
        return res;
    }

    public static Collection<Integer> getVRegId(AsmInstruction inst) {
        var res = getReadVRegId(inst);
        res.addAll(getWriteVRegId(inst));
        return res;
    }


    public static Collection<Register> getWriteRegSet(AsmInstruction inst) {
        var res = new HashSet<Register>();
        for (int i : getWriteRegId(inst)) {
            RegisterReplaceable op = (RegisterReplaceable) inst.getOperand(i);
            res.add(op.getRegister());
        }
        return res;
    }

    public static Collection<Integer> getWriteVRegSet(AsmInstruction inst) {
        var res = new HashSet<Integer>();
        for (int i : getWriteVRegId(inst)) {
            RegisterReplaceable op = (RegisterReplaceable) inst.getOperand(i);
            res.add(op.getRegister().getIndex());
        }
        return res;
    }

    public static Collection<Register> getReadRegSet(AsmInstruction inst) {
        var res = new HashSet<Register>();
        for (int i : getReadRegId(inst)) {
            RegisterReplaceable op = (RegisterReplaceable) inst.getOperand(i);
            res.add(op.getRegister());
        }
        return res;
    }

    public static Collection<Integer> getReadVRegSet(AsmInstruction inst) {
        var res = new HashSet<Integer>();
        for (int i : getReadVRegId(inst)) {
            RegisterReplaceable op = (RegisterReplaceable) inst.getOperand(i);
            res.add(op.getRegister().getIndex());
        }
        return res;
    }

    static class Block {
        String blockName;
        int l, r;
        Set<Integer> in = new HashSet<>();
        Set<Integer> out = new HashSet<>();
        Set<Integer> def = new HashSet<>();
        Set<String> nextBlockName = new HashSet<>();
        Block(AsmAbstractTag tag) {
            blockName = tag.getPureName();
        }
    }

    Set<Integer> minus(Set<Integer> a, Set<Integer> b) {
        Set<Integer> res = new HashSet<>();
        for (var x : a) {
            if (!b.contains(x)) {
                res.add(x);
            }
        }
        return res;
    }

    void insertInterval(int index, LifeTimeIndex l, LifeTimeIndex r) {
        if (!lifeTimeInterval.containsKey(index)) {
            lifeTimeInterval.put(index, new ArrayList<>());
            lifeTimeRange.put(index, new ComparablePair<>(l, r));
        }
        if (l.compareTo(r) > 0) {
            throw new RuntimeException("life time interval error");
        }
        lifeTimeInterval.get(index).add(new ComparablePair<>(l, r));
        l = min(lifeTimeRange.get(index).a, l);
        r = max(lifeTimeRange.get(index).b, r);
        lifeTimeRange.put(index, new ComparablePair<>(l, r));
    }

    void insertLifeTimePoint(int index, LifeTimePoint p) {
        if (!lifeTimePoints.containsKey(index)) {
            lifeTimePoints.put(index, new ArrayList<>());
        }
        lifeTimePoints.get(index).add(p);
    }

    /**
     * 把y的生命周期集合合并入x
     * 这里进行的是直接合并，mv a, a这类指令删除后还需要进行生存区间的重构，使用reconstructInterval函数进行重构
     * @param x 第一个虚拟寄存器的下标
     * @param y 第二个虚拟寄存器的下标
     */
    public void mergeRange(int x, int y) {
        if (x == y) {
            return;
        }
        if (lifeTimeInterval.containsKey(x) && lifeTimeInterval.containsKey(y)) {
            var iv = lifeTimeInterval.get(x);
            iv.addAll(lifeTimeInterval.get(y));
            Collections.sort(iv);
            var l = min(lifeTimeRange.get(x).a, lifeTimeRange.get(y).a);
            var r = max(lifeTimeRange.get(x).b, lifeTimeRange.get(y).b);
            lifeTimeInterval.put(x, iv);
            lifeTimeRange.put(x, new ComparablePair<>(l, r));
        } else if (lifeTimeInterval.containsKey(y)) {
            lifeTimeInterval.put(x, lifeTimeInterval.get(y));
            lifeTimeRange.put(x, lifeTimeRange.get(y));
        }
        lifeTimeInterval.remove(y);
        lifeTimeRange.remove(y);
    }

    public void reconstructInterval(int x, Map<Integer, Integer> trueValue) {
        var iv = lifeTimeInterval.get(x);
        List<ComparablePair<LifeTimeIndex, LifeTimeIndex>> newInterval = new ArrayList<>();
        ComparablePair<LifeTimeIndex, LifeTimeIndex> last = null;
        for (var r : iv) {
            if (last != null) {
                if (r.a.getSourceInst().isMoveVToV()) {
                    var ids = r.a.getSourceInst().getMoveVReg();
                    if (trueValue.get(ids.a).equals(trueValue.get(ids.b))) {
                        last.b = r.b;
                        continue;
                    }
                }
                if (r.a.compareTo(last.b) <= 0) {
                    last.b = r.b;
                    continue;
                }
                newInterval.add(last);
            }
            last = r;
        }
        if (last != null) {
            newInterval.add(last);
        }
        lifeTimeInterval.put(x, newInterval);
    }

    public List<ComparablePair<LifeTimeIndex, LifeTimeIndex>> getInterval(int x) {
        return lifeTimeInterval.get(x);
    }

    List<Block> blocks = new ArrayList<>();
    Map<String, Block> blockMap = new HashMap<>();
    void buildBlocks(List<AsmInstruction> instructionList) {
        Block now = null;
        for (int i = 0; i < instructionList.size(); i++) {
            var inst = instructionList.get(i);
            if (inst instanceof AsmAbstractTag tag) {
                now = new Block(tag);
                now.l = i;
                blocks.add(now);
                blockMap.put(now.blockName, now);
            }
            if (now == null) {
                throw new RuntimeException("function with no basic blocks");
            }
            if (inst instanceof AsmJump) {
                for (int j = 1; j <= 3; j++) {
                    if (inst.getOperand(j) instanceof GlobalTag tag) {
                        now.nextBlockName.add(tag.getPureName());
                    }
                }
            }
            for (var reg : getReadVRegSet(inst)) {
                if (!now.def.contains(reg)) {
                    now.in.add(reg);
                }
            }
            now.def.addAll(getWriteVRegSet(inst));
            blocks.get(blocks.size() - 1).r = i;
        }
    }
    void iterateActiveReg() {
        while (true) {
            boolean changeTag = false;
            for (var b : blocks) {
                for (var nextName : b.nextBlockName) {
                    var next = blockMap.get(nextName);
                    if (next != null) {
                        changeTag |= b.out.addAll(next.in);
                    }
                }
                changeTag |= b.in.addAll(minus(b.out, b.def));
            }
            if (!changeTag) {
                break;
            }
        }
    }
    void buildLifeTimeMessage(List<AsmInstruction> instructionList) {
        for (var b : blocks) {
            Map<Integer, LifeTimeIndex> defLoc = new HashMap<>();
            Map<Integer, LifeTimeIndex> useLoc = new HashMap<>();
            for (var x : b.in) {
                LifeTimeIndex index = LifeTimeIndex.getInstIn(this, instructionList.get(b.l));
                defLoc.put(x, index);
                insertLifeTimePoint(x, LifeTimePoint.getDef(index));
            }
            for (int i = b.l; i <= b.r; i++) {
                var inst = instructionList.get(i);
                for (var x : getReadVRegSet(inst)) {
                    LifeTimeIndex index = LifeTimeIndex.getInstIn(this, instructionList.get(i));
                    useLoc.put(x, index);
                    insertLifeTimePoint(x, LifeTimePoint.getUse(index));
                }
                for (var x : getWriteVRegSet(inst)) {
                    if (defLoc.containsKey(x) && useLoc.containsKey(x)) {
                        insertInterval(x, defLoc.get(x), useLoc.get(x));
                        useLoc.remove(x);
                    }
                    LifeTimeIndex index = LifeTimeIndex.getInstOut(this, instructionList.get(i));
                    defLoc.put(x, index);
                    insertLifeTimePoint(x, LifeTimePoint.getDef(index));
                }
            }
            for (var x : b.out) {
                LifeTimeIndex index = LifeTimeIndex.getInstOut(this, instructionList.get(b.r));
                insertInterval(x, defLoc.get(x), index);
                insertLifeTimePoint(x, LifeTimePoint.getUse(index));
                useLoc.remove(x);
                defLoc.remove(x);
            }
            for (var x : useLoc.keySet()) {
                insertInterval(x, defLoc.get(x), useLoc.get(x));
                defLoc.remove(x);
            }
            for (var x : defLoc.keySet()) {
                insertInterval(x, defLoc.get(x), LifeTimeIndex.getInstOut(this, instructionList.get(defLoc.get(x).getInstID())));
            }
        }
    }

    public void getAllVRegLifeTime(List<AsmInstruction> instructionList) {
        init();
        buildInstID(instructionList);
        buildBlocks(instructionList);
        iterateActiveReg();
        buildLifeTimeMessage(instructionList);
    }
}
