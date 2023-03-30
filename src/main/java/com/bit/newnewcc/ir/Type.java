package com.bit.newnewcc.ir;

/**
 * 值的类型 <br>
 * 所有类型都是单例，可以直接用==判断 <br>
 */
public abstract class Type {
    /**
     * 获取类型的名字 <br>
     * 此方法至多被调用一次，因为其会缓存之前的结果 <br>
     * @return 类型的名字
     */
    protected abstract String getTypeName_();

    private String typeNameCache;
    public String getTypeName(){
        if(typeNameCache==null){
            typeNameCache = getTypeName_();
        }
        return typeNameCache;
    }

}
