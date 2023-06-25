package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.ImmediateTools;
import cn.edu.bit.newnewcc.backend.asm.util.Pair;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.type.PointerType;
import cn.edu.bit.newnewcc.ir.value.BaseFunction;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * 函数以函数名作为唯一标识符加以区分
 */
public class AsmFunction {
    private final String functionName;

    AsmCode globalCode;
    private final List<AsmOperand> formalParameters = new ArrayList<>();
    private final Map<Value, AsmOperand> formalParameterMap = new HashMap<>();
    private final List<AsmBasicBlock> basicBlocks = new ArrayList<>();
    private final Map<BasicBlock, AsmBasicBlock> basicBlockMap = new HashMap<>();
    private List<AsmInstruction> instructionList = new ArrayList<>();
    private final GlobalTag retBlockTag;
    private final BaseFunction baseFunction;
    private final Register returnRegister;

    public boolean isExternal() {
        return basicBlocks.size() == 0;
    }

    public AsmFunction(BaseFunction baseFunction, AsmCode code) {
        this.globalCode = code;
        int intParameterId = 0, floatParameterId = 0;
        this.functionName = baseFunction.getValueName();
        this.baseFunction = baseFunction;

        {
            Register a0 = new IntRegister("a0");
            Register fa0 = new FloatRegister("fa0");
            this.returnRegister = (baseFunction.getReturnType() instanceof FloatType) ?
                    fa0 : (baseFunction.getReturnType() instanceof IntegerType ? a0 : null);
        }

        retBlockTag = new GlobalTag(functionName + "_ret", false);
        int stackSize = 0;
        for (var parameterType : baseFunction.getParameterTypes()) {
            if (parameterType instanceof IntegerType || parameterType instanceof PointerType) {
                int needSize = parameterType instanceof IntegerType ? 4 : 8;
                if (intParameterId < 8) {
                    formalParameters.add(new IntRegister(String.format("a%d", intParameterId)));
                } else {
                    formalParameters.add(new StackVar(stackSize, needSize, false));
                    stackSize += needSize;
                }
                intParameterId += 1;
            } else if (parameterType instanceof FloatType) {
                if (floatParameterId < 8) {
                    formalParameters.add(new FloatRegister(String.format("fa%d", floatParameterId)));
                } else {
                    formalParameters.add(new StackVar(stackSize, 4, false));
                    stackSize += 4;
                }
                floatParameterId += 1;
            }
        }
    }

    FloatRegister transConstFloat(Float value) {
        var constantFloat = globalCode.getConstFloat(value);
        IntRegister rAddress = registerAllocator.allocateInt();
        FloatRegister tmp = registerAllocator.allocateFloat();
        appendInstruction(new AsmLoad(rAddress, constantFloat.getConstantTag()));
        appendInstruction(new AsmLoad(tmp, new AddressContent(0, rAddress)));
        return tmp;
    }

    public void emitCode() {
        //生成具体函数的汇编代码
        if (baseFunction instanceof Function function) {
            var functionParameterList = function.getFormalParameters();
            for (var i = 0; i < functionParameterList.size(); i++) {
                formalParameterMap.put(functionParameterList.get(i), formalParameters.get(i));
            }

            //获得基本块的bfs序
            Set<BasicBlock> visited = new HashSet<>();
            Queue<BasicBlock> queue = new ArrayDeque<>();
            BasicBlock startBlock = function.getEntryBasicBlock();
            visited.add(startBlock);
            queue.add(startBlock);
            while (!queue.isEmpty()) {
                BasicBlock nowBlock = queue.remove();
                AsmBasicBlock asmBasicBlock = new AsmBasicBlock(this, nowBlock);
                basicBlocks.add(asmBasicBlock);
                basicBlockMap.put(nowBlock, asmBasicBlock);
                for (var nextBlock : nowBlock.getExitBlocks()) {
                    if (!visited.contains(nextBlock)) {
                        visited.add(nextBlock);
                        queue.add(nextBlock);
                    }
                }
            }

            for (var block : basicBlocks) {
                block.emitToFunction();
            }
            asmOptimizerBeforeRegisterAllocate();
            reAllocateStackVar();
            reAllocateRegister();
            asmOptimizerAfterRegisterAllocate();
        }
    }

    public String emit() {
        StringBuilder res = new StringBuilder();
        res.append(".text\n.align 1\n");
        res.append(String.format(".globl %s\n", functionName));
        res.append(String.format(".type %s, @function\n", functionName));
        res.append(String.format("%s:\n", functionName));
        for (var inst : stackAllocator.emitHead()) {
            res.append(inst.emit());
        }
        for (var inst : instructionList) {
            res.append(inst.emit());
        }
        for (var inst : stackAllocator.emitTail()) {
            res.append(inst.emit());
        }
        res.append(String.format(".size %s, .-%s\n", functionName, functionName));
        return res.toString();
    }


    //向函数添加指令的方法
    public void appendInstruction(AsmInstruction instruction) {
        instructionList.add(instruction);
    }

    public int getInstructionListSize() {
        return instructionList.size();
    }

    public void appendAllInstruction(Collection<AsmInstruction> instructions) {
        instructionList.addAll(instructions);
    }

    //函数内部资源分配器
    private final StackAllocator stackAllocator = new StackAllocator();
    private final RegisterAllocator registerAllocator = new RegisterAllocator();
    private final AddressAllocator addressAllocator = new AddressAllocator();
    private final LifeTimeController lifeTimeController = new LifeTimeController(this);

    //资源对应的get方法
    public AsmBasicBlock getBasicBlock(BasicBlock block) {
        return basicBlockMap.get(block);
    }

    public String getFunctionName() {
        return Objects.requireNonNull(functionName);
    }

    public GlobalTag getRetBlockTag() {
        return retBlockTag;
    }

    public AsmCode getGlobalCode() {
        return globalCode;
    }

    public RegisterAllocator getRegisterAllocator() {
        return registerAllocator;
    }

    public StackAllocator getStackAllocator() {
        return stackAllocator;
    }

    public AddressAllocator getAddressAllocator() {
        return addressAllocator;
    }

    public LifeTimeController getLifeTimeController() {
        return lifeTimeController;
    }

    /**
     * 获取一个可以被调用的实际参数
     * @param index 参数的下标
     * @return 参数操作数
     */
    public AsmOperand getParameterByIndex(int index) {
        if (index < 0 || index >= formalParameters.size()) {
            throw new RuntimeException("parameter id error");
        }
        var res = formalParameters.get(index);
        if (res instanceof StackVar stackVar) {
            return stackVar.flip();
        }
        return res;
    }

    public AsmOperand getParameterByFormal(Value formalParameter) {
        var res = formalParameterMap.get(formalParameter);
        if (res instanceof StackVar stackVar) {
            return stackVar.flip();
        }
        return res;
    }

    public int getParameterSize() {
        return formalParameters.size();
    }

    public Register getReturnRegister() {
        return returnRegister;
    }


    //调用另一个函数的汇编代码
    Collection<AsmInstruction> call(AsmFunction calledFunction, List<AsmOperand> parameters, Register returnRegister) {
        stackAllocator.callFunction();
        List<AsmInstruction> res = new ArrayList<>();
        //参数数量不匹配
        if (parameters.size() != calledFunction.formalParameters.size()) {
            throw new RuntimeException("Function parameter mismatch");
        }
        List<Pair<StackVar, Register>> pushList = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            var formalPara = calledFunction.formalParameters.get(i);
            //若形参中使用了参数保存寄存器，则需先push寄存器保存原有内容
            if (formalPara instanceof Register reg) {
                StackVar vreg = stackAllocator.push(8);
                res.add(new AsmStore(reg, vreg));
                pushList.add(new Pair<>(vreg, reg));
            } else {
                stackAllocator.push_back((StackVar)formalPara);
            }

            //将参数存储到形参对应位置
            var para = parameters.get(i);
            if (para instanceof Register reg) {
                res.add(new AsmStore(reg, formalPara));
            } else {
                if (formalPara instanceof FloatRegister freg) {
                    FloatRegister ftmp = registerAllocator.allocateFloat();
                    res.add(new AsmLoad(ftmp, para));
                    res.add(new AsmStore(ftmp, freg));
                } else {
                    IntRegister tmp = registerAllocator.allocateInt();
                    res.add(new AsmLoad(tmp, para));
                    res.add(new AsmStore(tmp, formalPara));
                }
            }
        }
        res.add(new AsmCall(calledFunction.getFunctionName()));
        if (returnRegister != null) {
            res.add(new AsmLoad(returnRegister, calledFunction.getReturnRegister()));
        }
        //执行完成后恢复寄存器现场
        for (var p : pushList) {
            res.add(new AsmLoad(p.b, p.a));
            stackAllocator.pop(8);
        }
        return res;
    }

    /**
     * 重新分配栈偏移量的过程，避免offset超出11位导致汇编错误
     */
    private void reAllocateStackVar() {
        List<AsmInstruction> newInstructionList = new ArrayList<>();
        for (var inst : instructionList) {
            for (int j = 1; j <= 3; j++) {
                AsmOperand operand = inst.getOperand(j);
                if (operand instanceof StackVar stackVar) {
                    Address stackAddress = stackVar.getAddress();
                    ExStackVarOffset offset = ExStackVarOffset.transform(stackVar, stackAddress.getOffset());
                    IntRegister tmp = registerAllocator.allocateInt();
                    newInstructionList.add(new AsmLoad(tmp, offset));
                    newInstructionList.add(new AsmAdd(tmp, tmp, stackAddress.getRegister()));
                    Address now = new AddressContent(0, tmp);
                    inst.replaceOperand(j, ExStackVarContent.transform(stackVar, now));
                }
            }
            newInstructionList.add(inst);
        }
        this.instructionList = newInstructionList;
    }


    //未分配寄存器的分配方法
    private void reAllocateRegister() {
        lifeTimeController.refreshAllVreg(instructionList);

        RegisterControl registerController = new RegisterControl(this, stackAllocator);
        registerController.linearScanRegAllocate(instructionList);
        var newInstructionList = registerController.spillRegisters(instructionList);

        for (var inst : newInstructionList) {
            for (int j = 1; j <= 3; j++) {
                AsmOperand operand = inst.getOperand(j);
                if (operand instanceof ExStackVarAdd operandAdd) {
                    inst.replaceOperand(j, operandAdd.add(-stackAllocator.getExSize()));
                }
            }
        }
        this.instructionList = registerController.emitHead();
        this.instructionList.addAll(newInstructionList);
        this.instructionList.add(new AsmTag(retBlockTag));
        this.instructionList.addAll(registerController.emitTail());
    }

    //寄存器分配前的优化器，用于合并重复虚拟寄存器
    private void asmOptimizerBeforeRegisterAllocate() {
        class DSU {
            final Map<Integer, Integer> fa = new HashMap<>();
            public DSU() {}
            public int getfa(int v) {
                if (!fa.containsKey(v)) {
                    fa.put(v, v);
                }
                if (fa.get(v) == v) {
                    return v;
                }
                return getfa(fa.get(v));
            }
            public void merge(int u, int v) {
                u = getfa(u);
                v = getfa(v);
                if (u != v) {
                    fa.put(u, v);
                }
            }
        }
        DSU dsu = new DSU();
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
        List<AsmInstruction> newInstructionList = new ArrayList<>();
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
        instructionList = newInstructionList;
    }

    private void asmOptimizerAfterRegisterAllocate() {
        List<AsmInstruction> newInstructionList = new ArrayList<>();
        for (int i = 0; i < instructionList.size(); i++) {
            //这个部分是将额外生成的栈空间地址重新转换为普通寻址的过程，必须首先进行该优化
            if (i + 2 < instructionList.size()) {
                var iLi = instructionList.get(i);
                var iAdd = instructionList.get(i + 1);
                var iMov = instructionList.get(i + 2);
                if (iLi instanceof AsmLoad && iLi.getOperand(2) instanceof Immediate offset) {
                    int offsetVal = offset.getValue();
                    if (!ImmediateTools.bitlengthNotInLimit(offsetVal)) {
                        if (iAdd instanceof AsmAdd && iAdd.getOperand(3) instanceof IntRegister baseRegister && baseRegister.isS0()) {
                            if (iMov instanceof AsmLoad iLoad && iLoad.getOperand(2) instanceof StackVar stackVar) {
                                if (stackVar.getRegister() == iLi.getOperand(1) && stackVar.getAddress().getOffset() == 0) {
                                    StackVar now = new StackVar(offsetVal, stackVar.getSize(), true);
                                    newInstructionList.add(new AsmLoad((Register) iLoad.getOperand(1), now));
                                    i += 2;
                                    continue;
                                }
                            } else if (iMov instanceof AsmStore iStore && iStore.getOperand(2) instanceof StackVar stackVar) {
                                if (stackVar.getRegister() == iLi.getOperand(1) && stackVar.getAddress().getOffset() == 0) {
                                    StackVar now = new StackVar(offsetVal, stackVar.getSize(), true);
                                    newInstructionList.add(new AsmStore((Register) iStore.getOperand(1), now));
                                    i += 2;
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
            newInstructionList.add(instructionList.get(i));
        }
        instructionList = newInstructionList;
    }

}
