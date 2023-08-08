package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmLabel;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmMove;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.AsmInstructions;
import cn.edu.bit.newnewcc.backend.asm.util.Pair;
import cn.edu.bit.newnewcc.backend.asm.util.Registers;
import cn.edu.bit.newnewcc.backend.asm.util.Utility;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.pass.ir.structure.LoopForest;

import java.util.*;

public class GraphColoringRegisterControl extends RegisterControl {
    public GraphColoringRegisterControl(AsmFunction function, StackAllocator allocator) {
        super(function, allocator);
        lifeTimeController = function.getLifeTimeController();
    }

    private List<Register> registers, physicRegisters;
    private final LifeTimeController lifeTimeController;
    private final List<LifeTimeInterval> intervals = new ArrayList<>();

    private final Map<Register, Set<Register>> interferenceEdges = new HashMap<>();
    private final Map<Register, Set<Register>> coalescentEdges = new HashMap<>();
    private final Set<Register> uncoloredReg = new HashSet<>();
    private final Set<Register> coalescentReg = new HashSet<>();

    private final Stack<Register> stack = new Stack<>();
    private final Map<Register, Register> physicRegisterMap = new HashMap<>();
    private final Map<Register, Integer> spillCost = new HashMap<>();
    private List<AsmInstruction> instList;
    private final static int inf = 0x3f3f3f3f;

    /**
     * 获取寄存器被spill到内存中的代价，物理寄存器的代价总是inf*2，保证不能被spill
     */
    private void getRegisterCost() {
        var irFunction = function.getBaseFunction();
        var loopForest = LoopForest.buildOver(irFunction);
        var basicBlockLoopMap = loopForest.getBasicBlockLoopMap();
        Map<LifeTimeIndex, BasicBlock> indexBlockMap = new HashMap<>();
        List<LifeTimeIndex> blockIndexList = new ArrayList<>();
        for (var instr : instList) {
            if (instr instanceof AsmLabel label) {
                LifeTimeIndex index = LifeTimeIndex.getInstIn(lifeTimeController, instr);
                blockIndexList.add(index);
                indexBlockMap.put(index, function.getBasicBlockByLabel(label));
            }
        }
        int nowBlockId = 0;
        for (var reg : registers) {
            spillCost.put(reg, 0);
            for (var point : lifeTimeController.getPoints(reg)) {
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
    private void addInterferenceEdge(Register u, Register v) {
        if (!u.equals(v)) {
            coalescentEdges.get(u).remove(v);
            coalescentEdges.get(v).remove(u);
            interferenceEdges.get(u).add(v);
            interferenceEdges.get(v).add(u);
        }
    }

    /**
     * 在不存在干涉边的情况下添加可并干涉边
     * @param u 边节点
     * @param v 边节点
     */
    private void addCoalesceEdge(Register u, Register v) {
        if (!u.equals(v) && !interferenceEdges.get(u).contains(v)) {
            coalescentEdges.get(u).add(v);
            coalescentEdges.get(v).add(u);
        }
    }

    private void removeEdge(Register u, Register v) {
        if (interferenceEdges.get(u).contains(v)) {
            interferenceEdges.get(u).remove(v);
            interferenceEdges.get(v).remove(u);
        }
        if (coalescentEdges.get(u).contains(v)) {
            coalescentEdges.get(u).remove(v);
            coalescentEdges.get(v).remove(u);
        }
    }

    /**
     * 为虚拟寄存器和代码中含有的可变物理寄存器建立干涉图
     */
    private void buildGraph() {
        intervals.clear();
        interferenceEdges.clear();
        coalescentEdges.clear();
        for (var reg : registers) {
            intervals.addAll(lifeTimeController.getInterval(reg));
            interferenceEdges.put(reg, new HashSet<>());
            coalescentEdges.put(reg, new HashSet<>());
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
                    addInterferenceEdge(ru, rv);
                }
            }
            activeSet.add(now);
        }
        uncoloredReg.clear();
        coalescentReg.clear();
        for (var reg : registers) {
            if (reg.isVirtual()) {
                if (coalescentEdges.get(reg).size() == 0) {
                    uncoloredReg.add(reg);
                } else {
                    coalescentReg.add(reg);
                }
            }
        }
    }

    /**
     * 未当前剩下的未着色的寄存器进行着色，若成功则重建图并构建出每个寄存器对应的物理寄存器
     * @return 若成功染色返回true，否则返回false
     */
    private boolean color() {
        Queue<Register> queue = new ArrayDeque<>();
        Set<Register> visited = new HashSet<>();
        for (var x : uncoloredReg) {
            if (interferenceEdges.get(x).size() < physicRegisters.size()) {
                queue.add(x);
                visited.add(x);
            }
        }
        while (!queue.isEmpty()) {
            var v = queue.remove();
            stack.push(v);
            uncoloredReg.remove(v);
            for (var u : Set.copyOf(interferenceEdges.get(v))) {
                removeEdge(u, v);
                if (interferenceEdges.get(u).size() < physicRegisters.size()) {
                    if (uncoloredReg.contains(u) && !visited.contains(u)) {
                        queue.add(u);
                        visited.add(u);
                    }
                }
            }
        }
        if (uncoloredReg.size() + coalescentReg.size() > 0) {
            return false;
        }
        buildGraph();
        physicRegisterMap.clear();
        for (var reg : registers) {
            if (!reg.isVirtual()) {
                physicRegisterMap.put(reg, reg);
            }
        }
        while (!stack.empty()) {
            var v = stack.pop();
            Set<Register> occupied = new HashSet<>();
            for (var u : interferenceEdges.get(v)) {
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
     * 检查是否能将寄存器v合并入寄存器u
     * @param u 并入的寄存器
     * @param v 被合并的寄存器
     * @return 是否能够合并
     */
    private boolean checkCoalesce(Register u, Register v) {
        if (!v.isVirtual() || !coalescentEdges.containsKey(u) || !coalescentEdges.get(u).contains(v)) {
            return false;
        }
        if (interferenceEdges.get(u).size() < interferenceEdges.get(v).size()) {
            var tmp = u;
            u = v;
            v = tmp;
        }
        int degree = interferenceEdges.get(u).size();
        for (var x : interferenceEdges.get(v)) {
            if (degree >= registers.size()) {
                break;
            }
            if (x.equals(u)) {
                continue;
            }
            degree += interferenceEdges.get(u).contains(x) ? 0 : 1;
        }
        return degree < registers.size();
    }

    void mergeEdges(Register u, Register v) {
        removeEdge(u, v);
        for (var x : interferenceEdges.get(v)) {
            addInterferenceEdge(u, x);
        }
        for (var x : coalescentEdges.get(v)) {
            addCoalesceEdge(u, x);
        }
        interferenceEdges.remove(v);
        coalescentEdges.remove(v);
    }

    /**
     * 执行将寄存器v合并入寄存器u的过程，分为替换指令，合并生存点和合并干涉图三步，最后删除所有和节点v相关的信息
     * @param u 并入的寄存器
     * @param v 被合并的寄存器
     */
    private void practiseCoalesce(Register u, Register v) {
        for (var point : lifeTimeController.getPoints(v)) {
            var inst = point.getIndex().getSourceInst();
            int id = AsmInstructions.getInstRegID(inst, v);
            if (id != -1) {
                inst.setOperand(id, ((RegisterReplaceable)inst.getOperand(id)).replaceRegister(u));
            }
        }
        lifeTimeController.mergePoints(u, v);
        lifeTimeController.constructInterval(u);
        mergeEdges(u, v);
        if (coalescentEdges.get(u).size() == 0 && u.isVirtual()) {
            coalescentReg.remove(u);
            uncoloredReg.add(u);
        }
        coalescentReg.remove(v);
        registers.remove(v);
        spillCost.remove(v);
    }

    /**
     * 寄存器合并过程，若成功合并寄存器，则重新进行着色检查
     * @return 是否成功合并寄存器
     */
    private boolean coalesce() {
        List<Pair<Register, Register>> coalescePair = new ArrayList<>();
        for (var u : coalescentReg) {
            for (var v : coalescentEdges.get(u)) {
                coalescePair.add(new Pair<>(u, v));
            }
        }
        boolean coalesced = false;
        for (var p : coalescePair) {
            if (checkCoalesce(p.a, p.b)) {
                practiseCoalesce(p.a, p.b);
                coalesced = true;
            }
        }
        return coalesced;
    }

    boolean freeze() {
        Register freezeReg = null;
        for (var v : coalescentReg) {
            if (freezeReg == null || interferenceEdges.get(v).size() < interferenceEdges.get(freezeReg).size()) {
                freezeReg = v;
            }
        }
        if (freezeReg != null) {
            coalescentEdges.get(freezeReg).clear();
            coalescentReg.remove(freezeReg);
            return true;
        }
        return false;
    }

    /**
     * 根据未分配度数和spill代价衡量spill对象的选择
     * @param reg 待spill寄存器
     * @return 代价估计值
     */
    private double costFunction(Register reg) {
        return 0;
    }

    private void practiseSpill(Register reg) {
        StackVar regSaved = stackPool.pop();
    }

    private void spill() {
        double minCost = -1;
        Register goal = null;
        practiseSpill(goal);
    }

    /**
     * 为registers列表中的每个虚拟寄存器分配物理寄存器，同时修改
     * @param instrList 分配前的指令列表
     * @return 分配后的指令列表
     */
    public List<AsmInstruction> allocatePhysicalRegisters(List<AsmInstruction> instrList) {
        instList = instrList;
        getRegisterCost();
        buildGraph();
        return instList;
    }

    @Override
    public List<AsmInstruction> work(List<AsmInstruction> instructionList) {
        List<Register> intRegList = new ArrayList<>(), floatRegList = new ArrayList<>();
        List<Register> intPRegList = new ArrayList<>(), floatPRegList = new ArrayList<>();
        for (var reg : lifeTimeController.getRegKeySet()) {
            if (reg instanceof IntRegister) {
                intRegList.add(reg);
            } else {
                floatRegList.add(reg);
            }
        }
        //图着色分配器可以将非静态寄存器全部分配
        for (int i = 0; i <= 31; i++) {
            Register reg = IntRegister.getPhysical(i);
            if (!Registers.CONSTANT_REGISTERS.contains(reg)) {
                intPRegList.add(reg);
            }
            reg = FloatRegister.getPhysical(i);
            if (!Registers.CONSTANT_REGISTERS.contains(reg)) {
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
