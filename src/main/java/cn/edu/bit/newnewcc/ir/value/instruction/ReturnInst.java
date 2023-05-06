package cn.edu.bit.newnewcc.ir.value.instruction;

import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.VoidType;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 返回语句
 */
public class ReturnInst extends TerminateInst{
    private final Operand returnValueOperand;

    /**
     * 构建一个未填入返回值的返回语句 <br>
     * 若要构建返回void的语句，请使用new ReturnInst(VoidValue.getInstance()) <br>
     * @param returnType 返回值类型
     */
    public ReturnInst(Type returnType) {
        this.returnValueOperand = new Operand(this,returnType,null);
    }

    /**
     * 构建一个返回语句 <br>
     * 若要构建返回void的语句，请传入VoidValue.getInstance() <br>
     * @param returnValue 返回值
     */
    public ReturnInst(Value returnValue) {
        this.returnValueOperand = new Operand(this, returnValue.getType(), returnValue);
    }

    public Value getReturnValue() {
        return returnValueOperand.getValue();
    }

    public void setReturnValue(Value returnValue) {
        this.returnValueOperand.setValue(returnValue);
    }

    @Override
    public String toString() {
        var returnValue = returnValueOperand.getValue();
        if(returnValue.getType() instanceof VoidType){
            return "ret void";
        }else{
            return String.format(
                    "ret %s %s",
                    returnValue.getTypeName(),
                    returnValue.getValueNameIR()
            );
        }
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(returnValueOperand);
        return list;
    }

    @Override
    public Collection<BasicBlock> getExits() {
        return new ArrayList<>();
    }
}
