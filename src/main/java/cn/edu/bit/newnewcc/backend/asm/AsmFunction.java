package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.*;
import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.max;

/**
 * 函数以函数名作为唯一标识符加以区分
 */
public class AsmFunction {
    private String functionName;
    AsmCode globalCode;
    private final List<AsmOperand> formalParameters = new ArrayList<>();
    private final List<AsmBasicBlock> basicBlocks = new ArrayList<>();
    private final StackAllocator stackAllocator = new StackAllocator();
    private final RegisterAllocator registerAllocator = new RegisterAllocator();
    private final FloatRegisterAllocator floatRegisterAllocator = new FloatRegisterAllocator();

    public AsmCode getGlobalCode() {
        return globalCode;
    }

    public RegisterAllocator getRegisterAllocator() {
        return registerAllocator;
    }

    public FloatRegisterAllocator getFloatRegisterAllocator() {
        return floatRegisterAllocator;
    }

    List<AsmInstruction> call(AsmFunction calledFunction, List<AsmOperand> parameters) {
        stackAllocator.callFunction(this);
        List<AsmInstruction> res = new ArrayList<>();
        //参数数量不匹配
        if (parameters.size() != calledFunction.formalParameters.size()) {
            throw new RuntimeException("call function parameter not match");
        }
        List<Pair<StackVar, Register>> pushList = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            var fpara = calledFunction.formalParameters.get(i);
            //若形参中使用了参数保存寄存器，则需先push寄存器保存原有内容
            if (fpara instanceof Register reg) {
                StackVar vreg = stackAllocator.push(8);
                res.add(new AsmStore(reg, vreg));
                pushList.add(new Pair<>(vreg, reg));
            }

            //将参数存储到形参对应为止
            var para = parameters.get(i);
            if (para instanceof Register reg) {
                res.add(new AsmStore(reg, fpara));
            } else {
                if (fpara instanceof FloatRegister freg) {
                    FloatRegister ftmp = floatRegisterAllocator.allocate();
                    res.add(new AsmLoad(ftmp, para));
                    res.add(new AsmStore(ftmp, freg));
                } else {
                    IntRegister tmp = registerAllocator.allocate();
                    res.add(new AsmLoad(tmp, para));
                    res.add(new AsmStore(tmp, fpara));
                }
            }
        }
        res.add(new AsmCall(calledFunction.getFunctionName()));
        //执行完成后恢复寄存器现场
        for (var p : pushList) {
            res.add(new AsmLoad(p.b, p.a));
        }
        return res;
    }

    String getFunctionName() {
        if (functionName == null) {
            throw new RuntimeException("function name is null");
        }
        return functionName;
    }

    public class RegisterAllocator {
        Map<Instruction, IntRegister> registerMap;
        int total;
        RegisterAllocator() {
            total = 0;
            registerMap = new HashMap<>();
        }
        IntRegister allocate(Instruction instruction) {
            total -= 1;
            IntRegister reg = new IntRegister(total);
            registerMap.put(instruction, reg);
            return reg;
        }
        IntRegister allocate() {
            total -= 1;
            return new IntRegister(total);
        }
        IntRegister get(Instruction instruction) {
            return registerMap.get(instruction);
        }
        boolean contain(Instruction instruction) {
            return registerMap.containsKey(instruction);
        }
    }

    public class FloatRegisterAllocator {
        Map<Instruction, FloatRegister> registerMap;
        int total;
        FloatRegisterAllocator() {
            total = 0;
            registerMap = new HashMap<>();
        }
        FloatRegister allocate(Instruction instruction) {
            total -= 1;
            FloatRegister reg = new FloatRegister(total);
            registerMap.put(instruction, reg);
            return reg;
        }
        FloatRegister allocate() {
            total -= 1;
            return new FloatRegister(total);
        }
        FloatRegister get(Instruction instruction) {
            return registerMap.get(instruction);
        }
        boolean contain(Instruction instruction) {
            return registerMap.containsKey(instruction);
        }
    }

    private class StackAllocator {
        private int top = 16, maxSize = 16;
        private boolean savedRa = false;

        /**
         * 进行函数调用前的准备，目前仅设置保存返回寄存器
         * @param originFunction 原函数
         */
        public void callFunction(AsmFunction originFunction) {
            savedRa = true;
        }

        /**
         * 在栈上申请一段长度为size的内存作为栈变量
         *
         * @param size 内存大小
         * @return 指向栈上对应的地址
         */
        public StackVar push(int size) {
            top += size;
            maxSize = max(maxSize, top);
            return new StackVar(-top, size);
        }

        /**
         * 释放大小为size的栈空间
         *
         * @param size 内存大小
         */
        public void pop(int size) {
            top -= size;
        }

        /**
         * 输出函数初始化栈帧的汇编代码
         * 注意，emit操作仅当maxSize初始化完成后进行，避免栈帧大小分配错误
         */
        public List<AsmInstruction> emitHead() {
            List<AsmInstruction> res = new ArrayList<>();
            IntRegister sp = new IntRegister("sp");
            IntRegister ra = new IntRegister("ra");
            IntRegister s0 = new IntRegister("s0");
            res.add(new AsmAdd(sp, sp, new Immediate(-maxSize)));
            if (savedRa) {
                res.add(new AsmStore(ra, new Address(maxSize - 8, sp)));
            }
            res.add(new AsmStore(s0, new Address(maxSize - 16, sp)));
            return res;
        }
        public List<AsmInstruction> emitTail() {
            List<AsmInstruction> res = new ArrayList<>();
            IntRegister sp = new IntRegister("sp");
            IntRegister ra = new IntRegister("ra");
            IntRegister s0 = new IntRegister("s0");
            if (savedRa) {
                res.add(new AsmLoad(ra, new Address(maxSize - 8, sp)));
            }
            res.add(new AsmLoad(s0, new Address(maxSize - 16, sp)));
            res.add(new AsmAdd(sp, sp, new Immediate(maxSize)));
            return res;
        }
    }
}
