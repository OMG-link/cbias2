package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.backend.asm.controller.LifeTimeController;
import cn.edu.bit.newnewcc.backend.asm.controller.LifeTimeIndex;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.operand.RegisterReplaceable;

import java.util.*;

public class CopyCoalescer {
    ArrayList<AsmInstruction> instructions;
    LifeTimeController lifeTimeController;
    static class LifeTimeInterval {
        ComparablePair<LifeTimeIndex, LifeTimeIndex> range;
        int vRegID;
        public LifeTimeInterval(int vRegID, ComparablePair<LifeTimeIndex, LifeTimeIndex> range) {
            this.vRegID = vRegID;
            this.range = range;
        }

        @Override
        public String toString() {
            return vRegID + ":" + range.toString();
        }
    }
    private final Map<LifeTimeInterval, LifeTimeInterval> value = new HashMap<>();
    private final Map<Integer, List<LifeTimeInterval>> intervalMap = new HashMap<>();
    private final DSU<Integer> dsu = new DSU<>();
    private final List<LifeTimeInterval> intervals = new ArrayList<>();
    private final Map<Integer, Set<Integer>> clique = new HashMap<>();
    private final Map<Integer, Set<Integer>> edges = new HashMap<>();

    public CopyCoalescer(ArrayList<AsmInstruction> instructions, LifeTimeController lifeTimeController) {
        this.instructions = instructions;
        this.lifeTimeController = lifeTimeController;
    }

    LifeTimeInterval getValue(LifeTimeInterval lifeTimeInterval) {
        if (!value.get(lifeTimeInterval).equals(lifeTimeInterval)) {
            value.put(lifeTimeInterval, getValue(value.get(lifeTimeInterval)));
        }
        return value.get(lifeTimeInterval);
    }

    void getIntervals() {
        for (int x : lifeTimeController.getKeySet()) {
            intervalMap.put(x, new ArrayList<>());
            for (var i : lifeTimeController.getInterval(x)) {
                LifeTimeInterval interval = new LifeTimeInterval(x, i);
                intervals.add(interval);
                intervalMap.get(x).add(interval);
            }
        }
        intervals.sort(Comparator.comparing(a -> a.range));
    }

    void getValues() {
        Map<Integer, LifeTimeInterval> lastActive = new HashMap<>();
        for (var interval : intervals) {
            value.put(interval, interval);
            int defID = interval.range.a.getInstID();
            var inst = instructions.get(defID);
            if (inst.isMoveVToV()) {
                var id = inst.getMoveVReg();
                if (id.a.equals(interval.vRegID)) {
                    var sourceID = id.b;
                    value.put(interval, getValue(lastActive.get(sourceID)));
                    dsu.merge(interval.vRegID, sourceID);
                }
            }
            lastActive.put(interval.vRegID, interval);
        }
    }

    void addEdge(int x, int y) {
        if (x == y) {
            throw new RuntimeException("coalesce error! conference on same value");
        }
        edges.get(x).add(y);
        edges.get(y).add(x);
    }

    void removeEdge(int x, int y) {
        edges.get(x).remove(y);
        edges.get(y).remove(x);
    }

    void getEdges() {
        Set<LifeTimeInterval> activeSet = new HashSet<>();
        for (var x : lifeTimeController.getKeySet()) {
            edges.put(x, new HashSet<>());
        }
        for (var now : intervals) {
            activeSet.removeIf(last -> last.range.b.compareTo(now.range.a) < 0);
            for (var last : activeSet) {
                if (!getValue(last).equals(getValue(now)) && dsu.getfa(last.vRegID).equals(dsu.getfa(now.vRegID))) {
                    addEdge(last.vRegID, now.vRegID);
                }
            }
            activeSet.add(now);
        }
        for (var i : lifeTimeController.getKeySet()) {
            int x = dsu.getfa(i);
            if (!clique.containsKey(x)) {
                clique.put(x, new HashSet<>());
            }
            clique.get(x).add(i);
        }
    }

    void coalesce() {
        Map<Integer, Integer> trueValue = new HashMap<>();
        for (var x : lifeTimeController.getKeySet()) {
            trueValue.put(x, x);
        }
        for (var x : clique.keySet()) {
            var points = clique.get(x);
            if (points.isEmpty()) {
                continue;
            }
            while (true) {
                int maxID = -1, maxValue = -1;
                for (var point : points) {
                    if (edges.get(point).size() > maxValue) {
                        maxValue = edges.get(point).size();
                        maxID = point;
                    }
                }
                if (maxValue <= 0) {
                    break;
                }
                points.remove(maxID);
                var out = edges.get(maxID).toArray();
                for (var u : out) {
                    removeEdge(maxID, (Integer)u);
                }
            }
            var array = points.toArray();
            int v = (Integer)array[0];
            for (int i = 1; i < array.length; i++) {
                int u = (Integer)array[i];
                trueValue.put(u, v);
                lifeTimeController.mergeRange(v, u);
            }
            lifeTimeController.reconstructInterval(v, trueValue);
        }
        for (var inst : instructions) {
            for (int i : inst.getVRegId()) {
                Register reg = ((RegisterReplaceable) inst.getOperand(i)).getRegister();
                reg.setIndex(-trueValue.get(reg.getIndex()));
            }
        }
    }

    List<AsmInstruction> filtInstructions() {
        List<AsmInstruction> newInstructionList = new ArrayList<>();
        for (var inst : instructions) {
            if (inst.isMoveVToV()) {
                var r = inst.getMoveVReg();
                if (r.a.equals(r.b)) {
                    continue;
                }
            }
            newInstructionList.add(inst);
        }
        lifeTimeController.buildInstID(newInstructionList);
        return newInstructionList;
    }

    public List<AsmInstruction> work() {
        getIntervals();
        getValues();
        getEdges();
        coalesce();
        return filtInstructions();
    }
}
