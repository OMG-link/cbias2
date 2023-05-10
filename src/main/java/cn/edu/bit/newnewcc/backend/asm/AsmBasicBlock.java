package cn.edu.bit.newnewcc.backend.asm;

import cn.edu.bit.newnewcc.backend.asm.instruction.AsmInstruction;
import cn.edu.bit.newnewcc.backend.asm.operand.GlobalTag;

import java.util.List;

public class AsmBasicBlock {
    private List<AsmInstruction> instructionList;
    private GlobalTag blockTag;
    public AsmBasicBlock(String blockName) {
        this.blockTag = new GlobalTag(blockName, false);
    }

}
