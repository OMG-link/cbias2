package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.AsmOperand;
import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.RegisterReplaceable;
import cn.edu.bit.newnewcc.backend.asm.util.Pair;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class LifeTimeController {
    //虚拟寄存器生命周期的设置过程
    private final AsmFunction function;
    private final Map<Integer, Pair<Integer, Integer>> lifeTimeRange = new HashMap<>();
    private final Map<Integer, List<Pair<Integer, Integer>>> lifeTimeInterval = new HashMap<>();

    public Set<Integer> getKeySet() {
        return lifeTimeRange.keySet();
    }

    public LifeTimeController(AsmFunction function) {
        this.function = function;
    }

    public Pair<Integer, Integer> getLifeTimeRange(Integer index) {
        return lifeTimeRange.get(index);
    }

    public Collection<Integer> getWriteVregSet(AsmInstruction inst) {
        if (inst instanceof AsmStore) {
            if (inst.getOperand(2) instanceof RegisterReplaceable registerReplaceable) {
                var reg = registerReplaceable.getRegister();
                if (reg.isVirtual()) {
                    return Collections.singleton(reg.getIndex());
                }
            }
            return new HashSet<>();
        }
        if (inst instanceof AsmJump) {
            return new HashSet<>();
        }
        if (inst.getOperand(1) instanceof RegisterReplaceable registerReplaceable) {
            var reg = registerReplaceable.getRegister();
            if (reg.isVirtual()) {
                return Collections.singleton(reg.getIndex());
            }
        }
        return new HashSet<>();
    }

    public Collection<Integer> getReadVregSet(AsmInstruction inst) {
        var res = new HashSet<Integer>();
        for (int i = 1; i <= 3; i++) {
            if (inst.getOperand(i) instanceof RegisterReplaceable op) {
                if ((i == 1 && (inst instanceof AsmStore || inst instanceof AsmJump)) || (i > 1)) {
                    var reg = op.getRegister();
                    if (reg.isVirtual()) {
                        res.add(reg.getIndex());
                    }
                }
            }
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
        lifeTimeInterval.get(index).add(new Pair<>(l, r));
    }

    List<Block> blocks = new ArrayList<>();
    Map<String, Block> blockMap = new HashMap<>();
    public void getAllVregLifeTime(List<AsmInstruction> instructionList) {
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
            lifeTimeInterval.get(x).sort((a, b) -> {
                if (!Objects.equals(a.a, b.a)) {
                    return a.a - b.a;
                }
                return a.b - b.b;
            });
            var range = lifeTimeInterval.get(x);
            int l = range.get(0).a;
            int r = range.get(range.size() - 1).b;
            lifeTimeRange.put(x, new Pair<>(l, r));
        }
    }
}
