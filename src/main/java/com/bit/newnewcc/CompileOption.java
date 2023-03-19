package com.bit.newnewcc;

/**
 * 编译器设置
 */
public class CompileOption {
    public enum OutputType{
        LLVM_IR, ASM
    }

    public enum OptimizeLevel{
        O0,O1,O2
    }

    public String sourceFile;

    public OutputType outputType = OutputType.ASM;
    public String outputFile = "a.out";

    public OptimizeLevel optimizeLevel = OptimizeLevel.O1;

    /**
     * 由命令行参数生成一个编译器设置
     * @param args 命令行参数
     * @return 命令行参数对应的编译器设置
     */
    public static CompileOption fromCmdArguments(String[] args){
        var compileOption = new CompileOption();
        for (int i = 0; i < args.length; i++){
            if(args[i].charAt(0) == '-'){
                switch (args[i].substring(1)) {
                    case "o" -> {
                        compileOption.outputFile = args[++i];
                    }
                    case "S" -> {
                        compileOption.outputType = CompileOption.OutputType.ASM;
                    }
                    case "emit-llvm" -> {
                        compileOption.outputType = CompileOption.OutputType.LLVM_IR;
                    }
                    case "O0" -> {
                        compileOption.optimizeLevel = CompileOption.OptimizeLevel.O0;
                    }
                    case "O1" -> {
                        compileOption.optimizeLevel = CompileOption.OptimizeLevel.O1;
                    }
                    case "O2" -> {
                        compileOption.optimizeLevel = CompileOption.OptimizeLevel.O2;
                    }
                }
            }else{
                // 如果有多个源文件，sourceFile应该改为数组
                compileOption.sourceFile = args[i];
            }
        }
        return compileOption;
    }

}
