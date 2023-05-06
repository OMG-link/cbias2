package cn.edu.bit.newnewcc.ir.value.instruction;

import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UnreachableInst extends TerminateInst {

    @Override
    public String toString() {
        return "unreachable";
    }

    @Override
    public List<Operand> getOperandList() {
        return new ArrayList<>();
    }

    @Override
    public Collection<BasicBlock> getExits() {
        return Collections.unmodifiableCollection(new ArrayList<>());
    }

}
