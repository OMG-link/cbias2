package com.bit.newnewcc;

public class Compiler {
    public static void main(String[] args) {
        try {
            var compilerOptions = CompilerOptions.fromCmdArguments(args);
            var driver = new Driver(compilerOptions);
            driver.launch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
