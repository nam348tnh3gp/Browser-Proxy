/*
 * Decompiled with CFR 0.152.
 */
import java.io.InputStream;
import java.util.TimerTask;

/*
 * Duplicate member names - consider using --renamedupmembers true
 */
public final class g
extends TimerTask
implements Runnable {
    public static boolean Code;
    public long Code;
    public Code Code;
    private int Code;
    private static g I;
    private static g Code;
    private static boolean J;
    public static boolean I;
    public String Code;
    public InputStream Code;
    public a Code;
    public static Object Code;
    public a I;
    public static boolean Z;

    public g(int n) {
        this.Code = n;
    }

    /*
     * Exception decompiling
     */
    public final void run() {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [27[CASE]], but top level block is 0[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    public static synchronized void Code() {
        if (I == null) {
            I = new g(27);
            new Thread(I).start();
        }
        g g2 = I;
        synchronized (g2) {
            Z = true;
            I.notify();
            return;
        }
    }

    public static synchronized void Code(Code code) {
        if (!a.c) {
            Runnable runnable;
            if (Code == null) {
                Code = new g(23);
                runnable = new Thread(Code);
                ((Thread)runnable).setPriority(1);
                ((Thread)runnable).start();
            }
            runnable = Code;
            synchronized (runnable) {
                g.Code.Code = code;
                J = true;
                Code.notify();
                return;
            }
        }
    }

    public static void I() {
        f.Code = new g(32);
        f.Code.Code = System.currentTimeMillis() + (long)10000;
        Browser.Code.Code.schedule((TimerTask)f.Code, 10000, (long)2000);
    }

    static {
        Code = new Object();
    }
}

