package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmMove;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.util.AsmInstructions;
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
    private final Map<Register, Set<Register>> edges = new HashMap<>();
    private final Map<Register, Set<Register>> coalesceEdges = new HashMap<>();
    private final Map<Register, Register> physicRegisterMap = new HashMap<>();
    private final Map<Register, Integer> spillCost = new HashMap<>();
    private final static int inf = 0x3f3f3f3f;

    /**
     * 获取寄存器被spill到内存中的代价，物理寄存器的代价总是inf*2，保证不能被spill
     * @param instructionList 作为代价计算依据的指令列表
     */
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
        int nowBlockId = 0;
        for (var reg : registers) {
            spillCost.put(reg, 0);
            for (var point : function.getLifeTimeController().getPoints(reg)) {
                while (nowBlockId + 1 < blockIndexList.size() && blockIndexList.get(nowBlockId + 1).compareTo(point.getIndex()) < 0) {
                    nowBlockId += 1;
                }
                var index = blockIndexList.get(nowBlockId);
                var block = indexBlockMap.get(index);
                var loop = basicBlockLoopMap.get(block);
                int val = 1;
                if (loop != null) {
                    int loopDepth = loop.getLoopDepth();
                    for (int i = 0; i <= loopDepth; i++) {
                        val = Math.toIntExact(Long.min(inf, val * 10L));
                    }
                }
                spillCost.put(reg, Math.min(inf, spillCost.get(reg) + val));
            }
            if (!reg.isVirtual()) {
                spillCost.put(reg, inf * 2);
            }
        }
    }

    /**
     * 添加双向干涉边，存在干涉边的两个变量不能存在可并边
     * @param u 边节点
     * @param v 边节点
     */
    void addEdge(Register u, Register v) {
        coalesceEdges.get(u).remove(v);
        coalesceEdges.get(v).remove(u);
        edges.get(u).add(v);
        edges.get(v).add(u);
    }

    /**
     * 在不存在干涉边的情况下添加可并干涉边
     * @param u 边节点
     * @param v 边节点
     */
    void addCoalesceEdge(Register u, Register v) {
        if (!edges.get(u).contains(v)) {
            coalesceEdges.get(u).add(v);
            coalesceEdges.get(v).add(u);
        }
    }

    /**
     * 为虚拟寄存器和代码中含有的可变物理寄存器建立干涉图
     */
    void buildGraph() {
        edges.clear();
        intervals.clear();
        for (var reg : registers) {
            intervals.addAll(function.getLifeTimeController().getInterval(reg));
            edges.put(reg, new HashSet<>());
            coalesceEdges.put(reg, new HashSet<>());
        }
        Collections.sort(intervals);
        Set<LifeTimeInterval> activeSet = new HashSet<>();
        for (var now : intervals) {
            activeSet.removeIf((r) -> r.range.b.compareTo(now.range.a) < 0);
            for (var last : activeSet) {
                var ru = now.reg;
                var rv = last.reg;
                var defInst = now.range.a.getSourceInst();
                if (defInst instanceof AsmMove asmMove && AsmInstructions.getMoveReg(asmMove).b.equals(rv)) {
                    addCoalesceEdge(ru, rv);
                } else {
                    addEdge(ru, rv);
                }
            }
            activeSet.add(now);
        }
    }

    /**
     * 为干涉图中的所有虚拟寄存器进行染色操作，若成功则结果保存与physicRegisterMap中
     * @return 若成功染色返回true，否则返回false
     */
    boolean color() {
        physicRegisterMap.clear();
        Map<Register, Integer> degree = new HashMap<>();
        for (var x : edges.keySet()) {
            edges.get(x).addAll(coalesceEdges.get(x));
            degree.put(x, edges.get(x).size());
        }
        Queue<Register> queue = new ArrayDeque<>();
        Set<Register> inQueue = new HashSet<>();
        int virtualRegCnt = 0;
        for (var x : edges.keySet()) {
            if (x.isVirtual()) {
                virtualRegCnt += 1;
                if (degree.get(x) < physicRegisters.size()) {
                    queue.add(x);
                    inQueue.add(x);
                }
            } else {
                physicRegisterMap.put(x, x);
            }
        }
        Stack<Register> stack = new Stack<>();
        while (!queue.isEmpty()) {
            var v = queue.remove();
            stack.push(v);
            for (var u : edges.get(v)) {
                degree.put(u, degree.get(u) - 1);
                if (degree.get(u) < physicRegisters.size()) {
                    if (u.isVirtual() && !inQueue.contains(u)) {
                        queue.add(u);
                        inQueue.add(u);
                    }
                }
            }
        }
        if (stack.size() < virtualRegCnt) {
            return false;
        }
        while (!stack.empty()) {
            var v = stack.pop();
            Set<Register> occupied = new HashSet<>();
            for (var u : edges.get(v)) {
                if (physicRegisterMap.containsKey(u)) {
                    occupied.add(physicRegisterMap.get(u));
                }
            }
            for (var pReg : physicRegisters) {
                if (!occupied.contains(pReg)) {
                    physicRegisterMap.put(v, pReg);
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
        getRegisterCost(instructionList);
        buildGraph();
        return instructionList;
    }

    @Override
    public List<AsmInstruction> work(List<AsmInstruction> instructionList) {
        List<Register> intRegList = new ArrayList<>(), floatRegList = new ArrayList<>();
        List<Register> intPRegList = new ArrayList<>(), floatPRegList = new ArrayList<>();
        for (var reg : function.getLifeTimeController().getRegKeySet()) {
            if (reg instanceof IntRegister) {
                intRegList.add(reg);
            } else {
                floatRegList.add(reg);
            }
        }
        for (var reg : Registers.USABLE_REGISTERS) {
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
