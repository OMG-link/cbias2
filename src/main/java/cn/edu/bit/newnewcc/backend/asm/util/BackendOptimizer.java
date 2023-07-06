package cn.edu.bit.newnewcc.backend.asm.util;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.*;

import java.util.*;
import java.util.function.Consumer;

public class BackendOptimizer {
    public static ArrayList<AsmInstruction> beforeAllocateScanForward(ArrayList<AsmInstruction> instructionList) {
        DSU<Integer> dsu = new DSU<>();
        for (AsmInstruction iMov : instructionList) {
            if (iMov instanceof AsmLoad || iMov instanceof AsmStore) {
                if (iMov.getOperand(1) instanceof Register r1 && iMov.getOperand(2) instanceof Register r2) {
                    if (r1.isVirtual() && r2.isVirtual() && r1.getType() == r2.getType()) {
                        dsu.merge(r1.getIndex(), r2.getIndex());
                    }
                }
            }
        }
        for (AsmInstruction inst : instructionList) {
            for (int j = 1; j <= 3; j++) {
                var op = inst.getOperand(j);
                if (op instanceof RegisterReplaceable registerReplaceable) {
                    var reg = registerReplaceable.getRegister();
                    if (reg.isVirtual()) {
                        var replaceReg = reg.replaceIndex(dsu.getfa(reg.getIndex()));
                        inst.replaceOperand(j, registerReplaceable.replaceRegister(replaceReg));
                    }
                }
            }
        }
        ArrayList<AsmInstruction> newInstructionList = new ArrayList<>();
        for (AsmInstruction iMov : instructionList) {
            if (iMov instanceof AsmLoad || iMov instanceof AsmStore) {
                if (iMov.getOperand(1) instanceof Register r1 && iMov.getOperand(2) instanceof Register r2) {
                    if (r1.getIndex() == r2.getIndex()) {
                        continue;
                    }
                }
            }
            newInstructionList.add(iMov);
        }
        return newInstructionList;
    }
    public static LinkedList<AsmInstruction> afterAllocateScanForward(LinkedList<AsmInstruction> oldInstructionList) {
        LinkedList<AsmInstruction> newInstructionList = new LinkedList<>();
        while (oldInstructionList.size() > 0) {
            Consumer<Integer> popx = (Integer x) -> {
                for (int j = 0; j < x; j++) {
                    oldInstructionList.removeFirst();
                }
            };
            Consumer<Integer> backward = (Integer x) -> {
                for (int j = 0; j < x && j < newInstructionList.size(); j++) {
                    oldInstructionList.addFirst(newInstructionList.removeLast());
                }
            };
            //这个部分是将额外生成的栈空间地址重新转换为普通寻址的过程，必须首先进行该优化
            if (oldInstructionList.size() > 1) {
                {
                    var iSv = oldInstructionList.get(0);
                    var iLd = oldInstructionList.get(1);
                    if (iSv instanceof AsmStore && iLd instanceof AsmLoad) {
                        var iSvOp2 = iSv.getOperand(2);
                        var iLdOp2 = iLd.getOperand(2);
                        if (!(iSvOp2 instanceof ExStackVarContent) && iSvOp2.equals(iLdOp2)) {
                            newInstructionList.addLast(oldInstructionList.removeFirst());
                            popx.accept(1);
                            Register source = (Register) iSv.getOperand(1);
                            Register destination = (Register) iLd.getOperand(1);
                            if (!source.equals(destination)) {
                                oldInstructionList.addFirst(new AsmLoad(destination, source));
                            }
                            backward.accept(1);
                            continue;
                        }
                    }
                }
                if (oldInstructionList.size() > 2) {
                    var iLi = oldInstructionList.get(0);
                    var iAdd = oldInstructionList.get(1);
                    var iMov = oldInstructionList.get(2);
                    if (iLi instanceof AsmLoad && iLi.getOperand(2) instanceof Immediate offset) {
                        int offsetVal = offset.getValue();
                        if (!ImmediateTools.bitlengthNotInLimit(offsetVal)) {
                            if (iAdd instanceof AsmAdd && iAdd.getOperand(3) instanceof IntRegister baseRegister && baseRegister.isS0()) {
                                if (iMov instanceof AsmLoad iLoad && iLoad.getOperand(2) instanceof StackVar stackVar) {
                                    if (stackVar.getRegister() == iLi.getOperand(1) && stackVar.getAddress().getOffset() == 0) {
                                        StackVar now = new StackVar(offsetVal, stackVar.getSize(), true);
                                        popx.accept(3);
                                        oldInstructionList.addFirst(new AsmLoad((Register) iLoad.getOperand(1), now));
                                        backward.accept(1);
                                        continue;
                                    }
                                } else if (iMov instanceof AsmStore iStore && iStore.getOperand(2) instanceof StackVar stackVar) {
                                    if (stackVar.getRegister() == iLi.getOperand(1) && stackVar.getAddress().getOffset() == 0) {
                                        StackVar now = new StackVar(offsetVal, stackVar.getSize(), true);
                                        popx.accept(3);
                                        oldInstructionList.addFirst(new AsmStore((Register) iStore.getOperand(1), now));
                                        backward.accept(1);
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            newInstructionList.addLast(oldInstructionList.removeFirst());
        }
        return newInstructionList;
    }

    public static LinkedList<AsmInstruction> afterAllocateScanBackward(LinkedList<AsmInstruction> oldInstructionList) {
        LinkedList<AsmInstruction> newInstructionList = new LinkedList<>();
        Set<String> lastWrite = new HashSet<>();
        while (oldInstructionList.size() > 0) {
            var iMov = oldInstructionList.removeLast();
            var address = iMov.getOperand(2);
            if (iMov instanceof AsmTag || iMov instanceof AsmPhiTag) {
                lastWrite.clear();
            }
            if (address instanceof StackVar && !(address instanceof ExStackVarContent)) {
                if (iMov instanceof AsmLoad) {
                    lastWrite.remove(address.toString());
                } else if (iMov instanceof AsmStore) {
                    if (lastWrite.contains(address.toString())) {
                        continue;
                    }
                    lastWrite.add(address.toString());
                }
            }
            newInstructionList.addFirst(iMov);
        }
        return newInstructionList;
    }
}
