package cn.edu.bit.newnewcc;

import cn.edu.bit.newnewcc.frontend.SysYLexer;
import cn.edu.bit.newnewcc.frontend.SysYParser;
import cn.edu.bit.newnewcc.frontend.Translator;
import cn.edu.bit.newnewcc.ir.Module;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Driver {
    private final CompilerOptions compilerOptions;

    public Driver(CompilerOptions compilerOptions) {
        this.compilerOptions = compilerOptions;
    }

    public void launch() throws IOException {
        for (String inputFileName : compilerOptions.getInputFileNames()) {
            CharStream input = CharStreams.fromFileName(inputFileName);
            SysYLexer lexer = new SysYLexer(input);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            SysYParser parser = new SysYParser(tokenStream);
            ParseTree tree = parser.compilationUnit();
            Translator visitor = new Translator();
            visitor.visit(tree);
            Module module = visitor.getModule();
        }
    }
}
