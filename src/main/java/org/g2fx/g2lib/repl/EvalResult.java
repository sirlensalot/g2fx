package org.g2fx.g2lib.repl;

public record EvalResult(EvalResultType type, int waitMs) {

    public enum EvalResultType {
        Continue,Quit,Wait;
    }

    public static EvalResult evalContinue() {
        return new EvalResult(EvalResultType.Continue, 0);
    }

    public boolean isWait() {
        return type == EvalResultType.Wait;
    }

    public boolean isQuit() {
        return type == EvalResultType.Quit;
    }
}
