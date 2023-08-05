package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.allocator.AddressAllocator;
import cn.edu.bit.newnewcc.backend.asm.allocator.RegisterAllocator;
import cn.edu.bit.newnewcc.backend.asm.allocator.StackAllocator;
import cn.edu.bit.newnewcc.backend.asm.controller.LifeTimeController;
import cn.edu.bit.newnewcc.backend.asm.controller.LinearScanRegisterControl;
import cn.edu.bit.newnewcc.backend.asm.controller.RegisterControl;
import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.BackendOptimizer;
import cn.edu.bit.newnewcc.backend.asm.util.Others;
import cn.edu.bit.newnewcc.backend.asm.util.Pair;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.type.PointerType;
import cn.edu.bit.newnewcc.ir.value.BaseFunction;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;

import java.util.*;
import java.util.function.Consumer;

/**
 * 函数以函数名作为唯一标识符加以区分
 */
public class AsmFunction {
    private final String functionName;

    private final boolean DEBUG_MODE = false;
    private final boolean ASSERT_MODE = true;

    AsmCode globalCode;
    private final List<AsmOperand> formalParameters = new ArrayList<>();
    private final Map<Value, AsmOperand> formalParameterMap = new HashMap<>();
    private final List<AsmBasicBlock> basicBlocks = new ArrayList<>();
    private final Map<BasicBlock, AsmBasicBlock> basicBlockMap = new HashMap<>();
    private final Map<AsmBasicBlock, AsmTag> blockAsmTagMap = new HashMap<>();
    private final Map<AsmTag, AsmBasicBlock> asmTagBlockMap = new HashMap<>();
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
            Register a0 = IntRegister.getParameter(0);
            Register fa0 = FloatRegister.getParameter(0);
            this.returnRegister = (baseFunction.getReturnType() instanceof FloatType) ?
                    fa0 : (baseFunction.getReturnType() instanceof IntegerType ? a0 : null);
        }

        retBlockTag = new GlobalTag(functionName + "_ret", false);
        int stackSize = 0;
        for (var parameterType : baseFunction.getParameterTypes()) {
            if (parameterType instanceof IntegerType || parameterType instanceof PointerType) {
                int needSize = parameterType instanceof IntegerType ? 4 : 8;
                if (intParameterId < 8) {
                    formalParameters.add(IntRegister.getParameter(intParameterId));
                } else {
                    formalParameters.add(new StackVar(stackSize, needSize, false));
                    stackSize += needSize;
                }
                intParameterId += 1;
            } else if (parameterType instanceof FloatType) {
                if (floatParameterId < 8) {
                    formalParameters.add(FloatRegister.getParameter(floatParameterId));
                } else {
                    formalParameters.add(new StackVar(stackSize, 4, false));
                    stackSize += 4;
                }
                floatParameterId += 1;
            }
        }
    }

    FloatRegister transConstFloat(Float value, Consumer<AsmInstruction> appendInstruction) {
        var constantFloat = globalCode.getConstFloat(value);
        IntRegister rAddress = registerAllocator.allocateInt();
        FloatRegister tmp = registerAllocator.allocateFloat();
        appendInstruction.accept(new AsmLoad(rAddress, constantFloat.getConstantTag()));
        appendInstruction.accept(new AsmLoad(tmp, new AddressContent(0, rAddress)));
        return tmp;
    }
    IntRegister transConstLong(long value, Consumer<AsmInstruction> appendInstruction) {
        var constantLong = globalCode.getConstLong(value);
        IntRegister rAddress = registerAllocator.allocateInt();
        IntRegister tmp = registerAllocator.allocateInt();
        appendInstruction.accept(new AsmLoad(rAddress, constantLong.getConstantTag()));
        appendInstruction.accept(new AsmLoad(tmp, new AddressContent(0, rAddress), 64));
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
                block.preTranslatePhiInstructions();
            }
            for (var block : basicBlocks) {
                block.emitToFunction();
            }
            if (ASSERT_MODE) {
                Others.check(this.instructionList);
            }
            if (!DEBUG_MODE) {
                lifeTimeController.getAllVRegLifeTime(instructionList);
                asmOptimizerBeforeRegisterAllocate(lifeTimeController);
                PeepholeOptimizer.runBeforeAllocation(instructionList);
                reAllocateRegister();
                asmOptimizerAfterRegisterAllocate();
                PeepholeOptimizer.runAfterAllocation(instructionList);
            }
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
        int id = 0;
        for (var inst : instructionList) {
            if (DEBUG_MODE) {
                res.append(String.format("%d: %s", id, inst.emit()));
            } else {
                res.append(inst.emit());
            }
            id += 1;
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
    private final LifeTimeController lifeTimeController = new LifeTimeController();

    //资源对应的get方法
    public AsmBasicBlock getBasicBlock(BasicBlock block) {
        return basicBlockMap.get(block);
    }

    public AsmTag getBlockAsmTag(AsmBasicBlock block) {
        return blockAsmTagMap.get(block);
    }

    public void putBlockAsmTag(AsmBasicBlock block, AsmTag tag) {
        blockAsmTagMap.put(block, tag);
        asmTagBlockMap.put(tag, block);
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

    public AsmOperand getParameterByFormal(Value formalParameter) {
        var res = formalParameterMap.get(formalParameter);
        if (res instanceof StackVar stackVar) {
            return transformStackVar(stackVar.flip());
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
        Map<String, StackVar> paraSaved = new HashMap<>();

        //若形参中使用了参数保存寄存器，则需先push寄存器保存原有内容，避免受到下一层影响
        for (AsmOperand formalParameter : formalParameters) {
            if (formalParameter instanceof Register reg) {
                StackVar vreg = stackAllocator.push(8);
                res.add(new AsmStore(reg, transformStackVar(vreg, res)));
                paraSaved.put(reg.emit(), vreg);
                pushList.add(new Pair<>(vreg, reg));
            }
        }

        for (int i = 0; i < parameters.size(); i++) {
            var formalPara = calledFunction.formalParameters.get(i);
            if (formalPara instanceof StackVar stackVar) {
                formalPara = transformStackVar(stackVar, res);
            }

            //为下一层的函数参数存储开辟栈空间
            if (!(formalPara instanceof Register)) {
                if (!(formalPara instanceof ExStackVarContent)) {
                    throw new RuntimeException("formal parameter in wrong place");
                }
                stackAllocator.push_back((StackVar)formalPara);
            }

            //将参数存储到形参对应位置
            var para = parameters.get(i);
            if (para instanceof Register reg) {
                //若当前参数已经被覆盖，则从栈中读取
                if (paraSaved.containsKey(reg.emit()) && reg.emit().compareTo(formalPara.emit()) < 0) {
                    var rs = transformStackVar(paraSaved.get(reg.emit()), res);
                    if (formalPara instanceof Register formalReg) {
                        res.add(new AsmLoad(formalReg, rs));
                    } else {
                        var tmp = registerAllocator.allocate(reg);
                        res.add(new AsmLoad(tmp, rs));
                        res.add(new AsmStore(tmp, formalPara));
                    }
                } else {
                    res.add(new AsmStore(reg, formalPara));
                }
            } else {
                if (formalPara instanceof Register formalReg) {
                    res.add(new AsmLoad(formalReg, para));
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
            res.add(new AsmLoad(p.b, transformStackVar(p.a, res)));
            stackAllocator.pop(8);
        }
        return res;
    }

    public ExStackVarContent transformStackVar(StackVar stackVar, List<AsmInstruction> newInstructionList) {
        Address stackAddress = stackVar.getAddress();
        ExStackVarOffset offset = ExStackVarOffset.transform(stackVar, stackAddress.getOffset());
        IntRegister tmp = registerAllocator.allocateInt();
        newInstructionList.add(new AsmLoad(tmp, offset));
        IntRegister t2 = registerAllocator.allocateInt();
        newInstructionList.add(new AsmAdd(t2, tmp, stackAddress.getRegister()));
        Address now = new AddressContent(0, t2);
        return ExStackVarContent.transform(stackVar, now);
    }

    public ExStackVarContent transformStackVar(StackVar stackVar) {
        return transformStackVar(stackVar, instructionList);
    }

    //未分配寄存器的分配方法

    private void reAllocateRegister() {
        RegisterControl registerController = new LinearScanRegisterControl(this, stackAllocator);
        registerController.virtualRegAllocateToPhysics();
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

    //寄存器分配前的优化器，用于合并copy后的权值
    private void asmOptimizerBeforeRegisterAllocate(LifeTimeController lifeTimeController) {
        instructionList = BackendOptimizer.beforeAllocateScanForward(instructionList, lifeTimeController);
    }


    private void asmOptimizerAfterRegisterAllocate() {
        LinkedList<AsmInstruction> linkedInstructionList = new LinkedList<>(instructionList);
        linkedInstructionList = BackendOptimizer.afterAllocateScanForward(linkedInstructionList);
        linkedInstructionList = BackendOptimizer.afterAllocateScanBackward(linkedInstructionList);
        instructionList = new ArrayList<>(linkedInstructionList);
    }

}
