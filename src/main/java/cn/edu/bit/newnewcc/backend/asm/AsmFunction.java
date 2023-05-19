package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.FloatType;
import cn.edu.bit.newnewcc.ir.type.IntegerType;
import cn.edu.bit.newnewcc.ir.value.BaseFunction;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.instruction.BinaryInstruction;
import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

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
        for (var parameterType : baseFunction.getParameterTypes()) {
            if (parameterType instanceof IntegerType) {
                if (intParameterId < 8) {
                    formalParameters.add(new IntRegister(String.format("a%d", intParameterId)));
                } else {
                    formalParameters.add(new StackVar((intParameterId - 8) * 4, 4, false));
                }
                intParameterId += 1;
            } else if (parameterType instanceof FloatType) {
                if (floatParameterId < 8) {
                    formalParameters.add(new FloatRegister(String.format("fa%d", intParameterId)));
                } else {
                    formalParameters.add(new StackVar((floatParameterId - 8) * 4, 4, false));
                }
                floatParameterId += 1;
            }
        }
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
            reAllocateRegister();
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
        res.append((new AsmTag(retBlockTag)).emit());
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

    public void appendAllInstruction(Collection<AsmInstruction> instructions) {
        instructionList.addAll(instructions);
    }


    //函数内部资源分配器
    private final StackAllocator stackAllocator = new StackAllocator();
    private final RegisterAllocator registerAllocator = new RegisterAllocator();

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
    Collection<AsmInstruction> call(AsmFunction calledFunction, List<AsmOperand> parameters) {
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

            //将参数存储到形参对应为止
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
        //执行完成后恢复寄存器现场
        for (var p : pushList) {
            res.add(new AsmLoad(p.b, p.a));
            stackAllocator.pop(8);
        }
        return res;
    }

    //未分配寄存器的分配方法
    private void reAllocateRegister() {
        stackAllocator.set_top();
        Map<Integer, Integer> lastLifeTime = new HashMap<>();
        for (int i = 0; i < instructionList.size(); i++) {
            AsmInstruction instruction = instructionList.get(i);
            for (int j = 1; j <= 3; j++) {
                AsmOperand op = instruction.getOperand(j);
                if (op instanceof Register register) {
                    if (register.isVirtual()) {
                        Integer index = register.getIndex();
                        lastLifeTime.put(index, i);
                    }
                }
            }
        }
        List<AsmInstruction> newInstructionList = new ArrayList<>();

        Map<Integer, AsmOperand> vregLocation = new HashMap<>();
        Map<Register, Integer> registerPool = new HashMap<>();
        Queue<StackVar> stackPool = new ArrayDeque<>();
        for (int i = 0; i <= 31; i++) {
            if ((6 <= i && i <= 7) || (28 <= i)) {
                registerPool.put(new IntRegister(i), 0);
            }
        }
        for (AsmInstruction instruction : instructionList) {
            Set<Integer> nowUsed = new HashSet<>();
            for (int j = 1; j <= 3; j++) {
                AsmOperand op = instruction.getOperand(j);
                if (op instanceof Register register && register.isVirtual()) {
                    Integer index = register.getIndex();
                    nowUsed.add(index);
                }
            }
            for (int j = 1; j <= 3; j++) {
                AsmOperand op = instruction.getOperand(j);
                if (op instanceof Register register && register.isVirtual()) {
                    int d = register.getIndex();
                    if (!vregLocation.containsKey(d)) {
                        for (var reg : registerPool.keySet()) {
                            if (registerPool.get(reg) == 0) {
                                registerPool.put(reg, d);
                                vregLocation.put(d, reg);
                                break;
                            }
                        }
                        if (!vregLocation.containsKey(d)) {
                            for (var reg : registerPool.keySet()) {
                                if (!nowUsed.contains(registerPool.get(reg))) {
                                    if (stackPool.isEmpty()) {
                                        stackPool.add(stackAllocator.push(8));
                                    }
                                    StackVar tmp = stackPool.remove();
                                    newInstructionList.add(new AsmStore(reg, tmp));
                                    vregLocation.put(registerPool.get(reg), tmp);
                                    vregLocation.put(d, reg);
                                    registerPool.put(reg, d);
                                    break;
                                }
                            }
                        }
                        //此处待优化
                    }
                    if (vregLocation.get(d) instanceof StackVar) {
                        for (var reg : registerPool.keySet()) {
                            if (registerPool.get(reg) == 0) {
                                registerPool.put(reg, d);
                                vregLocation.put(d, reg);
                                break;
                            }
                        }
                        if (vregLocation.get(d) instanceof StackVar) {
                            for (var reg : registerPool.keySet()) {
                                if (!nowUsed.contains(registerPool.get(reg))) {
                                    if (stackPool.isEmpty()) {
                                        stackPool.add(stackAllocator.push(8));
                                    }
                                    StackVar tmp = stackPool.remove();
                                    newInstructionList.add(new AsmStore(reg, tmp));
                                    vregLocation.put(registerPool.get(reg), tmp);
                                    vregLocation.put(d, reg);
                                    registerPool.put(reg, d);
                                    break;
                                }
                            }
                        }
                    }
                    if (vregLocation.get(d) instanceof Register vregnow) {
                        register.setIndex(vregnow.getIndex());
                    } else {
                        throw new RuntimeException("not enough register");
                    }
                }
            }
            newInstructionList.add(instruction);
            // 此处待优化部分：二元运算符的目标寄存器
        }
        this.instructionList = newInstructionList;
    }

}
