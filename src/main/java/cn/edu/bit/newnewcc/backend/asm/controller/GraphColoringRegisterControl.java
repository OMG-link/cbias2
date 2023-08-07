package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.util.Registers;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.pass.ir.structure.LoopForest;

import java.util.*;

public class GraphColoringRegisterControl extends RegisterControl {
    public GraphColoringRegisterControl(AsmFunction function, StackAllocator allocator) {
        super(function, allocator);
    }

    private List<Register> registers, physicRegisters;
    private final List<LifeTimeInterval> intervals = new ArrayList<>();
    private final Map<Integer, Set<Integer>> edges = new HashMap<>();
    private final Map<Register, Register> physicRegisterMap = new HashMap<>();

    void getRegisterCost(List<AsmInstruction> instructionList) {
        var irFunction = function.getBaseFunction();
        var loopForest = LoopForest.buildOver(irFunction);
        var basicBlockLoopMap = loopForest.getBasicBlockLoopMap();
        Map<LifeTimeIndex, BasicBlock> indexBlockMap = new HashMap<>();
        List<LifeTimeIndex> blockIndexList = new ArrayList<>();
        for (var instr : instructionList) {
            if (instr instanceof AsmLabel label) {
                LifeTimeIndex index = LifeTimeIndex.getInstIn(function.getLifeTimeController(), instr);
                blockIndexList.add(index);
                indexBlockMap.put(index, function.getBasicBlockByLabel(label));
            }
        }
    }

    Register getVReg(int x) {
        return function.getRegisterAllocator().get(x);
    }

    void buildGraph() {
        edges.clear();
        intervals.clear();
        for (var reg : registers) {
            intervals.addAll(function.getLifeTimeController().getInterval(reg.getIndex()));
            edges.put(reg.getIndex(), new HashSet<>());
        }
        Collections.sort(intervals);
        Set<LifeTimeInterval> activeSet = new HashSet<>();
        for (var now : intervals) {
            activeSet.removeIf((r) -> r.range.b.compareTo(now.range.a) < 0);
            for (var last : activeSet) {
                int u = now.vRegID, v = last.vRegID;
                edges.get(u).add(v);
                edges.get(v).add(u);
            }
            activeSet.add(now);
        }
    }

    boolean color() {
        physicRegisterMap.clear();
        Map<Integer, Integer> degree = new HashMap<>();
        for (var x : edges.keySet()) {
            degree.put(x, edges.get(x).size());
        }
        Queue<Integer> queue = new ArrayDeque<>();
        Set<Integer> inQueue = new HashSet<>();
        for (var x : edges.keySet()) {
            if (degree.get(x) < physicRegisters.size()) {
                queue.add(x);
                inQueue.add(x);
            }
        }
        Stack<Integer> stack = new Stack<>();
        while (!queue.isEmpty()) {
            int v = queue.remove();
            stack.push(v);
            for (var u : edges.get(v)) {
                degree.put(u, degree.get(u) - 1);
                if (degree.get(u) < physicRegisters.size()) {
                    if (!inQueue.contains(u)) {
                        queue.add(u);
                        inQueue.add(u);
                    }
                }
            }
        }
        if (stack.size() < registers.size()) {
            return false;
        }
        while (!stack.empty()) {
            int v = stack.pop();
            Set<Register> occupied = new HashSet<>();
            for (var u : edges.get(v)) {
                if (physicRegisterMap.containsKey(getVReg(u))) {
                    occupied.add(physicRegisterMap.get(getVReg(u)));
                }
            }
            for (var pReg : physicRegisters) {
                if (!occupied.contains(pReg)) {
                    physicRegisterMap.put(getVReg(v), pReg);
                    break;
                }
            }
        }
        return true;
    }

    /**
     * 为registers列表中的每个虚拟寄存器分配物理寄存器，同时修改
     * @param instructionList 分配前的指令列表
     * @return 分配后的指令列表
     */
    public List<AsmInstruction> allocatePhysicalRegisters(List<AsmInstruction> instructionList) {
        buildGraph();
        return instructionList;
    }

    @Override
    public List<AsmInstruction> work(List<AsmInstruction> instructionList) {
        List<Register> intRegList = new ArrayList<>(), floatRegList = new ArrayList<>();
        List<Register> intPRegList = new ArrayList<>(), floatPRegList = new ArrayList<>();
        for (var x : function.getLifeTimeController().getKeySet()) {
            var reg = getVReg(x);
            if (reg instanceof IntRegister) {
                intRegList.add(reg);
            } else {
                floatRegList.add(reg);
            }
        }
        for (var reg : Registers.getUsableRegisters()) {
            if (reg instanceof IntRegister) {
                intPRegList.add(reg);
            } else {
                floatPRegList.add(reg);
            }
        }
        registers = intRegList;
        physicRegisters = intPRegList;
        instructionList = allocatePhysicalRegisters(instructionList);
        registers = floatRegList;
        physicRegisters = floatPRegList;
        instructionList = allocatePhysicalRegisters(instructionList);
        return instructionList;
    }
}
