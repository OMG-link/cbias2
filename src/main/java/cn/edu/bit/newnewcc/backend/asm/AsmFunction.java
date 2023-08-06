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

    private final AsmCode globalCode;
    private final List<AsmOperand> formalParameters = new ArrayList<>();
    private final Map<Value, AsmOperand> formalParameterMap = new HashMap<>();
    private final List<AsmBasicBlock> basicBlocks = new ArrayList<>();
    private final Map<BasicBlock, AsmBasicBlock> basicBlockMap = new HashMap<>();
    private List<AsmInstruction> instrList = new ArrayList<>();
    private final Label retBlockLabel;
    private final BaseFunction baseFunction;
    private final Register returnRegister;

    public boolean isExternal() {
        return basicBlocks.isEmpty();
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

        retBlockLabel = new Label(functionName + "_ret", false);
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

    public FloatRegister transConstFloat(Float value, Consumer<AsmInstruction> appendInstruction) {
        var constantFloat = globalCode.getConstFloat(value);
        IntRegister rAddress = registerAllocator.allocateInt();
        FloatRegister tmp = registerAllocator.allocateFloat();
        appendInstruction.accept(new AsmLoad(rAddress, constantFloat.getConstantLabel()));
        appendInstruction.accept(new AsmLoad(tmp, new AddressContent(0, rAddress), 32));
        return tmp;
    }
    public IntRegister transConstLong(long value, Consumer<AsmInstruction> appendInstruction) {
        var constantLong = globalCode.getConstLong(value);
        IntRegister rAddress = registerAllocator.allocateInt();
        IntRegister tmp = registerAllocator.allocateInt();
        appendInstruction.accept(new AsmLoad(rAddress, constantLong.getConstantLabel()));
        appendInstruction.accept(new AsmLoad(tmp, new AddressContent(0, rAddress), 64));
        return tmp;
    }


    public void emitCode() {
        //生成具体函数的汇编代码
        //emitParameterHead();
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
            for (var inst : instrList) {
                for (var x : AsmInstructions.getReadVRegSet(inst)) {
                    for (var y : AsmInstructions.getWriteVRegSet(inst)) {
                        if (x.equals(y)) {
                            throw new AssertionError();
                        }
                    }
                }
            }
            lifeTimeController.getAllVRegLifeTime(instrList);
            asmOptimizerBeforeRegisterAllocate(lifeTimeController);
            PeepholeOptimizer.runBeforeAllocation(instrList);
            reAllocateRegister();
            asmOptimizerAfterRegisterAllocate();
            PeepholeOptimizer.runAfterAllocation(instrList);
        }
    }

    public String emit() {
        StringBuilder builder = new StringBuilder();
        builder.append(".text\n.align 1\n");
        builder.append(String.format(".globl %s\n", functionName));
        builder.append(String.format(".type %s, @function\n", functionName));
        builder.append(String.format("%s:\n", functionName));
        for (var inst : stackAllocator.emitPrologue()) {
            builder.append(inst.emit());
        }
        for (var inst : instrList) {
            builder.append(inst.emit());
        }
        for (var inst : stackAllocator.emitEpilogue()) {
            builder.append(inst.emit());
        }
        builder.append(String.format(".size %s, .-%s\n", functionName, functionName));
        return builder.toString();
    }


    //向函数添加指令的方法
    public void appendInstruction(AsmInstruction instruction) {
        instrList.add(instruction);
    }

    public void appendAllInstruction(Collection<AsmInstruction> instructions) {
        instrList.addAll(instructions);
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

    public String getFunctionName() {
        return Objects.requireNonNull(functionName);
    }

    public Label getRetBlockLabel() {
        return retBlockLabel;
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
        var result = formalParameterMap.get(formalParameter);
        if (result instanceof StackVar stackVar) {
            return transformStackVar(stackVar.flip());
        }
        return result;
    }

    public int getParameterSize() {
        return formalParameters.size();
    }

    public Register getReturnRegister() {
        return returnRegister;
    }


    //调用另一个函数的汇编代码
    public Collection<AsmInstruction> call(AsmFunction calledFunction, List<AsmOperand> parameters, Register returnRegister) {
        stackAllocator.callFunction();
        List<AsmInstruction> instrList = new ArrayList<>();
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
                instrList.add(new AsmStore(reg, transformStackVar(vreg, instrList)));
                paraSaved.put(reg.emit(), vreg);
                pushList.add(new Pair<>(vreg, reg));
            }
        }

        for (int i = 0; i < parameters.size(); i++) {
            var formalParam = calledFunction.formalParameters.get(i);
            if (formalParam instanceof StackVar stackVar) {
                formalParam = transformStackVar(stackVar, instrList);
            }

            //为下一层的函数参数存储开辟栈空间
            if (!(formalParam instanceof Register)) {
                if (!(formalParam instanceof ExStackVarContent)) {
                    throw new RuntimeException("formal parameter in wrong place");
                }
                stackAllocator.pushBack((StackVar)formalParam);
            }

            //将参数存储到形参对应位置
            var para = parameters.get(i);
            if (para instanceof Register reg) {
                //若当前参数已经被覆盖，则从栈中读取
                if (paraSaved.containsKey(reg.emit()) && reg.emit().compareTo(formalParam.emit()) < 0) {
                    var rs = transformStackVar(paraSaved.get(reg.emit()), instrList);
                    if (formalParam instanceof Register formalReg) {
                        instrList.add(new AsmLoad(formalReg, rs));
                    } else {
                        var tmp = registerAllocator.allocate(reg);
                        instrList.add(new AsmLoad(tmp, rs));
                        instrList.add(new AsmStore(tmp, (StackVar) formalParam));
                    }
                } else {
                    if (formalParam instanceof Register)
                        instrList.add(new AsmLoad((Register) formalParam, reg));
                    else
                        instrList.add(new AsmStore(reg, (StackVar) formalParam));
                }
            } else {
                if (formalParam instanceof Register formalReg) {
                    instrList.add(new AsmLoad(formalReg, para));
                } else {
                    IntRegister tmp = registerAllocator.allocateInt();
                    instrList.add(new AsmLoad(tmp, para));
                    instrList.add(new AsmStore(tmp, (StackVar) formalParam));
                }
            }
        }
        instrList.add(new AsmCall(new Label(calledFunction.getFunctionName(), true)));
        if (returnRegister != null) {
            instrList.add(new AsmLoad(returnRegister, calledFunction.getReturnRegister()));
        }
        //执行完成后恢复寄存器现场
        for (var p : pushList) {
            instrList.add(new AsmLoad(p.b, transformStackVar(p.a, instrList)));
            stackAllocator.pop(8);
        }
        return instrList;
    }

    public ExStackVarContent transformStackVar(StackVar stackVar, List<AsmInstruction> newInstructionList) {
        Address stackAddress = stackVar.getAddress();
        ExStackVarOffset offset = ExStackVarOffset.transform(stackAddress.getOffset());
        IntRegister tmp = registerAllocator.allocateInt();
        newInstructionList.add(new AsmLoad(tmp, offset));
        IntRegister t2 = registerAllocator.allocateInt();
        newInstructionList.add(new AsmAdd(t2, tmp, stackAddress.getRegister(), 64));
        Address now = new AddressContent(0, t2);
        return ExStackVarContent.transform(stackVar, now);
    }

    public ExStackVarContent transformStackVar(StackVar stackVar) {
        return transformStackVar(stackVar, instrList);
    }

    //未分配寄存器的分配方法

    private void reAllocateRegister() {
        RegisterControl registerController = new LinearScanRegisterControl(this, stackAllocator);
        registerController.virtualRegAllocateToPhysics();
        var newInstructionList = registerController.spillRegisters(instrList);

        for (var inst : newInstructionList) {
            for (int j = 1; j <= 3; j++) {
                AsmOperand operand = inst.getOperand(j);
                if (operand instanceof ExStackVarAdd operandAdd) {
                    inst.replaceOperand(j, operandAdd.add(-stackAllocator.getExSize()));
                }
            }
        }
        instrList = new ArrayList<>();
        instrList.addAll(registerController.emitPrologue());
        instrList.addAll(newInstructionList);
        instrList.add(new AsmLabel(retBlockLabel));
        instrList.addAll(registerController.emitEpilogue());
    }

    //寄存器分配前的优化器，用于合并copy后的权值
    private void asmOptimizerBeforeRegisterAllocate(LifeTimeController lifeTimeController) {
        instrList = BackendOptimizer.beforeAllocateScanForward(instrList, lifeTimeController);
    }


    private void asmOptimizerAfterRegisterAllocate() {
        LinkedList<AsmInstruction> linkedInstructionList = new LinkedList<>(instrList);
        linkedInstructionList = BackendOptimizer.afterAllocateScanForward(linkedInstructionList);
        linkedInstructionList = BackendOptimizer.afterAllocateScanBackward(linkedInstructionList);
        instrList = new ArrayList<>(linkedInstructionList);
    }

}
