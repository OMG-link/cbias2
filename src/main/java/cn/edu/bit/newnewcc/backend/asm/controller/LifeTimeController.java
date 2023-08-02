package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.RegisterReplaceable;
import cn.edu.bit.newnewcc.backend.asm.util.ComparablePair;
import cn.edu.bit.newnewcc.backend.asm.util.Pair;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class LifeTimeController {
    //虚拟寄存器生命周期的设置过程
    private final Map<Integer, ComparablePair<Integer, Integer>> lifeTimeRange = new HashMap<>();
    private final Map<Integer, List<ComparablePair<Integer, Integer>>> lifeTimeInterval = new HashMap<>();

    void init() {
        lifeTimeRange.clear();
        lifeTimeInterval.clear();
        blocks.clear();
        blockMap.clear();
    }

    public Set<Integer> getKeySet() {
        return lifeTimeRange.keySet();
    }

    public ComparablePair<Integer, Integer> getLifeTimeRange(Integer index) {
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
            if (inst.getOperand(i) instanceof RegisterReplaceable op) {
                boolean tag = (inst instanceof AsmStore) ? (i == 1 || (i == 2 && !(inst.getOperand(i) instanceof Register))) :
                        (inst instanceof AsmJump || (i > 1));
                if (tag) {
                    res.add(i);
                }
            }
        }
        return res;
    }

    public static Collection<Integer> getReadVregId(AsmInstruction inst) {
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
        var res = getReadVregId(inst);
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
        for (int i : getReadVregId(inst)) {
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

    void insertIntervel(int index, int l, int r) {
        if (!lifeTimeInterval.containsKey(index)) {
            lifeTimeInterval.put(index, new ArrayList<>());
        }
        assert(l < r);
        lifeTimeInterval.get(index).add(new ComparablePair<>(l, r));
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
            List<ComparablePair<Integer, Integer>> res = new ArrayList<>();
            ComparablePair<Integer, Integer> lst = null;
            for (var i : iv) {
                if (lst == null) {
                    lst = i;
                } else if (lst.b + 1 < i.a) {
                    res.add(lst);
                    lst = i;
                } else {
                    lst.b = i.b;
                }
            }
            if (lst != null) {
                res.add(lst);
            }
            int l = min(lifeTimeRange.get(x).a, lifeTimeRange.get(y).a);
            int r = max(lifeTimeRange.get(x).b, lifeTimeRange.get(y).b);
            lifeTimeInterval.put(x, iv);
            lifeTimeRange.put(x, new ComparablePair<>(l, r));
        } else if (lifeTimeInterval.containsKey(y)) {
            lifeTimeInterval.put(x, lifeTimeInterval.get(y));
            lifeTimeRange.put(x, lifeTimeRange.get(y));
        }
        lifeTimeInterval.remove(y);
        lifeTimeRange.remove(y);
    }

    public List<ComparablePair<Integer, Integer>> getInterval(int x) {
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
            Map<Integer, Integer> defLoc = new HashMap<>();
            Map<Integer, Integer> useLoc = new HashMap<>();
            for (var x : b.in) {
                defLoc.put(x, b.l);
            }
            for (int i = b.l; i <= b.r; i++) {
                var inst = instructionList.get(i);
                for (var x : getReadVregSet(inst)) {
                    useLoc.put(x, i);
                }
                for (var x : getWriteVregSet(inst)) {
                    if (defLoc.containsKey(x) && useLoc.containsKey(x)) {
                        insertIntervel(x, defLoc.get(x), useLoc.get(x));
                        useLoc.remove(x);
                    }
                    defLoc.put(x, i);
                }
            }
            for (var x : b.out) {
                insertIntervel(x, defLoc.get(x), b.r + 1);
                useLoc.remove(x);
                defLoc.remove(x);
            }
            for (var x : useLoc.keySet()) {
                insertIntervel(x, defLoc.get(x), useLoc.get(x));
                defLoc.remove(x);
            }
            for (var x : defLoc.keySet()) {
                insertIntervel(x, defLoc.get(x), defLoc.get(x) + 1);
            }
        }
        for (var x : lifeTimeInterval.keySet()) {
            Collections.sort(lifeTimeInterval.get(x));
            var range = lifeTimeInterval.get(x);
            int l = range.get(0).a;
            int r = range.get(range.size() - 1).b;
            lifeTimeRange.put(x, new ComparablePair<>(l, r));
        }
    }
}
