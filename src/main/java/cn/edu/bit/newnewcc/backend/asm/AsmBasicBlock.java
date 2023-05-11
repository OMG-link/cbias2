package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;

import java.util.List;

public class AsmBasicBlock {
    private List<AsmInstruction> instructionList;
    private GlobalTag blockTag;
    AsmFunction function;
    public AsmBasicBlock(AsmFunction function, BasicBlock block) {
        this.function = function;
        this.blockTag = new GlobalTag(function.getFunctionName() + "_" + block.getValueName(), false);
    }

}
