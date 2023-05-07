package cn.edu.bit.newnewcc.ir.value.instruction;

import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.type.PointerType;

import java.util.ArrayList;
import java.util.List;

/**
 * 栈空间分配指令
 */
public class AllocateInst extends MemoryInst{
    public AllocateInst(Type allocatedType){
        super(PointerType.getInstance(allocatedType));
    }

    @Override
    public PointerType getType() {
        return (PointerType) super.getType();
    }

    @Override
    public List<Operand> getOperandList() {
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format(
                "%s = alloca %s",
                this.getValueNameIR(),
                this.getType().getBaseType().getTypeName()
        );
    }
}
