package com.bit.newnewcc;

public class Compiler {
    public static void main(String[] args) {
        try{
            var compileOption = CompileOption.fromCmdArguments(args);
            var driver = new Driver(compileOption);
            driver.launch();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}