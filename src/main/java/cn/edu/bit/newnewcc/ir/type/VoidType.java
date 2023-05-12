package cn.edu.bit.newnewcc.ir.type;

import cn.edu.bit.newnewcc.ir.Type;
import cn.edu.bit.newnewcc.ir.value.Constant;

/**
 * Void类型
 * <p>
 * 该类型是单例
 */
public class VoidType extends Type {
    private VoidType() {
    }

    @Override
    protected String getTypeName_() {
        return "void";
    }

    private static VoidType instance = null;

    /**
     * 获取Void类型的实例
     * @return Void类型的唯一实例
     */
    public static VoidType getInstance() {
        if (instance == null) {
            instance = new VoidType();
        }
        return instance;
    }

    @Override
    public Constant getDefaultInitialization() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSize() {
        // 理论上应该返回0，但是这个方法应该不会被调用到
        throw new UnsupportedOperationException();
    }

}
