package cn.edu.bit.newnewcc.pass.ir;

import cn.edu.bit.newnewcc.ir.Module;
import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.type.ArrayType;
import cn.edu.bit.newnewcc.ir.type.PointerType;
import cn.edu.bit.newnewcc.ir.value.BasicBlock;
import cn.edu.bit.newnewcc.ir.value.Function;
import cn.edu.bit.newnewcc.ir.value.Instruction;
import cn.edu.bit.newnewcc.ir.value.constant.ConstInt;
import cn.edu.bit.newnewcc.ir.value.instruction.BitCastInst;
import cn.edu.bit.newnewcc.ir.value.instruction.GetElementPtrInst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 数组下标压缩 <br>
 * 对于完全由常量确定的下标，将其扁平化 <br>
 */
// 有助于优化局部数组的初始化语句
public class ArrayOffsetCompressPass {

    private final Function function;

    private ArrayOffsetCompressPass(Function function) {
        this.function = function;
    }

    private record ArrayAddress(Value rootAddress, int offset, Type type) {
    }

    private final Map<GetElementPtrInst, ArrayAddress> addressMap = new HashMap<>();

    private boolean canCompressGep(GetElementPtrInst getElementPtrInst) {
        var compressedGep = getCompressedGep(getElementPtrInst);
        // 当前gep也无法压缩
        if (compressedGep.rootAddress.getType() == getElementPtrInst.getType()) return false;
        // 只能压缩一层，相当于没压缩
        var uncompressedArray = (ArrayType) ((PointerType) compressedGep.rootAddress.getType()).getBaseType();
        var compressedArray = ((PointerType) compressedGep.type).getBaseType();
        if (uncompressedArray.getBaseType() == compressedArray) return false;
        return true;
    }

    private ArrayAddress getCompressedGep_(GetElementPtrInst getElementPtrInst) {
        boolean isConstGep = true;
        for (Value indexOperand : getElementPtrInst.getIndexOperands()) {
            if (!(indexOperand instanceof ConstInt)) {
                isConstGep = false;
                break;
            }
        }
        if (isConstGep) {
            var rootArrayAddress = getCompressedGep(getElementPtrInst.getRootOperand());
            var offset = rootArrayAddress.offset + ((ConstInt) getElementPtrInst.getIndexAt(0)).getValue();
            var type = ((PointerType) rootArrayAddress.type).getBaseType();
            for (int i = 1; i < getElementPtrInst.getIndicesSize(); i++) {
                var arrayType = (ArrayType) type;
                offset = offset * arrayType.getLength() + ((ConstInt) getElementPtrInst.getIndexAt(i)).getValue();
                type = arrayType.getBaseType();
            }
            type = PointerType.getInstance(type);
            return new ArrayAddress(rootArrayAddress.rootAddress, offset, type);
        } else {
            return new ArrayAddress(getElementPtrInst, 0, getElementPtrInst.getType());
        }
    }

    private ArrayAddress getCompressedGep(Value value) {
        if (!(value instanceof GetElementPtrInst getElementPtrInst)) {
            return new ArrayAddress(value, 0, value.getType());
        }
        if (!addressMap.containsKey(getElementPtrInst)) {
            addressMap.put(getElementPtrInst, getCompressedGep_(getElementPtrInst));
        }
        return addressMap.get(getElementPtrInst);
    }

    private boolean runOnFunction() {
        boolean changed = false;
        for (BasicBlock basicBlock : function.getBasicBlocks()) {
            for (Instruction instruction : basicBlock.getMainInstructions()) {
                if (instruction instanceof GetElementPtrInst) continue;
                for (Operand operand : instruction.getOperandList()) {
                    if (operand.getValue() instanceof GetElementPtrInst getElementPtrInst && canCompressGep(getElementPtrInst)) {
                        var arrayAddress = getCompressedGep(getElementPtrInst);
                        var bitcast = new BitCastInst(arrayAddress.rootAddress, arrayAddress.type);
                        var indices = new ArrayList<Value>();
                        indices.add(ConstInt.getInstance(arrayAddress.offset));
                        var gep = new GetElementPtrInst(bitcast, indices);
                        bitcast.insertBefore(instruction);
                        gep.insertBefore(instruction);
                        operand.setValue(gep);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    public static boolean runOnFunction(Function function) {
        return new ArrayOffsetCompressPass(function).runOnFunction();
    }

    public static boolean runOnModule(Module module) {
        boolean changed = false;
        for (Function function : module.getFunctions()) {
            changed |= runOnFunction(function);
        }
        return changed;
    }
}
