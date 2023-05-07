package cn.edu.bit.newnewcc.ir.exception;

public class IntegrityVerifyFailedException extends CompilationProcessCheckFailedException {
    public IntegrityVerifyFailedException() {
    }

    public IntegrityVerifyFailedException(String message) {
        super(message);
    }

    public IntegrityVerifyFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public IntegrityVerifyFailedException(Throwable cause) {
        super(cause);
    }
}
