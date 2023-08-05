package cn.edu.bit.newnewcc.backend.asm.instruction;

import cn.edu.bit.newnewcc.backend.asm.operand.*;
import cn.edu.bit.newnewcc.backend.asm.util.Others;
import cn.edu.bit.newnewcc.backend.asm.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 汇编指令基类
 */
public abstract class AsmInstruction {
    private String instructionName;
    AsmOperand operand1, operand2, operand3;

    protected AsmInstruction(String instructionName, AsmOperand operand1, AsmOperand operand2, AsmOperand operand3) {
        this.instructionName = instructionName;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operand3 = operand3;
    }

    public Collection<Integer> getWriteRegId() {
        if (this instanceof AsmStore) {
            if (getOperand(2) instanceof Register) {
                return Collections.singleton(2);
            }
            return new HashSet<>();
        }
        if (this instanceof AsmJump) {
            return new HashSet<>();
        }
        if (getOperand(1) instanceof RegisterReplaceable) {
            return Collections.singleton(1);
        }
        return new HashSet<>();
    }

    public Collection<Integer> getWriteVRegId() {
        var res = new HashSet<Integer>();
        var regId = getWriteRegId();
        for (var x : regId) {
            var reg = ((RegisterReplaceable)this.getOperand(x)).getRegister();
            if (reg.isVirtual()) {
                res.add(x);
            }
        }
        return res;
    }

    public Collection<Integer> getReadRegId() {
        var res = new HashSet<Integer>();
        for (int i = 1; i <= 3; i++) {
            if (getOperand(i) instanceof RegisterReplaceable) {
                boolean flag = (this instanceof AsmStore) ? (i == 1 || (i == 2 && !(getOperand(i) instanceof Register))) :
                        (this instanceof AsmJump || (i > 1));
                if (flag) {
                    res.add(i);
                }
            }
        }
        return res;
    }

    public Collection<Integer> getReadVRegId() {
        var res = new HashSet<Integer>();
        var regId = getReadRegId();
        for (var x : regId) {
            var reg = ((RegisterReplaceable)this.getOperand(x)).getRegister();
            if (reg.isVirtual()) {
                res.add(x);
            }
        }
        return res;
    }

    public Collection<Integer> getVRegId() {
        var res = getReadVRegId();
        res.addAll(getWriteVRegId());
        return res;
    }

    public Collection<Register> getWriteRegSet() {
        var res = new HashSet<Register>();
        for (int i : getWriteRegId()) {
            RegisterReplaceable op = (RegisterReplaceable) this.getOperand(i);
            res.add(op.getRegister());
        }
        return res;
    }

    public Collection<Integer> getWriteVRegSet() {
        var res = new HashSet<Integer>();
        for (int i : getWriteVRegId()) {
            RegisterReplaceable op = (RegisterReplaceable) this.getOperand(i);
            res.add(op.getRegister().getIndex());
        }
        return res;
    }

    public Collection<Register> getReadRegSet() {
        var res = new HashSet<Register>();
        for (int i : getReadRegId()) {
            RegisterReplaceable op = (RegisterReplaceable) this.getOperand(i);
            res.add(op.getRegister());
        }
        return res;
    }

    public Collection<Integer> getReadVRegSet() {
        var res = new HashSet<Integer>();
        for (int i : getReadVRegId()) {
            RegisterReplaceable op = (RegisterReplaceable) this.getOperand(i);
            res.add(op.getRegister().getIndex());
        }
        return res;
    }

    /**
     * 获取指令修改的寄存器列表
     * @return 寄存器列表
     */
    public Collection<Register> getModifiedRegisters() {
        if (this instanceof AsmCall) {
            Set<Register> res = new HashSet<>();
            Register tmp;
            for (int i = 0; i <= 31; i++) {
                tmp = IntRegister.getPhysical(i);
                if (!tmp.isPreserved()) {
                    res.add(tmp);
                }
                tmp = FloatRegister.getPhysical(i);
                if (!tmp.isPreserved()) {
                    res.add(tmp);
                }
            }
            return res;
        } else {
            return getWriteRegSet();
        }
    }

    protected void setInstructionName(String name) {
        this.instructionName = name;
    }

    protected String getInstructionName() {
        return instructionName;
    }

    protected void setOperand1(AsmOperand operand1) {
        this.operand1 = operand1;
    }

    protected void setOperand2(AsmOperand operand2) {
        this.operand2 = operand2;
    }

    protected void setOperand3(AsmOperand operand3) {
        this.operand3 = operand3;
    }

    public void replaceOperand(int index, AsmOperand operand) {
        if (!(1 <= index && index <= 3)) {
            throw new RuntimeException("asm operand index out of bound");
        }
        if (index == 1) {
            this.operand1 = operand;
        } else if (index == 2) {
            this.operand2 = operand;
        } else {
            this.operand3 = operand;
        }
    }

    /**
     * 获取第index个参数
     * @param index 下标（1 <= index <= 3）
     * @return 参数值
     */
    public AsmOperand getOperand(int index) {
        if (index == 1) {
            return this.operand1;
        } else if (index == 2) {
            return this.operand2;
        } else if (index == 3) {
            return this.operand3;
        } else {
            throw new RuntimeException("Asm Operand index error : " + index);
        }
    }

    /**
     * 指令输出函数，依次输出指令名称及参数
     */
    public String emit() {
        String res = instructionName;
        if (operand1 != null) {
            res += " " + operand1.emit();
            if (operand2 != null) {
                res += ", " + operand2.emit();
                if (operand3 != null) {
                    res += ", " + operand3.emit();
                }
            }
        }
        if (!(this instanceof AsmLabel)) {
            res = '\t' + res;
        }
        return res + "\n";
    }

    public boolean isMove() {
        if (this instanceof AsmLoad || this instanceof AsmStore) {
            if (getOperand(1) instanceof Register) {
                return getOperand(2) instanceof Register;
            }
        }
        return false;
    }

    public boolean isMoveVToV() {
        return isMove() && ((Register)getOperand(1)).isVirtual() && ((Register)getOperand(2)).isVirtual();
    }

    public Pair<Integer, Integer> getMoveVReg() {
        if (!isMoveVToV()) {
            throw new RuntimeException("error: get move reg from not move instruction");
        }
        var writeSet = this.getWriteVRegSet();
        var readSet = this.getReadVRegSet();
        return new Pair<>((Integer) writeSet.toArray()[0], (Integer) readSet.toArray()[0]);
    }

    @Override
    public String toString() {
        return Others.deleteCharString(emit(), "\t\n");
    }
}
