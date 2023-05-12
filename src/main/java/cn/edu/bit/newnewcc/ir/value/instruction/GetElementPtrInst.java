package cn.edu.bit.newnewcc.ir.value.instruction;

import cn.edu.bit.newnewcc.ir.Operand;
import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.Value;
import cn.edu.bit.newnewcc.ir.exception.IndexOutOfBoundsException;
import cn.edu.bit.newnewcc.ir.type.PointerType;
import cn.edu.bit.newnewcc.ir.value.Instruction;

import java.util.ArrayList;
import java.util.List;

/**
 * 取下标指令
 *
 * @see <a href="https://llvm.org/docs/LangRef.html#getelementptr-instruction">LLVM IR文档</a>
 */
public class GetElementPtrInst extends Instruction {
    /**
     * 基地址操作数
     */
    private final Operand rootOperand;

    /**
     * 下标操作数列表
     * <p>
     * 注意其与直观意义上的下标不同，例如 a[1] 会产生两个下标 [0,1]
     * <p>
     * 具体定义详见 LLVM IR 文档
     */
    private final List<Operand> indexOperands;

    /**
     * @param root    基地址操作数
     * @param indexes 下标操作数列表。这与直观意义上的下标不同，请务必阅读 LLVM IR 文档。
     */
    public GetElementPtrInst(Value root, List<Value> indexes) {
        super(analysisDereferencedType(root.getType(), indexes.size() - 1));
        this.rootOperand = new Operand(this, root.getType(), root);
        this.indexOperands = new ArrayList<>();
        for (var index : indexes) {
            indexOperands.add(new Operand(this, index.getType(), index));
        }
    }

    /**
     * @return 基地址操作数
     */
    public Value getRootOperand() {
        return rootOperand.getValue();
    }

    /**
     * @param value 基地址操作数
     */
    public void setRootOperand(Value value) {
        rootOperand.setValue(value);
    }

    /**
     * @return 下标操作数列表
     */
    public List<Value> getIndexOperands() {
        var list = new ArrayList<Value>();
        for (Operand indexOperand : indexOperands) {
            list.add(indexOperand.getValue());
        }
        return list;
    }

    /**
     * 设置某个下标的值
     *
     * @param index 下标
     * @param value 值
     */
    public void setIndexAt(int index, Value value) {
        if (index < 0 || index >= indexOperands.size()) {
            throw new IndexOutOfBoundsException(index, 0, indexOperands.size());
        }
        indexOperands.get(index).setValue(value);
    }

    @Override
    public List<Operand> getOperandList() {
        var list = new ArrayList<Operand>();
        list.add(rootOperand);
        list.addAll(indexOperands);
        return list;
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        builder.append(String.format(
                "%s = getelementptr %s, ptr %s",
                this.getValueNameIR(),
                this.getTypeName(),
                getRootOperand().getValueNameIR()
        ));
        for (var indexOperand : indexOperands) {
            var index = indexOperand.getValue();
            builder.append(String.format(
                    ", %s %s",
                    index.getTypeName(),
                    index.getValueNameIR()
            ));
        }
        return builder.toString();
    }

    /**
     * 分析解引用后的类型
     *
     * @param rootType         根类型
     * @param dereferenceCount 解引用的次数
     * @return 解引用后的类型
     */
    public static Type analysisDereferencedType(Type rootType, int dereferenceCount) {
        for (var i = 0; i < dereferenceCount; i++) {
            rootType = ((PointerType) rootType).getBaseType();
        }
        return rootType;
    }
}
