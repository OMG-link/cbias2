package com.bit.newnewcc.util;

import com.bit.newnewcc.ir.value.Function;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 名称分配器 <br>
 * 为局步变量和全局变量自动分配名字 <br>
 */
public class NameAllocator {

    /**
     * 局步变量分配状态
     */
    private static Map<Function, Integer> lvAllocateState;

    /**
     * 为局部变量获取一个名字
     * @param function 局部变量所属的函数
     * @return 一个纯数字的名字
     */
    public static @NonNull String getLvName(Function function){
        if(lvAllocateState ==null){
            lvAllocateState = new HashMap<>();
        }
        int id = lvAllocateState.getOrDefault(function,-1)+1;
        lvAllocateState.put(function,id);
        return String.valueOf(id);
    }

    /**
     * 全局变量分配状态
     */
    private static int gvAllocateState = 0;

    /**
     * 为全局变量获取一个名字
     * @return 一个纯数字的名字
     */
    public static @NonNull String getGvName(){
        return String.valueOf(gvAllocateState++);
    }

}
