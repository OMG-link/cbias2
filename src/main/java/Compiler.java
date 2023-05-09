import cn.edu.bit.newnewcc.NewNewCCompiler;

public class Compiler {
    public static void main(String[] args) {
        try {
            NewNewCCompiler.main(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
