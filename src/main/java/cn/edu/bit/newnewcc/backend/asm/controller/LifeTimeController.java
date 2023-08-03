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

    public static class LifeTimeIndex implements Comparable<LifeTimeIndex>{
        public enum TYPE {
            in, out
        }
        int instID;
        public TYPE type;
        public LifeTimeIndex(int instID, TYPE type) {
            this.instID = instID;
            this.type = type;
        }

        public int getInstID() {
            return instID;
        }
        public TYPE getType() {
            return type;
        }

        @Override
        public int compareTo(LifeTimeIndex o) {
            if (instID != o.instID) {
                return instID - o.instID;
            }
            if (type != o.type) {
                return type == TYPE.in ? -1 : 1;
            }
            return 0;
        }
    }

    public LifeTimeIndex min(LifeTimeIndex a, LifeTimeIndex b) {
        return a.compareTo(b) < 0 ? a : b;
    }
    public LifeTimeIndex max(LifeTimeIndex a, LifeTimeIndex b) {
        return a.compareTo(b) > 0 ? a : b;
    }

    //虚拟寄存器生命周期的设置过程
    private final Map<Integer, ComparablePair<LifeTimeIndex, LifeTimeIndex>> lifeTimeRange = new HashMap<>();
    private final Map<Integer, List<ComparablePair<LifeTimeIndex, LifeTimeIndex>>> lifeTimeInterval = new HashMap<>();

    void init() {
        lifeTimeRange.clear();
        lifeTimeInterval.clear();
        blocks.clear();
        blockMap.clear();
    }

    public Set<Integer> getKeySet() {
        return lifeTimeRange.keySet();
    }

    public ComparablePair<LifeTimeIndex, LifeTimeIndex> getLifeTimeRange(Integer index) {
        return lifeTimeRange.get(index);
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

    public static Collection<Integer> getWriteVregId(AsmInstruction inst) {
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

    public static Collection<Integer> getVregId(AsmInstruction inst) {
        var res = getReadVRegId(inst);
        res.addAll(getWriteVregId(inst));
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

    public static Collection<Integer> getWriteVregSet(AsmInstruction inst) {
        var res = new HashSet<Integer>();
        for (int i : getWriteVregId(inst)) {
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

    public static Collection<Integer> getReadVregSet(AsmInstruction inst) {
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

    void clearInterval(int x) {
        lifeTimeInterval.remove(x);
        lifeTimeRange.remove(x);
    }

    void insertInterval(int index, LifeTimeIndex l, LifeTimeIndex r) {
        if (!lifeTimeInterval.containsKey(index)) {
            lifeTimeInterval.put(index, new ArrayList<>());
            lifeTimeRange.put(index, new ComparablePair<>(l, r));
        }
        assert(l.compareTo(r) < 0);
        lifeTimeInterval.get(index).add(new ComparablePair<>(l, r));
        l = min(lifeTimeRange.get(index).a, l);
        r = max(lifeTimeRange.get(index).b, r);
        lifeTimeRange.put(index, new ComparablePair<>(l, r));
    }

    /**
     * 把y的生命周期集合合并入x
     * @param x 第一个虚拟寄存器的下标
     * @param y 第二个虚拟寄存器的下标
     */
    void mergeRange(int x, int y) {
        if (x == y) {
            return;
        }
        if (lifeTimeInterval.containsKey(x) && lifeTimeInterval.containsKey(y)) {
            var iv = lifeTimeInterval.get(x);
            iv.addAll(lifeTimeInterval.get(y));
            Collections.sort(iv);
            List<ComparablePair<LifeTimeIndex, LifeTimeIndex>> res = new ArrayList<>();
            ComparablePair<LifeTimeIndex, LifeTimeIndex> lst = null;
            for (var i : iv) {
                if (lst == null) {
                    lst = i;
                } else if (lst.b.compareTo(i.a) <= 0) {
                    res.add(lst);
                    lst = i;
                } else {
                    lst.b = i.b;
                }
            }
            if (lst != null) {
                res.add(lst);
            }
            var l = min(lifeTimeRange.get(x).a, lifeTimeRange.get(y).a);
            var r = max(lifeTimeRange.get(x).b, lifeTimeRange.get(y).b);
            lifeTimeInterval.put(x, res);
            lifeTimeRange.put(x, new ComparablePair<>(l, r));
        } else if (lifeTimeInterval.containsKey(y)) {
            lifeTimeInterval.put(x, lifeTimeInterval.get(y));
            lifeTimeRange.put(x, lifeTimeRange.get(y));
        }
        lifeTimeInterval.remove(y);
        lifeTimeRange.remove(y);
    }

    public List<ComparablePair<LifeTimeIndex, LifeTimeIndex>> getInterval(int x) {
        return lifeTimeInterval.get(x);
    }

    List<Block> blocks = new ArrayList<>();
    Map<String, Block> blockMap = new HashMap<>();
    public void getAllVregLifeTime(List<AsmInstruction> instructionList) {
        init();
        Block now = null;
        for (int i = 0; i < instructionList.size(); i++) {
            var inst = instructionList.get(i);
            if (inst instanceof AsmAbstractTag tag) {
                now = new Block(tag);
                now.l = i;
                blocks.add(now);
                blockMap.put(now.blockName, now);
            }
            assert now != null;
            if (inst instanceof AsmJump) {
                for (int j = 1; j <= 3; j++) {
                    if (inst.getOperand(j) instanceof GlobalTag tag) {
                        now.nextBlockName.add(tag.getPureName());
                    }
                }
            }
            for (var reg : getReadVregSet(inst)) {
                if (!now.def.contains(reg)) {
                    now.in.add(reg);
                }
            }
            now.def.addAll(getWriteVregSet(inst));
            blocks.get(blocks.size() - 1).r = i;
        }
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
        for (var b : blocks) {
            Map<Integer, LifeTimeIndex> defLoc = new HashMap<>();
            Map<Integer, LifeTimeIndex> useLoc = new HashMap<>();
            for (var x : b.in) {
                defLoc.put(x, new LifeTimeIndex(b.l, LifeTimeIndex.TYPE.in));
            }
            for (int i = b.l; i <= b.r; i++) {
                var inst = instructionList.get(i);
                for (var x : getReadVregSet(inst)) {
                    useLoc.put(x, new LifeTimeIndex(i, LifeTimeIndex.TYPE.in));
                }
                for (var x : getWriteVregSet(inst)) {
                    if (defLoc.containsKey(x) && useLoc.containsKey(x)) {
                        insertInterval(x, defLoc.get(x), useLoc.get(x));
                        useLoc.remove(x);
                    }
                    defLoc.put(x, new LifeTimeIndex(i, LifeTimeIndex.TYPE.out));
                }
            }
            for (var x : b.out) {
                insertInterval(x, defLoc.get(x), new LifeTimeIndex(b.r, LifeTimeIndex.TYPE.out));
                useLoc.remove(x);
                defLoc.remove(x);
            }
            for (var x : useLoc.keySet()) {
                insertInterval(x, defLoc.get(x), useLoc.get(x));
                defLoc.remove(x);
            }
            for (var x : defLoc.keySet()) {
                insertInterval(x, defLoc.get(x), new LifeTimeIndex(defLoc.get(x).getInstID(), LifeTimeIndex.TYPE.out));
            }
        }
    }
}
