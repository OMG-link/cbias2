package cn.edu.bit.newnewcc.backend.asm.controller;

import cn.edu.bit.newnewcc.backend.asm.AsmFunction;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.operand.IntRegister;
import cn.edu.bit.newnewcc.backend.asm.operand.Register;
import cn.edu.bit.newnewcc.backend.asm.util.Registers;

import java.util.*;

public class GraphColoringRegisterControl extends RegisterControl {
    public GraphColoringRegisterControl(AsmFunction function, StackAllocator allocator) {
        super(function, allocator);
    }

    private List<Register> registers, physicRegisters;
    private final List<LifeTimeInterval> intervals = new ArrayList<>();
    private final Map<Integer, Set<Integer>> edges = new HashMap<>();
    private final Map<Register, Register> physicRegisterMap = new HashMap<>();
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
        return false;
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
            var reg = function.getRegisterAllocator().get(x);
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
        instructionList = allocatePhysicalRegisters(instructionList);
        registers = floatRegList;
        instructionList = allocatePhysicalRegisters(instructionList);
        return instructionList;
    }
}
