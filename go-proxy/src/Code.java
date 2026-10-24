/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Font
 *  javax.microedition.lcdui.Graphics
 *  javax.microedition.lcdui.Image
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/*
 * Duplicate member names - consider using --renamedupmembers true
 */
public final class Code
implements Runnable {
    public int Code;
    public long Code;
    public B Code;
    public int I;
    private static final int aD;
    public byte[] I;
    public a Code;
    public boolean Code;
    public int[] Code;
    public int[] I;
    public int[] Z;
    public boolean Z;
    public int J;
    public String J;
    public String B;
    public Hashtable B;
    public boolean I;
    private Hashtable C;
    public boolean B;
    public boolean J;
    public String Code;
    public String I;
    public String Z;
    public static String c;
    public String b;
    public String C;
    public int x;
    public byte[] Code;
    public Image Code;
    public boolean C;
    public boolean g;
    public int g;
    public int B = -1;
    private int U;
    public int C = -1;
    public int a;
    public String a;
    public static Image[] Code;
    public boolean a;
    public boolean b;
    public boolean f;
    public boolean h;
    public boolean c;
    public int Z;
    public int T = -1;
    public boolean d;
    public int b;
    public String[] Code;
    public DataOutputStream Code;
    private int ay;
    private int am;
    private Integer Code;
    public I Code;
    public int f;
    private boolean n;
    public int c = 1;
    private Object[] I;
    public static int d;
    public int e;
    public int j;
    private int V;
    private int aG;
    private int W;
    private int X;
    private int Y;
    private int an;
    private int aE;
    private int aa;
    private int ab;
    private int ac;
    private int ad;
    private int ae;
    private int af;
    private int ag;
    private int ah;
    private int ai;
    private int aj;
    private int ak;
    private int al;
    public boolean e = true;
    public int h;
    public int i;
    public int w;
    public int k;
    private int ao;
    private int ap;
    private boolean k;
    public boolean j;
    private int aq;
    private int ar;
    private int as;
    private int at;
    private int au;
    private int av;
    private int aw;
    private int ax;
    private boolean l;
    private boolean m;
    public int l;
    public int m;
    public int n;
    public int o;
    public int p;
    public int q = 40;
    public int r = 25;
    public int s;
    public int t;
    public int u;
    public int v;
    public Hashtable Code = new int[6];
    public Hashtable I;
    public Hashtable Z;
    public Hashtable J;
    public B Z;
    public B I;
    public DataOutputStream I;
    private int az;
    public int y;
    public int z;
    public int A;
    public int M;
    public int D;
    public int E;
    public DataOutputStream Z;
    private int aA;
    private int aB;
    private int aC;
    private Vector J = -1;
    public boolean i;
    public static Object[] Code;
    public static int F;
    public static Vector Code;
    public int G;
    public int H;
    public int K;
    public int L;
    private int aF;
    public static char[] Code;
    public static Image[] I;
    public Vector Z = -1;
    public Vector I = new Object[11];
    public int N;
    public static int[] B;
    public static int[] J;
    public static int[] C;
    public static int O;
    public static int P;
    public int Q;
    public int R;
    public int S;

    /*
     * Exception decompiling
     */
    private void Z(int var1_1) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [0[TRYBLOCK]], but top level block is 15[CASE]
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

    /*
     * Exception decompiling
     */
    private void c(int var1_1, int var2_3) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [28[UNCONDITIONALDOLOOP]], but top level block is 5[TRYBLOCK]
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

    private void g() {
        this.Code = 1;
        this.I = 10;
        this.c = true;
        this.E = 1;
        this.J = new Vector();
    }

    private void d(int n, int n2) {
        this.G = this.I(this.G);
        this.H = this.I(this.H);
        this.K = this.I(this.K);
        this.L = this.I(this.L);
        if (this.K == 0) {
            this.K = 1;
        }
        if (this.L == 0) {
            this.L = 1;
        }
        this.G += n;
        this.H += n2;
    }

    private void a(int n) {
        if (n <= this.b) {
            return;
        }
        this.b = n;
        if (n == 7 || n == 4 || n == 5 || n == 6) {
            if (n == 4 && this.Code != null && this.Code[4] == 1) {
                this.I(true);
            }
            f.c = false;
            a.I(Browser.Code);
        }
        if (!a.aG && a.k != null && a.k.equals(this.Code)) {
            this.Code = 13;
            this.I = 14;
            this.C = true;
            this.b = true;
        }
        this.Code.Code(this);
    }

    public final void I() {
        if (this.Code != null) {
            if (this.Code) {
                this.W = this.Code[0];
                this.X = this.Code[1];
                this.o = this.Code[5];
            }
            this.e(this.Code[0], this.Code[1]);
            this.j = this.Code[2];
            this.k = this.Code[3];
        }
    }

    public static void Code(Code code, DataInputStream dataInputStream) throws IOException {
        code.Code(dataInputStream);
    }

    private void J(int n) throws IOException {
        this.Code = 9;
        this.I = 37;
        this.B = 23;
        this.J = "";
        int n2 = f.Code[42];
        String string = "";
        if (n == 0) {
            string = a.Code(168);
            if (Browser.Code.getAppProperty("MIDlet-Jar-RSA-SHA1") != null) {
                string = a.Code(169);
            }
        } else if (n == 1) {
            this.J = a.Code(37);
            string = a.Code(153) + " " + a.Code(154);
        }
        if (this.J.length() > 0) {
            this.Code(this.J, 0, n2);
            this.C(5);
        }
        this.Code(string + " " + a.Code(170, a.Code(35)), 0, n2);
    }

    private void i() {
        this.Code = 5;
        this.I = 6;
        this.B = 28;
        this.C = true;
        this.b = true;
    }

    private boolean C() {
        return this.f || this.e <= this.Code.c;
    }

    public final int I() {
        if (this.b == 2) {
            this.aG = Math.min(this.aG + Math.max((this.N * 10 - this.aG) / 5, 0) + 4, 1000);
            return this.aG + 250;
        }
        return this.v / 3 + (this.Z() << 1) / 3;
    }

    public final boolean Z() {
        return !this.I() && !this.Code() && this.e != 0 && this.Code.c != 0 && 256 * this.Code.c / this.e < 128;
    }

    private void Code(int n, String string, boolean bl) throws IOException {
        this.Code(string, "", bl << 1, 0, true);
        int n2 = this.z;
        this.D += 2;
        this.z = this.D;
        this.Code(a.Code(n), 0, 0);
        this.z = n2;
        this.C(5);
    }

    private void Code(Vector vector, boolean bl, int n) throws IOException {
        int n2;
        this.k();
        String string = null;
        if (vector == a.Code) {
            string = "bookmark:";
            n2 = -1000;
        } else if (vector == a.Z) {
            string = "search:";
            n2 = -2000;
        } else if (vector == a.I) {
            string = "speeddial:";
            n2 = -4000;
        } else if (vector == f.I) {
            n2 = -3000;
        } else if (vector == a.J) {
            string = "open:";
            n2 = -5000;
        } else {
            n2 = -3999;
        }
        int n3 = a.Code(0, "8.") + 2;
        n = n > -1 ? Math.min(n, vector.size()) : vector.size();
        for (int i = 0; i < n; ++i) {
            Object[] objectArray = a.Code(vector, i);
            boolean bl2 = vector == f.I || vector == a.J ? false : a.Code(objectArray, 5) == 82;
            String string2 = null;
            boolean bl3 = false;
            int n4 = 18858;
            if (!(vector != a.I || (bl3 = a.Code(objectArray)) && bl)) {
                int n5 = this.D;
                this.Code(i + 1 + ". ", 0, 0x808080);
                this.D = n5 + n3;
                if (bl3) {
                    string2 = a.Code(128);
                    n4 = 0x808080;
                }
            }
            if (this.h) {
                string2 = (String)objectArray[1];
                string = "";
                objectArray = null;
                n4 = 0;
                if (vector != a.Code && vector != f.I && i > 0 && a.Code((Object[])a.I.elementAt(i - 1))) {
                    n4 = 0x808080;
                }
            }
            if (bl && bl3 || bl2 && vector != a.I) continue;
            this.Code(objectArray, string + i + (string.length() > 0 ? ":" : ""), string2, n2 - i, false, false, vector == a.Z && this.o == 0 ? i == a.A : false, false, n4);
        }
    }

    private void Code(int n, String string) throws IOException {
        this.Code(a.Code(n) + ": ", 1, 0);
        this.Code(string, 0, 0);
        this.k();
    }

    private void Z(Vector vector) throws IOException {
        if (vector == a.Code || vector == a.I || vector == null) {
            f.I = true;
        }
        this.Code = 7;
        this.I = 10;
        this.b = true;
        String string = (String)Code[1];
        if (vector == a.Code || vector == null) {
            this.J = a.Code(7);
        } else if (vector == a.I) {
            this.J = a.Code(33);
        } else if (vector == a.Z) {
            this.J = a.Code(47);
        } else if (vector == a.J) {
            this.J = a.Code(145);
        }
        this.I(a.Code(65), null);
        this.Code("a", string, (byte[])Code[2], 0, 0);
        this.C(5);
        if (vector == a.J) {
            if (vector.size() > 2 && F == -1) {
                this.Code("s", "1", 0, 0, true);
                this.Code(a.Code(157), 0, 0);
            }
        } else {
            this.I(a.Code(63), null);
            this.Code("b", a.Z((String)Code[0]), null, 48, 0);
            this.C(2);
            this.Z(a.I(53), a.Code(129));
            this.C(5);
        }
        if (vector == a.I) {
            this.Z("dialog:" + a.I(54) + "&l=b", a.Code(132));
            this.k();
            if (f.I.size() > 0) {
                this.Z("dialog:" + a.I(54) + "&l=h", a.Code(133));
                this.k();
            }
        }
        if (vector == null) {
            this.Code(a.Code(33), 0, 0);
            this.k();
            this.C(2);
            if (Code == null) {
                vector = new Vector<Object[]>();
                Object[] objectArray = a.Code();
                Object[] objectArray2 = objectArray;
                objectArray[1] = "- " + a.Code(131) + " -";
                vector.addElement(objectArray2);
                for (int i = 0; i < a.I.size(); ++i) {
                    Object[] objectArray3 = a.Code((Object[])a.I.elementAt(i));
                    if (a.Code(objectArray3)) {
                        objectArray3[1] = a.Code(130);
                    }
                    objectArray3[1] = String.valueOf(i + 1) + ": " + objectArray3[1];
                    vector.addElement(objectArray3);
                }
                Code = vector;
                F = -1;
            }
            this.Code("$", "", null, 129, 0x888888);
        }
        if (vector == a.Z && string.length() == 0) {
            this.Code(a.Code(127, a.Code(108)), 0, 0x808080);
        }
    }

    private void Code(Vector vector, boolean bl) throws IOException {
        this.h = bl;
        this.c = true;
        this.y = 2;
        this.Code = 2;
        if (this.h) {
            this.J = a.Code(41);
            this.Code = 1;
            this.I = 10;
        } else if (vector == a.Code) {
            this.J = a.Code(7);
            this.Code(null, a.I(91), a.Code(33), -10, false, false, false, true, 18858);
            if (f.Code) {
                this.Code(null, "myopera:///b", a.Code(179), -9, false, false, false, true, 18858);
            }
            this.Code(null, "myopera:///n", a.Code(180), -12, false, false, false, true, 18858);
            if (a.X) {
                this.Code(null, a.I(136), a.Code(146), -9, false, false, false, true, 18858);
            }
        } else if (vector == a.I) {
            this.J = a.Code(33);
        } else if (vector == a.Z) {
            this.J = a.Code(47);
        } else if (vector == a.J) {
            this.J = a.Code(146);
        }
        this.Code(vector, false, -1);
    }

    public final void Code(Object[] objectArray) {
        this.I = objectArray;
        this.Code = (String)objectArray[0];
        this.J = (String)objectArray[1];
        this.Code = (byte[])objectArray[2];
        this.B = (String)objectArray[3];
        if (objectArray.length > 4) {
            this.I = (String)objectArray[4];
            this.Code = (int[])objectArray[7];
            if (this.Code != null) {
                if (this.Code || a.Code(this.Code) == 65) {
                    this.W = this.Code[0];
                    this.X = this.Code[1];
                    this.o = this.Code[5];
                }
                this.e(this.Code[0], this.Code[1]);
                this.j = this.Code[2];
                this.k = this.Code[3];
                return;
            }
            this.Code = new int[6];
        }
    }

    public final void c() {
        this.g = this.Y;
        this.U = this.W;
        this.V = this.X;
    }

    public final int C() {
        return this.I(this.U);
    }

    public final int J() {
        return this.I(this.e);
    }

    public Code(a a2) {
        this.Code = new Hashtable();
        this.I = new Hashtable();
        this.Z = new Hashtable();
        this.C = new Hashtable();
        this.Code = a2;
        this.Y = this.g = 256;
        this.a = f.l;
    }

    public final void f() {
        if (this.Code.aS) {
            this.Code.ao = 0;
            this.Code.aS = false;
            this.Z = null;
            this.B = null;
            this.T = -1;
            this.Code = this.Z;
            this.B = this.C;
            this.I = this.J;
            this.s();
            this.c(0);
        }
    }

    public final void Code(String string) {
        this.d = true;
        a.Code((Runnable)new a(this, string));
    }

    public static Code Code(Object[] objectArray) {
        if (objectArray.length <= 8) {
            return null;
        }
        return (Code)objectArray[8];
    }

    public final void Code(String object, boolean bl) {
        this.Code = bl;
        this.C = object;
        if (!this.Code()) {
            object = new Thread(this);
            ((Thread)object).setPriority(10);
            ((Thread)object).start();
            while (this.b == 0) {
                Thread.yield();
            }
            return;
        }
        this.Z(a.Code(this.Code));
    }

    public final synchronized String Code() {
        Object object;
        StringBuffer stringBuffer = new StringBuffer();
        if (this.Code.I(1)) {
            int n = this.k;
            object = this;
            int n2 = ((Code)object).Z(n, 5);
            stringBuffer.append("x=").append(String.valueOf(this.j)).append("&y=").append(String.valueOf(n + ((Code)object).I[n2 * 6] - ((Code)object).I[n2 * 6 + 5]));
        }
        Enumeration enumeration = this.Z.keys();
        while (enumeration.hasMoreElements()) {
            object = (String)enumeration.nextElement();
            if (((String)object).startsWith("o:")) continue;
            a.Code(stringBuffer.append("&"), (String)object);
            a.Code(stringBuffer.append("="), this.Z.get(object).toString());
        }
        return stringBuffer.toString();
    }

    public final void Z() {
        String string;
        int n;
        Object v;
        int n2;
        a.ay = this.Z.containsKey("b");
        a.aw = this.Z.containsKey("d");
        a.aB = this.Z.containsKey("e");
        a.aC = this.Z.containsKey("i");
        a.ax = this.Z.containsKey("a");
        if (this.Z.containsKey("c")) {
            a.v = Integer.parseInt((String)this.Z.get("c"));
        }
        if (this.Z.containsKey("g") && (n2 = Integer.parseInt((String)this.Z.get("g"))) != a.w) {
            a.w = n2;
            a.B();
            f.c();
            Browser.Code.Code();
            Browser.Code.I();
            this.Code.aP = true;
        }
        Browser.Code.Code(this.Z.containsKey("m"));
        if (this.Z.containsKey("l") ^ a.D) {
            Browser.Code.J();
        }
        if ((v = this.Z.get("f")) != null && a.aN != (n = v.equals("socket://") * -1 + 2)) {
            a.aN = n;
            a.ba = true;
            a.aO = -1;
        }
        if ((string = (String)this.Z.get("k")) != null) {
            try {
                int n3 = Integer.parseInt(string);
                int n4 = a.Code(false, false).get(11);
                int n5 = a.Code(false, false).get(12);
                a.x = (n3 >> 1) - n4;
                if ((n3 & 1) == 0 && n5 < 30 || (n3 & 1) == 1 && n5 >= 30) {
                    a.y = 0;
                } else {
                    a.y = 30;
                    if (n5 >= 30) {
                        --a.x;
                    }
                }
            }
            catch (NumberFormatException numberFormatException) {}
        }
        if (this.g) {
            a.o = this.Z.containsKey("u") ^ 1;
            a.i();
        }
        Browser.Code.e();
        a.Code(false);
    }

    public final Object[] Code() {
        this.Code();
        return this.I;
    }

    public final void Code() {
        this.Code[0] = this.W;
        this.Code[1] = this.X;
        this.Code[2] = this.j;
        this.Code[3] = this.k;
        this.Code[4] = this.I() ? 1 : 0;
        this.Code[5] = this.k();
        this.I[0] = this.Code;
        this.I[1] = this.J;
        this.I[2] = this.Code;
        this.I[3] = this.B;
        this.I[4] = this.I;
        this.I[7] = this.Code;
        if (this.Code() && this.I[this.n] == 76) {
            int n = this.n;
            int n2 = ++n;
            ++n;
            n = 10 * (this.I[n2] & 0xFF) + n;
            n += a.Code(this.I, n);
            this.I[9] = a.Code(this.I, n += 2);
        }
        if (!this.C) {
            this.I[8] = this;
        }
    }

    private void Code(String string, String string2, boolean bl) throws IOException {
        this.Code(string2, "", bl << 1, 0, true);
        int n = this.z;
        this.D += 2;
        this.z = this.D;
        this.Code(string, 0, 0);
        this.z = n;
        this.C(5);
    }

    public final Object[] I() {
        this.I = new Object[11];
        this.Code = new int[6];
        this.Code();
        return this.I;
    }

    public final boolean Code() {
        return a.Code(this.Code) != -1;
    }

    private void Code(String string, int n) {
        this.Code(string, a.Code(n));
    }

    private void C(int n, int n2) throws IOException {
        this.Code(a.I(n), n2);
    }

    private void Code(int n, int n2, boolean bl, boolean bl2, boolean bl3) throws IOException {
        String string = null;
        if (!bl3) {
            string = a.I(n);
            if (bl) {
                string = "menu:" + string;
            }
            if (bl2) {
                string = "dialog:" + string;
            }
        }
        this.Code(string, n2);
    }

    private void Code(String string, String string2) {
        int n;
        int n2 = 0;
        while (true) {
            if ((n = a.Code(0, string2) + (string != null && a.Code(string, "menu:") ? (a.J(0) + 4) / 3 + 2 : 0) + this.D + this.A) <= this.Code.a) break;
            string2 = string2.substring(0, string2.length() - (n2 * 3 + 1)) + "...";
            n2 = 1;
        }
        this.e = Math.max(this.e, n);
        this.J.addElement(string);
        this.J.addElement(string2);
    }

    private void h() throws IOException {
        int n = 4;
        if (a.Q) {
            n = 12;
        }
        int n2 = a.J(0) + 4;
        int n3 = 0;
        while (n3 < this.J.size()) {
            String string = (String)this.J.elementAt(n3++);
            String string2 = (String)this.J.elementAt(n3++);
            if (string == null) {
                this.Code(string2, 0, 0x999999, this.D, this.E + (n >> 1));
            } else {
                this.Code(string2, 0, 0, this.D, this.E + (n >> 1));
                if (a.Code(string, "menu:")) {
                    this.a(this.e, n2);
                }
                this.Code(76, "", string, false);
                this.Code(1, this.E, this.e - 2, n2);
                this.l();
            }
            this.E += n2;
        }
        this.J = null;
        this.E += -4;
    }

    private void I(Vector vector) throws IOException {
        if (vector == a.Code || vector == a.I) {
            f.I = true;
        }
        this.i();
        int n = this.Code.Code.I();
        if (vector == a.Code) {
            this.J = a.Code(26);
            this.Code(a.Code(27), 0, 0);
        } else if (vector == a.Z) {
            this.J = a.Code(123);
            this.Code(a.Code(124), 0, 0);
        } else if (vector == a.J) {
            this.J = a.Code(150);
            this.Code(a.Code(151), 0, 0);
        } else {
            this.J = a.Code(125);
            this.Code(a.Code(126), 0, 0);
        }
        this.C(5);
        this.Code(a.Code(vector, n), null, null, vector == a.Code ? -1000 - n : (vector == a.I ? -4000 - n : (vector == a.J ? -5000 - n : -2000 - n)), true, false, false, false, 0x808080);
    }

    private void I(int n, int n2, int n3) throws IOException {
        this.t = this.Code.size();
        this.I = this.Code.Code();
        Image image = this.Code(n, 0, 0);
        int n4 = image.getWidth();
        int n5 = image.getHeight();
        this.Code(n2, n3, n4, n5);
        this.I(n, n2, n3, n4, n5);
    }

    private void b(int n, int n2) throws IOException {
        if (this.Code(n)) {
            this.k();
        }
        this.e = Math.max(this.e, this.D + n + this.A);
        this.az = Math.max(this.az, n2);
        this.Code(this.D, this.E, n, n2);
    }

    private int Code(int n, int n2, int n3, int n4, int n5, int n6, String string, String string2, int n7, int n8) throws IOException {
        this.I.write(70);
        Code.Code(this.I, n, n2, n3, n4);
        this.I.writeInt(n8);
        this.I.write(n5);
        this.I.write(n6);
        this.I.writeUTF(string);
        this.I.writeUTF(string2);
        a.Code(this.I, n7);
        n8 = this.Code.size();
        this.Code.write(70);
        Code.Code(this.Code, n, n2, n3, n4);
        this.Code.writeInt(0);
        this.Code.write(n5);
        this.Code.write(n6);
        this.Code.writeUTF(string);
        this.Code.writeUTF(string2);
        a.Code(this.Code, n7);
        return n8;
    }

    public final void Code(Vector objectArray) {
        if (objectArray2.Z.get("a") != null) {
            Code.Code[1] = objectArray2.Z.get("a");
        }
        if (objectArray2.Z.get("b") != null) {
            Code.Code[0] = objectArray2.Z.get("b");
        }
        if (((String)Code[1]).length() == 0) {
            Code.Code[1] = a.Code(72);
        }
        Object[] objectArray2 = a.Code(objectArray2.Code(), "s");
        Object[] objectArray3 = F == -1 ? null : a.Code((Vector)objectArray, F);
        if (objectArray3 != null && objectArray3[1].equals(Code[1]) && objectArray3[0].equals(Code[0]) && (objectArray3[2] == null && Code[2] == null || objectArray3[2] != null && objectArray3[2].equals(Code[2]) || Code[2] != null && Code[2].equals(objectArray3[2])) && objectArray == a.J && objectArray2 == null) {
            return;
        }
        if (objectArray == a.I) {
            int n = a.Code(objectArray3, 5);
            Code.Code[5] = n == 97 || n == 83 && a.Code(objectArray3) ? new Integer(97) : new Integer(65);
            Code.Code[6] = new Integer(28);
        } else if (a.Code(Code, 5) != 97 && objectArray != a.J) {
            Code.Code[5] = new Integer(77);
            Code.Code[6] = new Integer(31);
        }
        if (objectArray2 != null && objectArray == a.J) {
            a.J.removeElementAt(F);
            a.J.insertElementAt(Code, 0);
            a.Code(a.J, true);
            return;
        }
        if (F == -1) {
            objectArray2 = Code;
            if (objectArray != null && objectArray2 != null) {
                objectArray.addElement(objectArray2);
                a.Code((Vector)objectArray, true);
            }
            return;
        }
        Object[] objectArray4 = objectArray;
        int n = F;
        objectArray = Code;
        objectArray3 = objectArray4;
        if (objectArray4 != null) {
            if (n + 1 > objectArray3.size()) {
                objectArray3.setSize(n + 1);
            }
            objectArray3.setElementAt(objectArray, n);
            a.Code((Vector)objectArray3, true);
        }
    }

    private void C(boolean bl) throws IOException {
        int n = f.I.size();
        int n2 = 0;
        if (bl) {
            n2 = n - Math.min(3, n);
        }
        --n;
        while (n >= n2) {
            this.Code((Object[])f.I.elementAt(n), "history:" + n + ":", null, -3000 - n, false, bl ^ true, false, false, 18858);
            --n;
        }
    }

    private void I(int n, int n2, int n3, int n4, int n5) throws IOException {
        this.I.write(73);
        Code.Code(this.I, n2, n3, n4, n5);
        this.I.writeInt(0);
        this.I.write(0);
        this.I.write(0);
        this.I.write(0);
        a.Code(this.I, n);
    }

    private boolean Code(int n) {
        return this.D + n + this.A > this.Code.a;
    }

    private void I(String string, String string2) throws IOException {
        this.k();
        this.B(a.J(0) + 1);
        if (string2 != null) {
            this.Code(76, string, string2, false);
            this.Code(string, 1, a.f * 18858);
            this.l();
        } else {
            this.Code(string, 1, 0);
        }
        this.C(1);
        this.j();
        this.C(2);
    }

    private String B() {
        String string = this.J();
        if (string != null && string.length() > 0 && string.charAt(0) == '\u0000') {
            return this.Z + string.substring(1, string.length());
        }
        return string;
    }

    private synchronized void Code(Integer n, Image image) {
        if (image != null) {
            this.Code.put(n, image);
            return;
        }
        this.I.put(n, n);
    }

    private void Code(Object[] objectArray, String string, String string2, int n, boolean bl, boolean bl2, boolean bl3, boolean bl4, int n2) throws IOException {
        if (string2 == null) {
            string2 = "";
        }
        if (string == null) {
            string = "";
        }
        if (objectArray != null) {
            string2 = string2 + (String)objectArray[1];
            string = string + (String)objectArray[0];
        }
        ++this.D;
        int n3 = 0;
        if (bl2 && Code.Code(objectArray) != null) {
            n3 = 1;
        }
        if (n != 0 && this.Code(n, 0, 0) != null) {
            this.Code(n, false, n3);
        } else {
            this.D += f.k;
        }
        this.D += 4;
        this.Code(a.Code(n3, string2, this.f() - this.D, null), n3, n2);
        if (bl4) {
            this.a(this.f(), a.J(0) + 2);
            this.D = this.f();
        }
        if (!bl) {
            this.Code(76, string2, string, bl3);
            this.Code(this.z, this.E, this.D, this.az);
            this.l();
        }
        this.k();
    }

    private void Z(String string, String object) throws IOException {
        Code code = object2;
        Object object2 = object;
        object = code;
        code.Code(76, (String)object2, string, false);
        super.Code((String)object2, 0, 18858);
        super.l();
    }

    private int g() throws IOException {
        this.aC = 0;
        this.x = this.Code.size();
        a.Code(this.Code, 0);
        return this.x;
    }

    private void j() throws IOException {
        this.Code(0, this.E++, this.f() + this.z + this.A, 1, -3881788);
        this.Code(0, this.E++, this.f() + this.z + this.A, 1, -3092272);
        this.Code(0, this.E++, this.f() + this.z + this.A, 1, -2171170);
        this.Code(0, this.E++, this.f() + this.z + this.A, 1, -1381654);
        this.Code(0, this.E++, this.f() + this.z + this.A, 1, -526345);
    }

    private void B(int n) throws IOException {
        int n2 = 200;
        this.Code(0, this.E, this.f() + this.z + this.A, n + 1, -986896);
        for (int i = this.f() + this.z + this.A; i > 0 && n2 <= 240; --i, ++n2) {
            this.Code(i - 1, this.E, 1, n + 1, 0xFF000000 | n2 << 16 | n2 << 8 | n2);
        }
    }

    private int Z(int n) {
        return (n << 8) / this.g;
    }

    private void a(int n, int n2) throws IOException {
        for (int i = 1; i < n2 / 3; ++i) {
            this.Code(n - i - this.A, this.E + (n2 >> 1) - i + 1, 1, (i << 1) - 1, -16777216);
        }
    }

    public final boolean I() {
        return this.g != 256 && 256 * this.Code.c / this.e <= this.g;
    }

    private int Code(byte[] byArray) throws IOException {
        int n = byArray.length;
        Code code = this;
        int n2 = code.Code.size();
        code.Code.writeShort(n);
        code.Code.write(byArray, 0, n);
        return n2;
    }

    private void k() {
        this.D = this.z;
        this.E += this.az + this.y;
        this.az = 0;
    }

    private void C(int n) {
        this.k();
        this.E += n;
    }

    private synchronized String Code(int n, int n2) {
        this.M = n;
        if (this.I[n] != n2) {
            return "";
        }
        this.m();
        return this.B();
    }

    public final Image Code() {
        if ((this.Code == null || this.Code.getHeight() != f.k) && this.Code != null) {
            Object[] objectArray = new Object[3];
            a.Code(this.Code, 0, this.Code.length, 256, objectArray);
            if (objectArray[0] != null) {
                if ((Integer)objectArray[2] != f.k) {
                    objectArray[0] = a.Code((int[])objectArray[0], (int)((Integer)objectArray[1]), (int)((Integer)objectArray[2]), f.k, 1, true);
                    objectArray[1] = new Integer(f.k);
                    objectArray[2] = new Integer(f.k);
                }
                int n = f.j - f.k >> 1;
                this.Code = a.Code(objectArray, Browser.Code.I, -n);
            }
        }
        return this.Code;
    }

    private int h() {
        this.M += 3;
        return ((this.I[this.M - 3] & 0xFF) << 16) + ((this.I[this.M - 2] & 0xFF) << 8) + (this.I[this.M - 1] & 0xFF);
    }

    private void b(int n) {
        switch (n) {
            case 84: {
                this.M += 5;
                this.M = this.i() + this.M;
                return;
            }
            case 73: {
                this.M += 10;
                return;
            }
            case 66: {
                this.M += 4;
                return;
            }
            case 70: {
                this.M += 6;
                this.M = this.i() + this.M;
                this.M = this.i() + this.M + 3;
                return;
            }
            case 83: {
                this.M = this.h() + this.M;
                return;
            }
            case 76: {
                this.M += 9;
                return;
            }
            case 77: {
                ++this.M;
                this.M = this.h() + this.M;
            }
        }
    }

    public final synchronized void B(boolean bl) {
        if (!this.a()) {
            return;
        }
        this.b(bl);
        this.M = this.n;
        this.w = this.I[this.M];
        this.b = null;
        int string = this.m();
        switch (this.w) {
            case 76: {
                this.Code.Code(this.B(), this.Code, this.Code(), this.J);
                return;
            }
            case 83: {
                this.a = this.J();
                this.n = true;
                this.M = string;
                this.Code(this.J(), true);
                return;
            }
            case 73: {
                String string2;
                this.M += 2;
                this.M = this.h();
                this.M += 15;
                this.w = this.j();
                string = this.j();
                this.a = this.J();
                CharSequence n = this.J();
                if ((string & 8) != 0) {
                    n = "";
                }
                String string3 = string2 = (String)this.Z.get(this.a);
                if (this.a.equals("http://m.yahoo.com/opera/search?p=")) {
                    Browser.Code.I(a.I(109));
                    return;
                }
                if (this.a.equals("$")) {
                    Browser.Code.I("dialog:" + a.I(54));
                    return;
                }
                if (string3 == null) {
                    string3 = n;
                }
                int n2 = 0;
                switch (string & 0x30) {
                    case 16: {
                        n2 = 2;
                        break;
                    }
                    case 32: {
                        n2 = 3;
                        break;
                    }
                    case 48: {
                        n2 = 4;
                    }
                }
                this.n = string & 1;
                switch (this.w) {
                    case 120: {
                        if (n2 == 4) {
                            if (a.B() != 0 && a.Code(this.Code) == 65) {
                                this.Code.g();
                                Browser.Code.Code("", null, 16, this.Code);
                                return;
                            }
                            Browser.Code.J(string3);
                            return;
                        }
                        f.Code(null, string3, n2, this.b != null && this.b.length() > 0);
                        return;
                    }
                    case 112: {
                        if (a.p) {
                            f.Code(null, string3, n2, false);
                            return;
                        }
                        f.Code(null, string3, n2 | 0x10000, false);
                        return;
                    }
                    case 97: {
                        f.Code(null, string3, 0, false);
                        return;
                    }
                    case 99: {
                        this.Code((String)(string2 != null ? null : n), false);
                        return;
                    }
                    case 114: {
                        this.Code((String)n, false);
                        return;
                    }
                    case 115: {
                        n = new StringBuffer();
                        a.Code(((StringBuffer)n).append("a="), string3);
                        ((StringBuffer)n).append("&b=").append(string & 4);
                        this.x = this.h();
                        this.Code.Code("dialog:" + a.I(112), this.Code, ((StringBuffer)n).toString());
                        return;
                    }
                    case 102: {
                        new C(this).start();
                        return;
                    }
                    default: {
                        return;
                    }
                }
            }
            case 119: {
                if (a.X) {
                    this.j = true;
                    String code = this.B();
                    new C(this, code).start();
                    return;
                }
            }
            case 80: 
            case 87: 
            case 105: {
                String string4 = this.B();
                g g2 = new g(6);
                new g(6).Code = string4;
                a.Code(g2);
                return;
            }
            case 78: {
                this.M += 2;
                this.Code.Code(this.i(), this.h(), this.J());
                return;
            }
            case 67: {
                Code code = this;
                int n = code.i() + code.M;
                while (code.M < n) {
                    int n3 = code.j();
                    int n4 = code.i();
                    code.I[n4 * 6 + 4] = code.I[n4 * 6 + 4] & 0xFFFFFFFE | n3;
                }
                code.r();
                code.s();
                this.n = this.Code(this.j, this.k, 1, 1, 0, false);
            }
        }
    }

    private int Code(String string, String string2, int n, int n2, boolean bl) throws IOException {
        int n3 = n2 = n2 == -1 ? 0 : Math.max(0, a.J(n2) - 12 >> 1);
        if ((n & 2) != 0) {
            n &= 0xFFFFFFFD;
            this.Z.put(string, string2);
        }
        if (bl) {
            this.b(12, 12 + 2 * n2);
        }
        this.Code(this.D, this.E + n2, 12, 12, -33554432);
        this.Code(this.D + 1, this.E + n2 + 1, 12 - 2, 12 - 2, -16777217);
        int n4 = this.Code(this.D, this.E + n2, 12, 12, 99, n, string, string2, 0, 0);
        if (bl) {
            this.I("", n4);
            this.Code(this.D, this.E + n2, 12, 12);
            this.l();
        }
        this.D += 12 + 1;
        return n4;
    }

    private void Code(String string, String string2, int n) throws IOException {
        if (this.f() - this.D < this.f() / 5) {
            this.k();
            this.C(5);
        }
        int n2 = this.f() - this.D;
        int n3 = a.J(0) + 6;
        this.b(n2, n3);
        this.Code(this.D, this.E, n2, 1, -4868683);
        this.Code(this.D + n2 - 1, this.E + 1, 1, n3 - 1, -10461088);
        this.Code(this.D + 1, this.E + n3, n2 - 1, 1, -10461088);
        this.Code(this.D, this.E, 1, n3 + 1, -4868683);
        int n4 = this.D + n2 - (n3 >> 1) - 6;
        int n5 = this.E + (n3 >> 1) - 1;
        for (int i = 7; i > 0; i -= 2) {
            this.Code(n4, n5, i, 1, -16777216);
            ++n4;
            ++n5;
        }
        int n6 = this.Code(this.D + 4, this.E + 3, n2 - 24, n3 - 2, 115, 0, string, string2, n, 0);
        this.I("", n6);
        this.Code(this.D + 1, this.E + 1, n2 - 2, n3 - 2);
        this.l();
        this.D += n2;
    }

    private void Code(int n, int n2, int n3, int n4) throws IOException {
        this.e = Math.max(this.e, n + n3);
        this.f = Math.max(n2 + n4, this.f);
        if (this.aA > 0) {
            Code.Code(this.Z, n, n2, n3, n4);
        }
    }

    private void Code(int n, String string, String string2, boolean bl) throws IOException {
        if (this.I[9] != null && this.I[9].equals(string2)) {
            this.o = this.aB;
        }
        ++this.aB;
        this.Z.write(n);
        if (bl) {
            this.l = this.Z.size();
        }
        this.Z.writeUTF(string);
        this.Z.writeUTF(string2);
        this.aA = this.Z.size();
        this.Z.write(0);
    }

    private int f() {
        return this.Code.a - this.z - this.A;
    }

    public final int e() {
        return this.Code.Code() - d;
    }

    private static void Code(DataOutputStream dataOutputStream, int n, int n2, int n3, int n4) throws IOException {
        dataOutputStream.writeShort(n);
        a.Code(dataOutputStream, n2);
        dataOutputStream.writeShort(n3);
        a.Code(dataOutputStream, n4);
    }

    private void I(String string, int n) throws IOException {
        ++this.aB;
        this.Z.write(73);
        this.Z.writeUTF(string);
        this.Z.writeShort(3);
        a.Code(this.Z, n);
        this.aA = this.Z.size();
        this.Z.write(0);
    }

    private void l() throws IOException {
        this.Z.Code()[this.aA] = (byte)((this.Z.size() - this.aA) / 10);
        this.aA = 0;
    }

    private void Code(int n, int n2, int n3, int n4, int n5) throws IOException {
        this.Code(n, n2, n3, n4);
        this.I.write(66);
        Code.Code(this.I, n, n2, n3, n4);
        this.I.writeInt(n5);
    }

    private void Code(String string, int n, int n2) throws IOException {
        int n3;
        int n4 = string.indexOf(10);
        if (n4 >= 0) {
            if (n4 > 0) {
                this.Code(string.substring(0, n4), n, n2);
            }
            this.k();
            this.Code(string.substring(n4 + 1, string.length()), n, n2);
            return;
        }
        n4 = a.Code(n, string);
        if (n4 > (n3 = this.Code.a - Math.min(this.D, this.z) - this.A)) {
            n4 = string.lastIndexOf(32, string.length());
            while (n4 > 0 && a.Code(n, string.substring(0, n4)) > n3) {
                n4 = string.substring(0, n4).lastIndexOf(32);
            }
            if (n4 > 0 && n4 != string.length()) {
                this.Code(string.substring(0, n4), n, n2);
                this.k();
                this.Code(string.substring(n4 + 1), n, n2);
                return;
            }
            if (this.Code(a.Code(n, string.substring(0, 3)))) {
                this.k();
            }
            n4 = this.Code.a - this.A - this.D;
            char[] cArray = string.toCharArray();
            n4 = Math.max(a.I(n, cArray, cArray.length, n4), 1);
            this.Code(string.substring(0, n4), n, n2);
            this.Code(string.substring(n4), n, n2);
            return;
        }
        n3 = a.J(n);
        this.b(n4, n3);
        this.I.write(84);
        Code.Code(this.I, this.D, this.E, n4, n3);
        this.I.writeInt(n2);
        this.I.write(a.Code(n));
        this.I.writeUTF(string);
        this.D += n4;
    }

    private void Code(String string, int n, int n2, int n3, int n4) throws IOException {
        int n5 = a.Code(n, string);
        int n6 = a.J(n);
        this.Code(n3, n4, n5, n6);
        this.I.write(84);
        Code.Code(this.I, n3, n4, n5, n6);
        this.I.writeInt(n2);
        this.I.write(a.Code(n));
        this.I.writeUTF(string);
    }

    private void t() {
        if (!this.C()) {
            this.n = this.Code(this.j, this.k, 1, 1, 0, false);
        }
    }

    public final synchronized String I() {
        if (this.n <= 0) {
            return null;
        }
        this.M = this.n;
        byte by = this.I[this.M];
        this.m();
        if (by == 76) {
            return this.B();
        }
        return null;
    }

    private int m() {
        ++this.M;
        int n = this.M = 10 * this.j() + this.M;
        this.b = this.J();
        return n;
    }

    private boolean a() {
        return this.n > 0 && this.Code.I == null && !this.j && (!a.X || Browser.Code.Code.Code <= -1);
    }

    public final int Z() {
        if (this.Code != null) {
            return this.Code.B;
        }
        return 0;
    }

    public final void b() {
        if (this.I()) {
            if (this.k >= this.f - this.Code.Code()) {
                this.Code(0, (this.Code.Code() >> 1) - this.k, false);
            } else if (this.I() && this.k < this.al) {
                this.Code(this.ak + (this.Code.c >> 1) - this.j, this.al + (this.Code.Code() >> 1) - this.k, false);
            } else {
                this.Code(0, this.f - (this.Code.Code() >> 1) + this.Z(1) - this.k, false);
            }
            Browser.Code.b = false;
            return;
        }
        if (this.X >= this.f - this.Code.Code()) {
            this.Z(false);
        } else if (this.X < this.al) {
            this.Code(this.ak - this.W, this.al - this.X, false, false);
        } else {
            this.Code(0, this.f, false, false);
        }
        Browser.Code.b = true;
    }

    public final void Z(boolean bl) {
        if (bl) {
            this.n = this.l;
        }
        this.Code(0, -this.f, false, false);
    }

    public final int d() {
        return this.Code.c;
    }

    public final void a() {
        if (this.I() && this.k >= this.am) {
            this.I(false);
            return;
        }
        this.B(false);
    }

    public final void J(boolean bl) {
        if (this.Y != this.g) {
            this.s();
        }
        this.g = f.Code(this.g, this.Y, this.ae, this.af);
        if (a.aj) {
            this.g = this.Y;
        }
        if (!(this.C() || this.aq == 0 && this.ar == 0)) {
            if (bl) {
                int n;
                int n2;
                int n3 = (int)System.currentTimeMillis() - this.an;
                int n4 = this.ao + this.aq * this.Z((n3 >> 1) + n3 * n3 / 5000) - this.j;
                if (Math.abs(n4) > this.Z(this.Code.c >> 1)) {
                    n4 = this.aq * this.Z(this.Code.c >> 1);
                }
                if (Math.abs(n2 = this.ap + this.ar * this.Z((n3 >> 1) + n3 * n3 / 5000) - this.k) > this.Z(this.Code.Code() >> 1)) {
                    n = this.ar * this.Z(this.Code.Code() >> 1);
                }
                if (this.k < this.am && this.k + n >= this.am) {
                    this.ao = this.j = this.Z(this.j);
                }
                if (this.am > 0 && this.k >= this.am && this.k + n < this.am) {
                    this.ao = this.j = this.I(this.j);
                }
                this.Code(n4, n, false);
                this.U = this.W;
                this.V = this.X;
                f.b();
            } else {
                this.Code(0, 0, true);
                this.aq = 0;
                this.ar = 0;
            }
        }
        this.U = Math.max(0, Math.min(this.e - this.Z(this.Code.c), f.Code(this.U, this.W, this.aa, this.ab)));
        this.V = Math.max(0, Math.min(this.f - this.Z(this.Code.Code()), f.Code(this.V, this.X, this.ac, this.ad)));
        if (this.k < this.am) {
            this.j = Math.min(this.j, this.I(this.e - 1));
        }
    }

    public final void I(int n, int n2) {
        if (n2 != 0) {
            this.Code(0, (this.B() - this.Code.Code()) * n / n2 - this.a(), false, false);
            this.V = this.X;
        }
    }

    private String J() {
        String string = a.Code(this.I, this.M);
        this.M = this.i() + this.M;
        return string;
    }

    private void u() {
        this.M = this.i() + this.M;
        this.M = this.i() + this.M;
    }

    public final int b() {
        if (this.k < this.am) {
            return this.j - this.C();
        }
        return this.I(this.j - this.U);
    }

    private void Code(int n, boolean bl, int n2) throws IOException {
        this.t = this.Code.size();
        this.I = this.Code.Code();
        Image image = this.Code(n, 0, 0);
        int n3 = image.getWidth();
        int n4 = image.getHeight();
        if (bl) {
            this.D = (this.Code.a >> 1) - (n3 >> 1);
            this.D = Math.max(0, this.D);
        }
        if (n2 >= 0) {
            this.E += a.J(n2) - n4 >> 1;
        }
        this.b(n3, n4);
        this.I(n, this.D, this.E, n3, n4);
        if (n2 >= 0) {
            this.E -= a.J(n2) - n4 >> 1;
        }
        this.D += n3;
    }

    private void Code(String string, String string2, byte[] byArray, int n, int n2) throws IOException {
        int n3 = this.f();
        int n4 = n2;
        int n5 = n;
        String string3 = string2;
        String string4 = string;
        Code code = this;
        int n6 = a.J(0);
        int n7 = 0;
        int n8 = 0;
        int n9 = 0;
        int n10 = 0;
        if (byArray != null && byArray.length == 0) {
            byArray = null;
        }
        if (byArray != null) {
            n10 = code.Code(byArray);
            code.t = code.Code.size();
            code.I = code.Code.Code();
            Image image = code.Code(n10, 0, 0);
            n7 = image.getWidth() + 3;
            n8 = Math.max(0, image.getHeight() - n6 >> 1);
            n9 = Math.max(0, n6 - image.getHeight() + 1 >> 1);
            n6 = Math.max(n6, image.getHeight());
        }
        code.b(n3, n6 += 5);
        code.Code(code.D, code.E, n3, n6, -1);
        code.Code(code.D, code.E + n6 - 1, n3, 1, -2171170);
        code.Code(code.D, code.E + 2, 1, n6 - 2, -4276546);
        code.Code(code.D + 1, code.E + 2, 1, n6 - 3, -2171170);
        code.Code(code.D + 2, code.E + 2, 1, n6 - 3, -1118482);
        code.Code(code.D, code.E, 1, 1, -12961222);
        code.Code(code.D, code.E + 1, 1, 1, -6842473);
        code.Code(code.D + n3 - 1, code.E, 1, 1, -13158601);
        code.Code(code.D + 1, code.E, n3 - 2, 1, -9408400);
        code.Code(code.D + 1, code.E + 1, n3 - 2, 1, -4276546);
        code.Code(code.D + 2, code.E + 2, n3 - 3, 1, -1118482);
        code.Code(code.D + n3 - 1, code.E + 2, 1, n6 - 3, -4276546);
        code.Code(code.D + n3 - 1, code.E + 1, 1, 1, -6842473);
        if (byArray != null) {
            code.I(n10, code.D + 3, code.E + 3 + n9);
        }
        if ((n5 & 0x80) != 0) {
            int n11 = code.D + n3 - (n6 >> 1) - 6;
            n9 = code.E + (n6 >> 1) - 1;
            for (n10 = 7; n10 > 0; n10 -= 2) {
                code.Code(n11, n9, n10, 1, -16777216);
                ++n11;
                ++n9;
            }
        }
        n4 = code.Code(code.D + 3 + n7, code.E + 3 + n8, n3 - 4 - n7 - ((n5 & 0x80) != 0 ? n6 : 0), n6 - 4, 120, n5, string4, string3, 0, n4);
        code.I("", n4);
        code.Code(code.D, code.E, n3, n6);
        code.l();
        code.D += n3;
    }

    public final int Code(String string) {
        if (string == null || string.length() == 0) {
            return 0;
        }
        this.Code.Code = string;
        string = string.toLowerCase();
        this.Z = new Vector();
        this.M = this.s;
        while (this.M < this.I.length) {
            int n = this.j();
            if (n == 84 || n == 66 || n == 73 || n == 70) {
                this.p();
            }
            if (n == 84) {
                int n2;
                n = this.M;
                int n3 = 0;
                int n4 = 0;
                int n5 = 0;
                int n6 = 0;
                this.M += 4;
                int n7 = a.I(this.j());
                String string2 = this.J();
                String string3 = string2.toLowerCase();
                if (string3.indexOf(string) == -1) continue;
                for (n2 = 0; n2 < string2.length(); ++n2) {
                    if (string2.charAt(n2) != '\n') continue;
                    ++n6;
                }
                for (n2 = 0; n2 < string3.length(); ++n2) {
                    int[] nArray;
                    int n8;
                    int n9;
                    if (string3.charAt(n2) == '\n') {
                        n3 = n2 + 1;
                        ++n5;
                        n4 = 0;
                        continue;
                    }
                    n4 = n4 < string.length() && string3.charAt(n2) == string.charAt(n4) ? ++n4 : 0;
                    if (n4 != string.length()) continue;
                    n4 = a.Code(n7, string2.substring(n3, n2 - string.length() + 1));
                    int n10 = a.Code(n7, string2.substring(n3, n2 + 1));
                    int[] nArray2 = new int[]{n4, this.L / (n6 + 1) * n5, n10 - n4, this.L / (n6 + 1), this.G, this.H, n};
                    n10 = nArray2[1] + nArray2[5];
                    for (n9 = this.Z.size() - 1; n9 > -1 && n10 < (n8 = (nArray = (int[])this.Z.elementAt(n9))[1] + nArray[5]); --n9) {
                    }
                    this.Z.insertElementAt(nArray2, n9 + 1);
                    n4 = 0;
                }
                continue;
            }
            this.b(n);
        }
        this.B = new Hashtable();
        for (int i = 0; i < this.Z.size(); ++i) {
            Object object = (int[])this.Z.elementAt(i);
            object = new Integer(object[6]);
            int[] nArray = new int[]{i};
            if (this.B.containsKey(object)) {
                int[] nArray3 = (int[])this.B.get(object);
                int[] nArray4 = new int[nArray3.length + 1];
                nArray = nArray4;
                nArray4[nArray3.length] = i;
                System.arraycopy(nArray3, 0, nArray, 0, nArray3.length);
            }
            this.B.put(object, nArray);
        }
        return this.Z.size();
    }

    public final synchronized boolean B() {
        if (this.n == 0 || this.I[this.n] != 73) {
            return false;
        }
        int n = this.M;
        this.M = this.n;
        this.m();
        this.M += 2;
        this.M = this.h();
        this.M += 15;
        int n2 = this.j();
        this.M = n;
        return n2 == 120 || n2 == 97 || n2 == 112 || n2 == 102 || n2 == 115;
    }

    public final int Code() {
        if (this.b == 2) {
            return 4375;
        }
        return this.v;
    }

    public final void Code(String string, int n, Object object) {
        if (this.J == null) {
            this.J = new Hashtable();
        }
        this.J.put(this.a, new Object[]{string, new Integer(n), object});
        this.Code(string, false);
    }

    public final void I(int n) {
        int n2 = this.Z.size();
        if (n2 == 0) {
            return;
        }
        if (1 < 0) {
            n = n2 - 1;
        }
        this.T += n;
        this.T %= n2;
        if (this.T < 0) {
            return;
        }
        int[] nArray = (int[])this.Z.elementAt(this.T);
        n2 = nArray[0] + (nArray[2] >> 1) + nArray[4];
        int n3 = nArray[1] + nArray[3] + nArray[5] - 1;
        if ((this.I[this.Z(n3, 0) * 6 + 4] & 1) == 0) {
            this.n = this.Code(0, this.J(n3), this.e, 10, this.n, false);
            if (this.n != -1 && this.I[this.n] == 67) {
                this.B(false);
            }
        }
        n3 = this.J(n3);
        int n4 = this.q;
        this.q = 0;
        this.Code(n2 - this.j, n3 - this.k, true);
        this.q = n4;
        if (this.I()) {
            this.I(false);
        }
        this.s();
    }

    private int J(int n) {
        int n2 = this.Z(n, 0);
        if ((this.I[n2 * 6 + 4] & 1) == 0) {
            return -1;
        }
        return n - this.I[n2 * 6] + this.I[n2 * 6 + 5];
    }

    public final void run() {
        try {
            this.a(1);
            this.c(0, 0);
            this.a(true);
            return;
        }
        catch (Throwable throwable) {
            return;
        }
    }

    public static void Code(Code code) {
        code.n();
    }

    private void J(String string, String string2) throws IOException {
        ++this.aC;
        this.Code.writeUTF(string);
        this.Code.writeUTF(string2);
    }

    private void m() throws IOException {
        byte[] byArray = this.Code.Code();
        a.I(this.aC, byArray, this.x);
    }

    private void n() {
        this.a(5);
    }

    private synchronized void b(boolean bl) {
        int n;
        if (!this.C() || bl) {
            return;
        }
        int n2 = -1;
        int n3 = -1;
        int n4 = 0;
        this.M = this.n + 1;
        for (int i = this.j(); i > 0; --i) {
            this.p();
            this.H = this.J(this.H);
            int n5 = Math.min(this.H + this.L, this.V + this.Code.Code()) - Math.max(this.H, this.V);
            if (n5 <= n4) continue;
            n4 = n5;
            n = this.G + (this.K >> 1);
            n3 = this.H + (this.L >> 1);
        }
        this.j = n;
        this.k = n3;
    }

    public final void J() {
        this.a(7);
        this.a(false);
    }

    private synchronized int Code(int n) {
        this.M = n;
        block11: while (this.M < this.I.length && this.t == this.I.length || this.M < this.t - 16) {
            int n2 = this.M;
            int n3 = this.j();
            block0 : switch (n3) {
                case 83: {
                    this.M = this.h() + this.M;
                    break;
                }
                case 76: {
                    n = this.h();
                    if (this.C() && n > 0) {
                        this.n = n;
                    }
                    this.l = this.h();
                    this.m = this.h();
                    if (this.Code == null || this.Code.Code() || this.I == null || this.I.equals(a.k)) break;
                    this.n = 0;
                    this.l = 0;
                    this.m = 0;
                    break;
                }
                case 77: {
                    int n4;
                    n2 = this.j();
                    n3 = this.h();
                    if (this.M + n3 > this.t) {
                        return n;
                    }
                    switch (n2) {
                        case 115: {
                            this.Code = this.j();
                            this.I = this.j();
                            break;
                        }
                        case 67: {
                            this.h = (n3 -= 8) / 6;
                            this.I = new int[(this.i() << 1) + this.h * 7];
                            this.i = this.j();
                            this.ak = this.i();
                            this.al = this.h();
                            n = 0;
                            if (this.Code != null && this.Code.indexOf(35) != -1) {
                                n = 1;
                            }
                            if (this.e && !this.Code) {
                                if (this.I()) {
                                    this.Code(this.ak + (this.Code.c >> 1) - this.j, this.al + (this.Code.Code() >> 1) - this.k, true);
                                } else if (this.Z() || !a.aC || n != 0) {
                                    this.Code(this.ak - this.W, this.al - this.X, false, true);
                                }
                                this.c();
                            }
                            n = this.h * 6;
                            n2 = 0;
                            n3 = 0;
                            while (n3 / 6 < this.h) {
                                n4 = this.h();
                                this.I[n3++] = n2;
                                this.I[n3++] = n4;
                                this.I[n3++] = n;
                                int n5 = ++n3;
                                ++n3;
                                this.I[n5] = this.j();
                                ++n3;
                                n += 2 * this.i() + 1;
                                n2 += n4;
                            }
                            this.r();
                            break;
                        }
                        case 84: {
                            this.p = n3 / 12;
                            this.Z = new int[n3];
                            for (n = 0; n < this.p << 2; ++n) {
                                this.Z[n] = this.h();
                            }
                            break block0;
                        }
                        case 83: {
                            this.Code = new String[7];
                            for (n = 0; n < 7; ++n) {
                                this.Code[n] = this.J();
                            }
                            break block0;
                        }
                        default: {
                            this.M += n3;
                            break;
                        }
                    }
                    break;
                }
                default: {
                    this.p();
                    this.b(n3);
                    if (this.M > this.t) break block11;
                    if (!this.d) {
                        this.G = (short)this.G + this.aE;
                        this.H = (this.H << 8 >> 8) + this.aF;
                        a.Code(this.G, this.I, n2 + 1);
                        a.I(this.H, this.I, n2 + 3);
                        this.aE = this.G;
                        this.aF = this.H;
                    }
                    n = this.M;
                    Code code = this;
                    int n4 = code.Z(code.H, 0);
                    do {
                        int n6 = code.I[n4 * 6 + 2];
                        while (code.I[n6 + 2] != 0) {
                            n6 += 2;
                        }
                        if (code.I[n6] == 0) {
                            code.I[n6] = n2;
                        } else if (code.I[n6 + 1] + code.i < n2) {
                            code.I[n6 += 2] = n2;
                        }
                        code.I[n6 + 1] = n;
                    } while (++n4 < code.h && code.H + code.L > code.I[n4 * 6]);
                    this.I(this.G, this.H, this.K, this.L);
                }
            }
            n = this.M;
        }
        return n;
    }

    private void Code(DataInputStream dataInputStream) throws IOException {
        int n;
        byte[] byArray = new byte[3];
        int n2 = 0;
        while ((n2 += dataInputStream.read(byArray, n2, 3 - n2)) < 3) {
        }
        this.u = a.Z(byArray, 0);
        for (n = 0; n <= 3; ++n) {
            try {
                this.I = new byte[this.u];
                break;
            }
            catch (Throwable throwable) {
                f.Code(n);
                continue;
            }
        }
        if (this.I == null) {
            this.I = new byte[this.u];
        }
        this.t = 0;
        n = 0;
        while (this.u > this.t) {
            this.t += dataInputStream.read(this.I, this.t, Math.min(1024, this.u - this.t));
            g.Code(this);
            if (this.s == 0 && (this.t >= 2000 || this.t == this.u)) {
                this.o();
                n = this.s;
                if (this.I != null && this.I.startsWith("server:gettitle")) {
                    Code.Code[1] = this.J;
                    Code.Code[2] = this.Code;
                    this.Code.Code.C = "keep";
                    this.J();
                    this.Code.c();
                }
                this.c();
                this.a(4);
            }
            if (this.s <= 0) continue;
            n = this.Code(n);
            this.c(1000);
        }
    }

    public final synchronized void Code(int n) {
        if (n >= 1) {
            this.Code.clear();
            return;
        }
        if (n >= 0) {
            Hashtable hashtable = new Hashtable();
            Enumeration enumeration = this.C.keys();
            while (enumeration.hasMoreElements()) {
                Integer n2 = (Integer)enumeration.nextElement();
                Integer n3 = (Integer)this.C.get(n2);
                if (!this.Code.containsKey(n3) || !this.Code((int)n2, false, true)) continue;
                hashtable.put(n3, this.Code.get(n3));
            }
            this.Code = hashtable;
        }
    }

    private void a(boolean bl) {
        try {
            this.Code.Code(bl);
        }
        catch (Throwable throwable) {}
        if (bl) {
            this.Code = null;
        }
    }

    private Image Code(Integer n) {
        int n2;
        int n3 = n2 = (n & 0x40000000) != 0 ? 256 * this.Code.c / this.e : 256;
        if (n < 0) {
            return Browser.Code.Code.Code((Object[])this.I.elementAt(-6000 - n));
        }
        int n4 = n & 0x1FFFFFFF;
        int n5 = a.Code(this.I, n4);
        return a.Code(this.I, n4 + 2, n5, n2, null);
    }

    public static void Code(a object) {
        if (a.T < 160 && a.aG) {
            ((a)object).Code("overlay:" + a.I(40), null, null);
            while (a.aM) {
                a.Code(a.J, 1000);
            }
            Browser.Code.I(f.Code(8));
        }
        if (a.T >= 160 && a.Code[1] == null) {
            byte[] byArray = new byte[32];
            object = byArray;
            a.Code(byArray);
            int n = ((Object)object).length;
            Object object2 = object;
            n = Math.min(n, 128);
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < n; ++i) {
                if ((object2[i] & 0xFF) < 16) {
                    stringBuffer.append('0');
                }
                stringBuffer.append(Integer.toString(object2[i] & 0xFF, 16));
            }
            a.Code(1, stringBuffer.toString(), true);
        }
    }

    private synchronized void o() {
        this.M = 0;
        this.j();
        this.e = this.i();
        this.f = this.h();
        this.j();
        this.j();
        this.j();
        this.i();
        this.J = this.J();
        int n = this.i();
        if (n > 0) {
            byte[] byArray = this.Code = new byte[n];
            Code code = this;
            System.arraycopy(code.I, code.M, byArray, 0, n);
            code.M += n;
        }
        this.Z = this.J();
        this.I = this.Code;
        this.Code = this.B();
        n = this.j();
        this.e = n & 1;
        this.c = (n & 6) >>> 1;
        if (!this.e && this.Code.Code != null && this.Code.Code != this.Code.I) {
            this.U = this.W = this.Code.Code.U;
            this.V = this.X = this.Code.Code.V;
            this.j = this.Code.Code.j;
            this.k = this.Code.Code.k;
            this.I(false);
        }
        this.B = null;
        this.J = new Hashtable();
        this.s = this.M;
        this.q();
    }

    public final void Z(int n, int n2) {
        if (n2 != 0) {
            this.Code((this.e - this.Code.c) * n / n2 - this.W, 0, false, false);
            this.U = this.W;
        }
    }

    private void q() {
        this.I = new int[9];
        this.I[1] = this.f;
        this.I[2] = 6;
        this.I[4] = 3;
        this.h = 1;
    }

    private synchronized void I(Integer n, Image object) {
        this.Code = null;
        if (this.Code.Code != this) {
            return;
        }
        int n2 = 0;
        if (n != null) {
            this.Code(n, (Image)object);
            n2 = object != null ? n : 0;
        }
        object = this.C.keys();
        while (object.hasMoreElements()) {
            Integer n3 = (Integer)object.nextElement();
            Integer n4 = (Integer)this.C.get(n3);
            int n5 = n2 - n4;
            if (!this.Code((int)n3, ((n5 | -n5) >>> 31 ^ 1) != 0, false) || this.Code.containsKey(n4) || this.I.containsKey(n4)) continue;
            int n6 = n4 & 0x20000000;
            if (((n6 | -n6) >>> 31 ^ this.I()) == 0 || !this.Code() && this.t < (n4 & 0x1FFFFFFF) + 2 + a.Code(this.I, n4 & 0x1FFFFFFF)) continue;
            this.Code = n4;
        }
        if (this.Code != null) {
            g.Code(this);
            return;
        }
        if (n != null) {
            g.Code = false;
            this.c(0);
        }
    }

    public final int a() {
        if (this.V < this.am) {
            return this.V;
        }
        return this.I(this.V - this.am) + this.am;
    }

    private int i() {
        this.M += 2;
        return a.Code(this.I, this.M - 2);
    }

    /*
     * Unable to fully structure code
     */
    private synchronized Image Code(int var1_1, int var2_5, int var3_6) {
        block13: {
            block15: {
                block17: {
                    block16: {
                        block14: {
                            var4_8 = 256;
                            if (this.g < 128) {
                                var4_8 = 256 * this.Code.c / this.e;
                            }
                            v0 = var4_8 - 256;
                            var3_7 = new Integer(var1_1 | ((v0 | -v0) >>> 31 ^ 1) * 0x20000000 | (var4_8 == 256 || (var3_6 & 4) != 0 ? 0 : 0x40000000));
                            if (this.I.containsKey(var3_7)) {
                                return null;
                            }
                            if (this.Code.containsKey(var3_7)) {
                                return (Image)this.Code.get(var3_7);
                            }
                            if (var1_1 >= 0 || var1_1 <= -6000) break block13;
                            if (var1_1 <= -1000) break block14;
                            v1 = var1_1 == -41 ? Browser.Code.Z : a.Code(a.Code(-var1_1), (Object)("" + var1_1));
                            break block15;
                        }
                        if (var1_1 <= -3000) break block16;
                        v2 = (byte[])a.Code(var1_1 <= -2000 ? a.Z : a.Code, var1_1 <= -2000 ? -2000 - var1_1 : -1000 - var1_1)[2];
                        v1 = a.Code(v2, (Object)v2);
                        break block15;
                    }
                    if (-3000 - var1_1 >= f.I.size()) break block17;
                    v3 = (byte[])((Object[])f.I.elementAt(-3000 - var1_1))[2];
                    v1 = a.Code(v3, (Object)v3);
                    break block15;
                }
                if (-4000 - var1_1 == -1) ** GOTO lbl-1000
                if (-4000 - var1_1 < a.I.size() && -4000 - var1_1 >= 0) {
                    v4 = (byte[])((Object[])a.I.elementAt(-4000 - var1_1))[2];
                    v1 = a.Code(v4, (Object)v4);
                } else if (-5000 - var1_1 < a.J.size()) {
                    v5 = (byte[])((Object[])a.J.elementAt(-5000 - var1_1))[2];
                    v1 = a.Code(v5, (Object)v5);
                } else lbl-1000:
                // 2 sources

                {
                    v1 = var1_2 = null;
                }
            }
            if (var4_8 != 256) {
                var1_2 = a.Code(var1_2, 0, Code.Code(var1_2.getHeight(), var4_8), 1, true);
            }
            this.Code(var3_7, var1_2);
            return var1_2;
        }
        if (var2_5 == 0) {
            if (var1_1 < -6000) {
                return Browser.Code.Code.Code();
            }
            var1_3 = this.Code(var3_7);
            this.Code(var3_7, var1_3);
            return var1_3;
        }
        if (a.c) {
            if (var1_1 < -6000 || var1_1 + 2 <= this.t && var1_1 + a.Code(this.I, var1_1) <= this.t) {
                var1_4 = this.Code(var3_7);
                this.Code(var3_7, var1_4);
                return var1_4;
            }
            this.s();
            return null;
        }
        this.C.put(new Integer(var2_5 | var3_7 & 0x60000000), var3_7);
        g.Code(this);
        if (var1_1 < -6000) {
            return Browser.Code.Code.Code();
        }
        return null;
    }

    public final void B() {
        if (!this.Z()) {
            this.Y = 256;
            this.c();
            return;
        }
        if (this.g != 256 && 256 * this.Code.c / this.e != this.g) {
            this.Y = 256 * this.Code.c / this.e;
            this.c();
        }
    }

    public final void Code(boolean bl) {
        if (bl) {
            this.Code = null;
        }
        if (this.Code == null) {
            this.I((Integer)null, (Image)null);
            return;
        }
        g.Code = true;
        this.I(this.Code, this.Code(this.Code));
    }

    private int j() {
        return this.I[this.M++] & 0xFF;
    }

    private void r() {
        int n = 0;
        this.am = 0;
        for (int i = 0; i < this.h; ++i) {
            this.I[i * 6 + 5] = n;
            if ((this.I[i * 6 + 4] & 1) == 0) continue;
            n += this.I[i * 6 + 1];
            if ((this.I[i * 6 + 4] & 2) != 0) continue;
            this.am += this.I[i * 6 + 1];
        }
        this.f = n;
    }

    public final void I(boolean bl) {
        if (bl) {
            if (this.Z()) {
                this.Y = 256 * this.Code.c / this.e;
                this.ae = this.g;
                this.af = (int)System.currentTimeMillis();
                g.I = true;
                this.e(0, this.k >= this.am ? this.Code(this.k, -this.Code.Code() >> 1, this.Y) : this.X);
            } else if (!this.I()) {
                this.Y = 256;
                this.c();
            }
        } else {
            this.Y = 256;
            this.ae = this.g;
            this.af = (int)System.currentTimeMillis();
            g.I = true;
            if (this.k >= this.am) {
                int n = this.Code.c;
                int n2 = this.Code.Code();
                n2 = a.Code(this.k - (n2 >> 1), this.am, this.f - n2);
                int n3 = a.Code(this.B(this.J(this.j, n2), this.j - (n >> 1)), 0, this.e - n);
                this.e(n3, n2);
                this.t();
            }
        }
        this.s();
    }

    private void Code(int n, int n2, boolean n3) {
        this.j = a.Code(this.j + n, 0, this.e - 1);
        this.k = a.Code(this.k + n2, 0, this.f - 1);
        this.t();
        n = this.W;
        n2 = -1;
        if (n3 != 0) {
            int n4;
            block2: {
                Code code = this;
                for (n3 = 0; n3 < code.p; ++n3) {
                    if (code.j <= code.q + code.Z[n3 << 2] || code.j > code.Z[(n3 << 2) + 2] - code.q || code.k <= code.Z[(n3 << 2) + 1] || code.k > code.Z[(n3 << 2) + 3]) continue;
                    n4 = n3;
                    break block2;
                }
                n4 = -1;
            }
            n2 = n4;
            n = this.B(n2, n);
        }
        n2 = n2 < 0 ? 40 : 0;
        n = a.Code(n, this.j + n2 - this.Code.c, this.j - n2);
        n2 = a.Code(this.X, this.Code(this.k, 50 - this.Code.Code(), this.g), this.Code(this.k, -50, this.g));
        this.e(n, n2);
    }

    private void e(int n, int n2) {
        n = a.Code(n, 0, this.e - Code.I(this.Code.c, this.Y));
        n2 = a.Code(n2, 0, this.f - Code.I(this.Code.Code(), this.Y));
        if (this.W != n) {
            this.U = a.Code(f.Code(this.U, this.W, this.aa, this.ab), 0, this.e - this.Z(this.Code.c));
            this.W = n;
            this.aa = this.U;
            this.ab = (int)System.currentTimeMillis();
        }
        if (this.X != n2) {
            this.V = a.Code(f.Code(this.V, this.X, this.ac, this.ad), 0, this.f - this.Z(this.Code.Code()));
            this.X = n2;
            this.ac = this.V;
            this.ad = (int)System.currentTimeMillis();
        }
        if (this.I()) {
            g.I = true;
        }
        f.b();
    }

    private int Code(int n, int n2, int n3) {
        if (n < this.am && n + n2 < this.am) {
            return n + n2;
        }
        if (n < this.am) {
            return this.am + Code.I(n2 - (this.am - n), n3);
        }
        if (n + Code.I(n2, n3) < this.am) {
            return this.am + (n2 + Code.Code(n - this.am, n3));
        }
        return n + Code.I(n2, n3);
    }

    private void s() {
        this.I(0, 0, Math.max(this.e, this.Code.c), Math.max(this.f, this.Code.Code()));
    }

    private void p() {
        this.G = this.i();
        this.H = this.h();
        this.K = this.i();
        this.L = this.h();
    }

    public final void Code(int n, int n2) {
        this.G = this.ag;
        this.H = this.J(this.ah);
        this.K = this.ai;
        this.L = this.aj;
        if (this.ai > 0 && this.aj > 0) {
            this.d(n, n2);
            this.H += this.am - this.I(this.am);
            if (this.H < this.am) {
                this.H = 0;
                this.L += this.am;
                this.K += this.ai;
            }
        }
        this.ai = 0;
        this.aj = 0;
    }

    private synchronized void I(int n, int n2, int n3, int n4) {
        if (this.ai > 0 && this.aj > 0) {
            int n5 = this.ag + this.ai;
            this.ag = Math.min(this.ag, n);
            this.ai = Math.max(n + n3, n5) - this.ag;
            n = this.ah + this.aj;
            this.ah = Math.min(this.ah, n2);
            this.aj = Math.max(n2 + n4, n) - this.ah;
            return;
        }
        this.ag = n;
        this.ah = n2;
        this.ai = n3;
        this.aj = n4;
    }

    private boolean Code(int n, boolean bl, boolean bl2) {
        n &= 0x1FFFFFFF;
        int n2 = this.G;
        int n3 = this.H;
        int n4 = this.K;
        int n5 = this.L;
        int n6 = this.M;
        int n7 = this.g;
        int n8 = this.U;
        int n9 = this.V;
        if (bl2) {
            this.g = 256 * this.Code.c / this.e;
            this.U = a.Code(this.U, 0, this.e - Code.I(this.Code.c, this.g));
            this.V = a.Code(this.V, 0, this.f - Code.I(this.Code.Code(), this.g));
        }
        this.M = n;
        this.p();
        this.H = this.J(this.H);
        bl2 = false;
        if (this.H >= 0) {
            this.d(-this.I(this.U), -this.I(this.V));
            bl2 = a.Code(this.G, this.H, this.K, this.L, 0, 0, this.Code.c, this.Code.Code());
            if (bl2 && bl) {
                this.M = n;
                this.p();
                this.I(this.G, this.H, this.K, this.L);
                this.c(1000);
            }
        }
        this.G = n2;
        this.H = n3;
        this.K = n4;
        this.L = n5;
        this.M = n6;
        this.g = n7;
        this.U = n8;
        this.V = n9;
        return bl2;
    }

    public final int B() {
        return this.I(this.f - this.am) + this.am;
    }

    private void c(int n) {
        if (this.ai > 0 && (int)System.currentTimeMillis() - this.ay >= n) {
            this.ay = (int)System.currentTimeMillis();
            this.Code.f();
        }
    }

    public final boolean Code(String string, boolean bl) {
        if (this.a == null) {
            return false;
        }
        String string2 = this.a;
        this.a = null;
        this.g |= "u".equals(string2);
        if (string == null) {
            this.Z.remove(string2);
        } else {
            this.Z.put(string2, string);
        }
        this.s();
        if (this.n) {
            String string3 = this.Code;
            String string4 = "";
            if (string2.equals("o:a")) {
                Browser.Code.Z(string);
                return true;
            }
            switch (a.Code(string3)) {
                case 65: 
                case 118: {
                    string3 = string2 + string;
                    break;
                }
                default: {
                    string4 = this.Code();
                }
            }
            if (bl) {
                this.Z.remove(string2);
            }
            this.Code.Code(string3, this.Code, string4, this.J);
        }
        return true;
    }

    private int l() {
        int n;
        do {
            if (this.M > this.m) {
                return this.M;
            }
            n = this.M;
            this.j();
            int n2 = this.j();
            this.p();
            this.M += n2 * 10 - 10;
            this.u();
        } while (this.I[n] == 105 || (this.I[this.Z(this.H, 0) * 6 + 4] & 1) == 0);
        return n;
    }

    public final synchronized void C() {
        int n = 0;
        this.M = this.l;
        while (this.M <= this.m && n < this.n) {
            int n2 = this.M;
            this.m();
            if (!a.Code(this.J(), "menu:")) continue;
            n = n2;
        }
        if (n > 0) {
            this.n = n;
            this.B(false);
        }
    }

    private synchronized int k() {
        if (this.n <= 0) {
            return 0;
        }
        this.M = this.l;
        int n = 0;
        while (this.M < this.n) {
            ++n;
            ++this.M;
            this.M = 10 * this.j() + this.M;
            this.u();
        }
        return n;
    }

    /*
     * Unable to fully structure code
     */
    public final synchronized void Code(int var1_1, int var2_2, int var3_3) {
        block28: {
            block26: {
                block27: {
                    if (this.C()) {
                        if (var1_1 != 0) {
                            this.Code(0, var1_1 * this.e(), false, false);
                            return;
                        }
                        if (var3_3 >= 8 && this.X > 0 && this.X < this.f - this.Code.Code()) {
                            this.Code(0, var2_2 * (39 + var3_3), false, false);
                            return;
                        }
                        var1_1 = 0;
                        if (this.n > 0) {
                            this.B(this.n);
                            var1_1 = (int)a.Code(0, this.H, 1, this.L, 0, this.X, 1, this.Code.Code());
                        }
                        if ((var3_3 = this.l) > 0) {
                            this.M = var3_3;
                            var3_3 = this.l();
                        }
                        var5_6 = var4_4 = var3_3;
                        if (var3_3 > 0 && var3_3 <= this.m) {
                            do {
                                var6_8 = var5_6;
                                var5_6 = var4_4;
                                this.M = var4_4 = this.B(var4_4);
                                var4_4 = this.l();
                                this.B(var5_6);
                                if (var2_2 < 0) {
                                    if (var1_1 != 0) {
                                        if (var5_6 != this.n && var4_4 != this.n) continue;
                                        break;
                                    }
                                    if (this.H + this.L <= this.X + this.Code.Code()) continue;
                                    var5_6 = var6_8;
                                    this.B(var5_6);
                                    break;
                                }
                                if (var1_1 == 0 ? this.H >= this.X : var5_6 > this.n) break;
                            } while (var4_4 <= this.m);
                            this.M = var4_4;
                            var4_4 = var4_4 > this.m ? var5_6 : this.m;
                            var6_8 = this.X;
                            if (var2_2 < 0) {
                                if (!(var5_6 != var3_3 && this.H >= var6_8 || var5_6 != this.n && this.H + this.L >= var6_8 + Code.d || var6_8 <= 0)) {
                                    var6_8 -= 44;
                                }
                            } else if (!(var5_6 != var4_4 && this.H + this.L <= var6_8 + this.Code.Code() || var5_6 != this.n && this.H <= var6_8 + this.Code.Code() - Code.d || var6_8 + this.Code.Code() >= this.f)) {
                                var6_8 += 44;
                            }
                            if (this.c) {
                                if (var2_2 < 0 && this.X == 0 && (var1_1 == 0 || this.n == var3_3)) {
                                    var6_8 = this.f - this.Code.Code();
                                    this.n = var5_6 = var4_4;
                                }
                                if (var2_2 > 0 && this.X + this.Code.Code() >= this.f && (var1_1 == 0 || this.n == var4_4)) {
                                    var6_8 = 0;
                                    this.n = var5_6 = var3_3;
                                }
                                this.B(var5_6);
                            }
                            if (var6_8 == this.X) {
                                this.n = var5_6;
                            }
                            this.e(this.W, var6_8);
                            return;
                        }
                        this.e(this.W, this.X + var2_2 * 44);
                        return;
                    }
                    if (var3_3 >= 5) break block26;
                    if (this.I() && this.k >= this.am) {
                        if (this.am > 0 && this.k + var2_2 * (44 + (var3_3 << 1)) < this.am) {
                            this.j = this.I(this.j);
                            this.k = this.am - 1;
                            this.Code(var1_1, var2_2, var3_3);
                            return;
                        }
                        this.Code(var1_1 * (44 + (var3_3 << 1)), var2_2 * (44 + (var3_3 << 1)), false);
                        this.U = this.W;
                        this.V = this.X;
                        return;
                    }
                    var4_5 = this.j;
                    var5_7 = this.k;
                    this.n = var2_2 != 0 ? this.Code(this.j - 1, this.k + var2_2, 3, var2_2 * 22, this.n, false) : this.Code(this.j + var1_1, this.k, var1_1 * 22, 1, this.n, false);
                    if (this.k >= this.am || this.k + (this.n > 0 ? this.R - this.k : var2_2 * 22) < this.am) break block27;
                    v0 = this;
                    v1 = this.Z(this.j) - this.j;
                    v2 = this.Code(this.k, 22, this.g) - this.k;
                    ** GOTO lbl-1000
                }
                if (this.n > 0) {
                    v0 = this;
                    v1 = this.Q - this.j;
                    v2 = a.Code(this.R, this.k - 44, this.k + 44) - this.k;
                    v3 = true;
                } else {
                    v0 = this;
                    v1 = var1_1 * 22;
                    v2 = var2_2 * 22;
                    if (var3_3 < 5) {
                        v3 = true;
                    } else lbl-1000:
                    // 2 sources

                    {
                        v3 = false;
                    }
                }
                v0.Code(v1, v2, v3);
                if (Math.abs(this.W - this.U) <= Math.abs(var4_5 - this.j)) {
                    this.U = this.W;
                }
                if (Math.abs(this.X - this.V) <= Math.abs(var5_7 - this.k)) {
                    this.V = this.X;
                    return;
                }
                break block28;
            }
            if (var3_3 == 5) {
                this.an = (int)System.currentTimeMillis();
                this.ao = this.j;
                this.ap = this.k;
                this.aq = var1_1;
                this.ar = var2_2;
            }
        }
    }

    public final void Code(Graphics graphics, int n, int n2) {
        int n3;
        block7: {
            block11: {
                block10: {
                    block9: {
                        block8: {
                            block6: {
                                n3 = this.g;
                                if (this.k < this.am || this.ax != 0) {
                                    this.g = 256;
                                }
                                if (!this.Code.I(2)) break block6;
                                this.i = true;
                                this.Code(graphics, n, n2, f.Code[7], f.Code[8], 0, true);
                                this.i = false;
                                break block7;
                            }
                            if (!this.h) break block8;
                            int n4 = this.Code.I(8) ? f.Code[44] : -3092272;
                            this.Code(graphics, n, n2, n4, n4, 0, true);
                            break block7;
                        }
                        if (this.ax <= 0) break block9;
                        int n5 = this.I[this.n] - 80;
                        int n6 = ((n5 | -n5) >>> 31) * -40705 + 40960;
                        this.Code(graphics, n, n2, 0x29000000 | n6, 0x29000000 | n6, 0xFF000000 | n6, false);
                        break block7;
                    }
                    if (this.I()) break block10;
                    Code code = this;
                    if ((f.Code() == code ? true : false) && !Browser.Code.Code()) break block11;
                }
                if (!this.C()) break block7;
            }
            int n7 = this.I[this.n] - 80;
            int n8 = ((n7 | -n7) >>> 31) * 4194495 + 0x408040;
            this.Code(graphics, n, n2, 0x29000000 | n8, 0x29000000 | n8, 0xFF000000 | n8, false);
        }
        if (a.ag) {
            boolean bl = false;
            if (this.n > 0) {
                this.B(this.n);
                bl = a.Code(this.G + n, this.H + n2, this.K, this.L, graphics.getClipX(), graphics.getClipY(), graphics.getClipWidth(), graphics.getClipHeight());
            }
            Browser.Code.I(bl);
        }
        this.g = n3;
    }

    public final void Code(int n, int n2, boolean bl, boolean bl2) {
        if (this.I() && bl) {
            if (this.k >= this.am && this.k + n2 < this.am) {
                this.j = this.I(this.j);
            } else if (this.k < this.am && this.k + n2 >= this.am) {
                this.j = this.Z(this.j);
            }
            this.Code(n, n2, false);
            return;
        }
        if (bl2) {
            Browser.Code.b = true;
        }
        if (n != 0) {
            n = this.I(this.W, this.X, n);
        }
        this.j -= this.W;
        this.k -= this.X;
        this.e(this.W + n, this.Code(this.X, n2, this.g));
        this.j += this.W;
        this.k += this.X;
    }

    public final int c() {
        if (this.k < this.am) {
            return this.k - this.a();
        }
        if (this.V < this.am) {
            return this.I(this.k - this.am) - this.V + this.am;
        }
        return this.I(this.k - this.V);
    }

    private int I(int n) {
        return n * this.g + 255 >> 8;
    }

    private static int Code(int n, int n2) {
        return n * n2 + 255 >> 8;
    }

    public final synchronized String Z() {
        return this.Code(this.n, 76);
    }

    private synchronized int Code(int n, int n2, int n3, int n4, int n5, boolean n6) {
        int n7;
        int n8;
        int n9;
        if (this.l == 0) {
            return 0;
        }
        if (n4 < 0) {
            n9 = n3 + (-n4 - 1 << 1);
            n8 = n2 + n4 - 1;
            n7 = -n4;
            this.Q = 0;
            this.R = 0;
        } else {
            n9 = n3 + (n4 - 1 << 1);
            n8 = n2;
            n7 = n4;
            this.Q = 0;
            this.R = 0;
        }
        int n10 = n + (n3 >> 1) - (n9 >> 1);
        int n11 = 0;
        this.S = Integer.MAX_VALUE;
        this.M = this.l;
        while (this.M <= this.m) {
            int n12 = n6;
            int n13 = n5;
            int n14 = n7;
            int n15 = n9;
            int n16 = n8;
            int n17 = n10;
            int n18 = n4;
            int n19 = n3;
            int n20 = n2;
            int n21 = n;
            Code code = this;
            int n22 = 0;
            int n23 = code.M;
            int n24 = code.j();
            for (int i = code.j(); i > 0; --i) {
                int n25;
                code.p();
                code.H = code.J(code.H);
                if (code.H < 0 || !a.Code(code.G, code.H, code.K, code.L, n17, n16, n15, n14) || (n12 == 0 ? n24 == 105 : n24 != 105)) continue;
                int n26 = n21 + (n19 >> 1);
                if (code.G + code.K <= n26) {
                    n26 = code.G + code.K - 1;
                } else if (code.G > n26) {
                    n26 = code.G;
                }
                int n27 = -1;
                if (n18 < 0 && code.H < n16 + code.G + code.K - n17 && code.H < n16 - code.G + n17 + n15) {
                    n27 = n16;
                    if (code.H + (code.L - 1 >> 1) < n20 && n23 != n13) {
                        n27 = code.H + (code.L - 1 >> 1);
                    }
                }
                if (n18 > 0 && code.H + code.L > n16 - code.G - code.K + n21 && code.H + code.L > n16 + n14 + code.G - n17 - n15) {
                    n27 = n20 + n18 - 1;
                    if (code.H + (code.L - 1 >> 1) > n20 && n23 != n13) {
                        n27 = code.H + (code.L - 1 >> 1);
                    }
                }
                if (n27 < 0 || !a.Code(code.G, code.H, code.K, code.L, n26, n27, 1, 1) || (n25 = (n21 + (n19 >> 1) - n26) * (n21 + (n19 >> 1) - n26) + (n20 - n27) * (n20 - n27)) >= code.S) continue;
                code.S = n25;
                code.Q = n26;
                code.R = n27;
                n22 = n23;
            }
            code.u();
            n12 = n22;
            if (n12 <= 0) continue;
            n11 = n12;
        }
        return n11;
    }

    public final boolean J() {
        return this.I() && this.g == this.Y && this.k >= this.am;
    }

    public final synchronized void Z(Graphics graphics, int n, int n2) {
        int n3;
        int n4;
        this.ay = (int)System.currentTimeMillis();
        this.Code = System.currentTimeMillis();
        if (this.am > 0) {
            n4 = a.c;
            a.c = true;
            n3 = this.g;
            this.g = 256;
            int n5 = Math.min(graphics.getClipY() - n2, this.f - 1);
            int n6 = Math.min(n5 + graphics.getClipHeight() - 1, this.am - 1);
            this.Code(graphics, n, n2, n5, n6);
            this.g = n3;
            n2 += this.am - this.I(this.am);
            a.c = n4;
        }
        n4 = Math.min(Math.max(this.am, this.Z(graphics.getClipY() - n2)), this.f - 1);
        n3 = Math.min(n4 + this.Z(graphics.getClipHeight() - 1), this.f - 1);
        this.Code(graphics, n, n2, n4, n3);
    }

    public final void I(Graphics graphics, int n, int n2) {
        int n3;
        if (this.J()) {
            n3 = this.Code.c;
            int n4 = this.Code.Code();
            int n5 = a.Code(this.k - (n4 >> 1), this.am, this.f - n4);
            int n6 = n + this.I(a.Code(this.B(this.J(this.j, n5), this.j - (n3 >> 1)), 0, this.e - n3));
            n5 = this.V < this.am ? n2 + this.I(n5 - this.am) - this.V + this.am : n2 + this.I(n5 - this.V);
            n3 = this.I(n3) - 1;
            n4 = this.I(n4) - 1;
            if (a.j && Code[5] != null) {
                n3 = n5;
                n4 = n6;
                Graphics graphics2 = graphics;
                Code code = this;
                int n7 = code.I(code.Code.c);
                int n8 = code.I(code.Code.Code());
                int n9 = n7 > 9 ? n7 - 9 : 1;
                int n10 = n8 > 9 ? n8 - 9 : 1;
                int n11 = n7 > 9 ? 30 : 21 + n7;
                int n12 = n8 > 9 ? 30 : 21 + n8;
                a.Code(graphics2, n4 - 21, n3 - 21, n11, n12);
                graphics2.drawImage(Code[5], n4 - 21, n3 - 21, 0);
                a.Code(graphics2);
                a.Code(graphics2, n4 + n9, n3 - 21, 30, n12);
                graphics2.drawImage(Code[5], n4 + n7 - 59, n3 - 21, 0);
                a.Code(graphics2);
                a.Code(graphics2, n4 - 21, n3 + n10, n11, 30);
                graphics2.drawImage(Code[5], n4 - 21, n3 + n8 - 63, 0);
                a.Code(graphics2);
                a.Code(graphics2, n4 + n9, n3 + n10, 30, 30);
                graphics2.drawImage(Code[5], n4 + n7 - 59, n3 + n8 - 63, 0);
                a.Code(graphics2);
                if (n9 > 1) {
                    n9 = (n7 - 18) / 19;
                    if (n9 > 0) {
                        for (n11 = 0; n11 < n9; ++n11) {
                            a.Code(graphics2, n4 + 9 + 19 * n11, n3 - 21, 19, 30);
                            graphics2.drawImage(Code[5], n4 - 21 + 19 * n11, n3 - 21, 0);
                            a.Code(graphics2);
                            a.Code(graphics2, n4 + 9 + 19 * n11, n3 + n8 - 9, 19, 30);
                            graphics2.drawImage(Code[5], n4 - 21 + 19 * n11, n3 + n8 - 63, 0);
                            a.Code(graphics2);
                        }
                    }
                    if ((n11 = n7 - 18 - n9 * 19) > 0) {
                        a.Code(graphics2, n4 + 9 + 19 * n9, n3 - 21, n11, 30);
                        graphics2.drawImage(Code[5], n4 - 21 + 19 * n9, n3 - 21, 0);
                        a.Code(graphics2);
                        a.Code(graphics2, n4 + 9 + 19 * n9, n3 + n8 - 9, n11, 30);
                        graphics2.drawImage(Code[5], n4 - 21 + 19 * n9, n3 + n8 - 63, 0);
                        a.Code(graphics2);
                    }
                }
                if (n10 > 1) {
                    n9 = (n8 - 18) / 24;
                    if (n9 > 0) {
                        for (n10 = 0; n10 < n9; ++n10) {
                            a.Code(graphics2, n4 - 21, n3 + 9 + 24 * n10, 30, 24);
                            graphics2.drawImage(Code[5], n4 - 21, n3 - 22 + 24 * n10, 0);
                            a.Code(graphics2);
                            a.Code(graphics2, n4 + n7 - 9, n3 + 9 + 24 * n10, 30, 24);
                            graphics2.drawImage(Code[5], n4 + n7 - 59, n3 - 22 + 24 * n10, 0);
                            a.Code(graphics2);
                        }
                    }
                    if ((n10 = n8 - 18 - n9 * 24) > 0) {
                        a.Code(graphics2, n4 - 21, n3 + 9 + 24 * n9, 30, n10);
                        graphics2.drawImage(Code[5], n4 - 21, n3 - 22 + 24 * n9, 0);
                        a.Code(graphics2);
                        a.Code(graphics2, n4 + n7 - 9, n3 + 9 + 24 * n9, 30, n10);
                        graphics2.drawImage(Code[5], n4 + n7 - 59, n3 - 22 + 24 * n9, 0);
                        a.Code(graphics2);
                    }
                }
                if (a.w) {
                    graphics2.setColor(0);
                    graphics2.drawRect(n4, n3, n7 - 1, n8 - 1);
                    graphics2.setColor(0xFFFFFF);
                    graphics2.drawRect(n4 + 1, n3 + 1, n7 - 3, n8 - 3);
                }
                n3 = 2;
            } else {
                graphics.setColor(0);
                graphics.drawRect(n6, n5, n3, n4);
                graphics.setColor(0xFFFFFF);
                graphics.drawRect(n6 + 1, n5 + 1, n3 - 2, n4 - 2);
                n3 = 2;
            }
        } else {
            if (this.C()) {
                a.Code(graphics);
                return;
            }
            Code code = this.Code.Code();
            n3 = code != null && code.b < 5 || g.Code ? (this.n > 0 ? 4 : 3) : (this.n > 0 ? 1 : 0);
        }
        a.Code(graphics);
        if (this.Code.aS) {
            n3 = 0;
        }
        graphics.drawImage(Code[n3], this.Code.Z() + n - 5, this.Code.J() + n2 - 3, 20);
    }

    private static int I(int n, int n2) {
        return (n << 8) / n2;
    }

    private int Z(int n, int n2) {
        int n3 = 0;
        int n4 = this.h - 1;
        do {
            int n5;
            if (this.I[(n5 = n3 + n4 >> 1) * 6 + n2] > n) {
                n4 = n5 - 1;
                continue;
            }
            if (this.I[n5 * 6 + n2] + this.I[n5 * 6 + 1] <= n) {
                n3 = n5 + 1;
                continue;
            }
            return n5;
        } while (n3 <= n4 && n4 >= n3);
        throw new RuntimeException();
    }

    private void Code(Graphics graphics, int n, int n2, int n3, int n4) {
        int n5 = n3 = this.Z(n3, 5);
        while (true) {
            if (n5 == this.h) {
                this.I(graphics, n, n2, n3, n5 - 1);
                return;
            }
            this.I[n5 * 6 + 3] = this.I[n5 * 6 + 2];
            if ((this.I[n5 * 6 + 4] & 1) == 0) {
                this.I(graphics, n, n2, n3, n5 - 1);
                do {
                    if (++n3 != this.h) continue;
                    return;
                } while ((this.I[n3 * 6 + 4] & 1) == 0);
                n5 = n3 - 1;
            } else if (this.I[n5 * 6 + 5] > n4) {
                this.I(graphics, n, n2, n3, n5 - 1);
                return;
            }
            ++n5;
        }
    }

    private void I(Graphics graphics, int n, int n2, int n3, int n4) {
        if (n4 < 0) {
            return;
        }
        a.Code(graphics, n, n2 += this.I(this.I[n3 * 6 + 5]), this.e, this.I[n4 * 6] + this.I[n4 * 6 + 1]);
        n2 -= this.I(this.I[n3 * 6]);
        this.M = 0;
        block15: while (true) {
            int n5;
            int n6;
            int n7;
            int n8 = Integer.MAX_VALUE;
            block16: for (n7 = n3; n7 <= n4; ++n7) {
                n6 = this.I[n7 * 6 + 3];
                while (this.I[n6] != 0) {
                    this.I[n7 * 6 + 3] = n6;
                    if (this.I[n6 + 1] > this.M) {
                        n8 = a.Code(n8, this.M, this.I[n6]);
                        continue block16;
                    }
                    n6 += 2;
                }
            }
            if (n8 == Integer.MAX_VALUE) break;
            n7 = n8;
            do {
                n6 = n7;
                for (n5 = n3; n5 <= n4; ++n5) {
                    int n9 = this.I[n5 * 6 + 3];
                    while (this.I[n9] != 0 && this.I[n9] <= n7 + this.i) {
                        this.I[n5 * 6 + 3] = n9;
                        n7 = Math.max(n7, this.I[n9 + 1]);
                        n9 += 2;
                    }
                }
            } while (n6 != n7);
            int n10 = n8;
            n8 = n7;
            n7 = n10;
            n6 = n2;
            n5 = n;
            Graphics graphics2 = graphics;
            Code code = this;
            this.M = n7;
            block21: while (true) {
                if (code.M >= n8) continue block15;
                n7 = code.j();
                if (n7 == 83 || n7 == 76 || n7 == 77) {
                    code.b(n7);
                    continue;
                }
                code.p();
                int n11 = code.L;
                code.d(n5, n6);
                Graphics graphics3 = graphics2;
                Code code2 = code;
                if (a.Code(code2.G, code2.H, code2.K, code2.L, graphics3.getClipX(), graphics3.getClipY(), graphics3.getClipWidth(), graphics3.getClipHeight())) {
                    String string;
                    Code code3;
                    Code code4 = code3 = code;
                    code3.M += 4;
                    int n12 = (code4.I[code4.M - 4] << 24) + ((code4.I[code4.M - 3] & 0xFF) << 16) + ((code4.I[code4.M - 2] & 0xFF) << 8) + (code4.I[code4.M - 1] & 0xFF);
                    if (code3.i && n12 >>> 24 != 254) {
                        n12 = n12 & 0xFF000000 | 0xFFFFFF - (n12 & 0xFFFFFF);
                    }
                    int n13 = n12;
                    switch (n7) {
                        default: {
                            continue block21;
                        }
                        case 84: {
                            int n14;
                            Object object = new Integer(code.M - 4);
                            if (code.B != null && code.B.containsKey(object)) {
                                int n15 = (n13 & 0xFFFFFF) > 0xC0C0C0 ? 1 : 0;
                                object = (int[])code.B.get(object);
                                for (int i = 0; i < ((Object)object).length; ++i) {
                                    n14 = n15 * -33545421 + 0xFFEE66;
                                    int[] nArray = (int[])code.Z.elementAt((int)object[i]);
                                    graphics2.setColor(n14);
                                    if (object[i] == code.T) {
                                        graphics2.setColor(n15 * -28391 + 61030);
                                    }
                                    graphics2.fillRect(code.G + nArray[0], code.H + nArray[1], nArray[2], nArray[3]);
                                }
                            }
                            int n16 = a.I(code.j());
                            int n17 = code.i();
                            char[] cArray = Code;
                            if (n17 > Code.length) {
                                cArray = new char[n17];
                            }
                            n14 = a.Code(code.I, code.M, n17, cArray);
                            if (code.Code() && cArray[0] == '\u0000') {
                                int n18 = cArray[1] - 32;
                                String string2 = null;
                                if (code.I != null && n18 < code.I.size()) {
                                    string2 = (String)((Object[])code.I.elementAt(n18))[3];
                                }
                                if ((code.K = a.Code(n16, cArray = string2 != null ? string2.toCharArray() : new char[]{' '}, 0, n14 = cArray.length)) != code.K) {
                                    code.I[code.M - 12] = (byte)((code.K & 0xFF00) >> 8);
                                    code.I[code.M - 11] = (byte)(code.K & 0xFF);
                                }
                            }
                            code.Code(graphics2, cArray, n14, n13, n16, n11);
                            code.M += n17;
                            continue block21;
                        }
                        case 73: {
                            n7 = code.G - code.j();
                            n11 = code.H - code.j();
                            int n19 = code.j();
                            int n20 = code.h() << 8 >> 8;
                            int n21 = n19 & (code.g >= 128 ? 4 : 2);
                            int n22 = (n21 | -n21) >>> 31 ^ 1;
                            int n23 = n19 & 8;
                            int n24 = (n23 | -n23) >>> 31;
                            Image image = null;
                            if (n22 != 0) {
                                image = code.Code(n20, code.M - 20, n19);
                            }
                            if (n22 != 0 && image != null) {
                                a.Code(graphics2, code.G, code.H, code.K, code.L);
                                n13 = image.getWidth();
                                n19 = image.getHeight();
                                if (n7 + n13 < graphics2.getClipX()) {
                                    n7 = graphics2.getClipX() - (graphics2.getClipX() - n7) % n13;
                                }
                                if (n11 + n19 < graphics2.getClipY()) {
                                    n11 = graphics2.getClipY() - (graphics2.getClipY() - n11) % n19;
                                }
                                n20 = graphics2.getClipX() + graphics2.getClipWidth();
                                n22 = graphics2.getClipY() + graphics2.getClipHeight();
                                while (n7 < n20) {
                                    for (n24 = n11; n24 < n22; n24 += n19) {
                                        graphics2.drawImage(image, n7, n24, 20);
                                    }
                                    n7 += n13;
                                }
                                a.Code(graphics2);
                                continue block21;
                            }
                            if (n22 == 0 && n24 == 0) continue block21;
                            a.Code(graphics2, code.G, code.H, code.K, code.L, n13 | 0xFF000000, false);
                            continue block21;
                        }
                        case 66: {
                            a.Code(graphics2, code.G, code.H, code.K, code.L, n13, false);
                            continue block21;
                        }
                        case 70: 
                    }
                    n7 = code.j();
                    int n25 = code.j();
                    String string3 = code.J();
                    String string4 = code.J();
                    int n26 = code.h();
                    String string5 = string = (String)code.Z.get(string3);
                    if (string == null) {
                        string5 = string4;
                    }
                    char[] cArray = null;
                    Image image = null;
                    switch (n7) {
                        case 112: {
                            cArray = new char[string5.length()];
                            for (n7 = 0; n7 < cArray.length; ++n7) {
                                cArray[n7] = 42;
                            }
                            break;
                        }
                        case 97: {
                            cArray = string5.toCharArray();
                            n7 = 0;
                            n25 = 1;
                            for (int i = 0; i < cArray.length; ++i) {
                                if (cArray[i] == '\n') {
                                    n7 = 0;
                                    continue;
                                }
                                if (i != 0 && cArray[i] != ' ') continue;
                                while (i + n25 < cArray.length && cArray[i + n25] != ' ') {
                                    ++n25;
                                }
                                if (n7 + (n25 = a.Code(0, cArray, i, n25)) > code.K) {
                                    cArray[i] = 10;
                                    n7 = 0;
                                }
                                n7 += n25;
                                n25 = 1;
                            }
                            break;
                        }
                        case 120: {
                            cArray = string5.toCharArray();
                            if (!string3.equals("$")) break;
                            cArray = ((String)((Object[])Code.elementAt(F + 1))[1]).toCharArray();
                            break;
                        }
                        case 99: {
                            int n27 = n25 & 2;
                            if (((string != null ? 1 : 0) ^ (n27 | -n27) >>> 31) == 0) break;
                            if (a.h) {
                                graphics2.setColor(0);
                                graphics2.drawLine(code.G + 1, code.H + 1, code.G + code.K - 2, code.H + code.L - 2);
                                graphics2.drawLine(code.G + 1, code.H + code.L - 2, code.G + code.K - 2, code.H + 1);
                                break;
                            }
                            image = I[1];
                            break;
                        }
                        case 114: {
                            if (!(string != null ? string.equals(string4) : (n25 & 2) != 0)) break;
                            if (a.h) {
                                graphics2.setColor(0x404040);
                                graphics2.fillArc(code.G + 1, code.H + 1, code.K - 3, code.L - 3, -90, 360);
                                break;
                            }
                            image = I[3];
                            break;
                        }
                        case 115: {
                            n7 = code.M;
                            code.M = n26;
                            string4 = "\u0000" + string5 + "\u0000";
                            StringBuffer stringBuffer = new StringBuffer();
                            for (int i = code.h(); i > 0; --i) {
                                boolean bl;
                                String string6 = code.J();
                                string5 = code.J();
                                boolean bl2 = bl = string4.indexOf("\u0000" + string6 + "\u0000") >= 0;
                                if ((n25 & 4) == 0) {
                                    if (!bl) continue;
                                    stringBuffer.append(string5).append("\n");
                                    break;
                                }
                                if (bl) {
                                    stringBuffer.append("*");
                                }
                                stringBuffer.append(string5).append("\n");
                            }
                            code.M = n7;
                            cArray = stringBuffer.toString().toCharArray();
                            break;
                        }
                        case 102: {
                            Object[] objectArray;
                            cArray = code.J != null && (objectArray = (Object[])code.J.get(string3)) != null ? (objectArray[0] + ", " + (Integer)objectArray[1] / 1000 + " kB").toCharArray() : a.Code(182).toCharArray();
                        }
                    }
                    if (cArray != null) {
                        a.Code(graphics2, code.G, code.H, code.K, code.L);
                        code.Code(graphics2, cArray, cArray.length, n13, 0, n11);
                        a.Code(graphics2);
                    }
                    if (image == null || code.g != 256) continue;
                    graphics2.drawImage(image, code.G + (code.K >> 1), code.H + (code.L >> 1), 3);
                    continue;
                }
                code.b(n7);
            }
            break;
        }
        a.Code(graphics);
    }

    private void Code(Graphics graphics, int n, int n2, int n3, int n4, int n5, boolean bl) {
        int n6;
        int n7;
        int n8;
        int n9;
        if (this.n <= 0) {
            return;
        }
        if (!this.a()) {
            if (n3 == n4) {
                n4 = n3 &= 0xFF000000;
            }
            n5 = n5 & 0xFF000000 | 0xA8A8A8;
        }
        this.M = this.n;
        this.j();
        int n10 = this.j();
        Vector<Integer> vector = new Vector<Integer>();
        int n11 = Integer.MAX_VALUE;
        block0: while (n10-- > 0) {
            this.p();
            this.H = this.J(this.H);
            --this.G;
            --this.H;
            ++this.L;
            if (n5 != 0) {
                this.G -= 2;
                this.H -= 2;
                this.K += 4;
                this.L += 3;
            }
            this.d(n, n2);
            n11 = Math.min(n11, this.H);
            n9 = this.G;
            n8 = n9 + this.K;
            n7 = 0;
            while (true) {
                n6 = 0x7FFFFFFE;
                if (n7 < vector.size()) {
                    n6 = (Integer)vector.elementAt(n7);
                }
                if (n6 >= n9) {
                    vector.insertElementAt(new Integer(n9), n7++);
                    vector.insertElementAt(new Integer(this.H), n7++);
                    vector.insertElementAt(new Integer(this.H + this.L), n7++);
                    vector.insertElementAt(new Integer(1), n7++);
                    vector.insertElementAt(new Integer(this.K > 40 && this.L > aD ? 1 : 0), n7++);
                    n9 = Integer.MAX_VALUE;
                }
                if (n6 > n8) {
                    vector.insertElementAt(new Integer(this.K > 40 && this.L > aD ? -1 : 0), n7);
                    vector.insertElementAt(new Integer(-1), n7);
                    vector.insertElementAt(new Integer(this.H + this.L), n7);
                    vector.insertElementAt(new Integer(this.H), n7);
                    vector.insertElementAt(new Integer(n8), n7);
                    continue block0;
                }
                n7 += 5;
            }
        }
        n10 = n11;
        while (n11 != Integer.MAX_VALUE) {
            n9 = Integer.MAX_VALUE;
            n8 = 0;
            n7 = 0;
            n6 = 0;
            int n12 = 0;
            int n13 = 0;
            int n14 = 0;
            while (n14 < vector.size()) {
                int n15 = n7;
                int n16 = n6;
                int n17 = (Integer)vector.elementAt(n14) + 1;
                n12 += n13;
                n13 = 0;
                do {
                    ((Integer)vector.elementAt(n14++)).intValue();
                    int n18 = (Integer)vector.elementAt(n14++);
                    int n19 = (Integer)vector.elementAt(n14++);
                    int n20 = (Integer)vector.elementAt(n14++);
                    int n21 = (Integer)vector.elementAt(n14++);
                    if (n18 > n11) {
                        n9 = Math.min(n9, n18);
                    }
                    if (n19 > n11) {
                        n9 = Math.min(n9, n19);
                    }
                    if (n18 < n11 && n19 >= n11) {
                        n7 += n20;
                        n13 += n21;
                    }
                    if (n18 > n11 || n19 <= n11) continue;
                    n6 += n20;
                } while (n14 < vector.size() && (Integer)vector.elementAt(n14) <= n17);
                --n17;
                if (n12 == 0 && n15 > 0) {
                    if (n3 == n4) {
                        a.Code(graphics, n8 + 1, n10 + 1, n17 - n8, n11 - n10, n3, false);
                    } else {
                        a.Code(graphics, n8 + 1, n10 + 1, n17 - n8, n11 - n10, n3, n4);
                    }
                    if (bl) {
                        a.Code(graphics, n8 + 1, n10 + 1, n17 - n8, n11 - n10);
                        this.Z(graphics, n, n2);
                        a.Code(graphics);
                    }
                }
                int n22 = n15;
                int n23 = n7;
                if (((n22 | -n22) >>> 31 ^ 1 ^ ((n23 | -n23) >>> 31 ^ 1)) != 0) {
                    a.Code(graphics, n17, n10 + 1, 2, n11 - n10, n5, false);
                }
                int n24 = n15;
                int n25 = n16;
                if (((n24 | -n24) >>> 31 ^ 1 ^ ((n25 | -n25) >>> 31 ^ 1)) != 0) {
                    a.Code(graphics, n8 + 1, n11, n17 - n8, 2, n5, false);
                }
                n8 = n17;
            }
            n10 = n11;
            n11 = n9;
        }
    }

    static {
        d = 16;
        aD = 2 + (Font.getFont((int)64, (int)1, (int)16).getHeight() << 1);
        F = -1;
        Code = new char[512];
        c = "";
        O = -1;
        P = -1;
    }

    private void Code(Graphics graphics, char[] cArray, int n, int n2, int n3, int n4) {
        int n5;
        int n6;
        int n7;
        int n8 = 1;
        int n9 = Math.max(n4 / a.J(n3), 1);
        for (n6 = 0; n6 < n && n8 <= n9; n8 += (n7 | -n7) >>> 31 ^ 1, ++n6) {
            n7 = cArray[n6] - 10;
        }
        n = n8;
        if (this.g >= 200) {
            graphics.setColor(n2);
        } else {
            if (J == null || J.length < 2 * f.l) {
                B = null;
                C = null;
                J = null;
                B = new int[2 * f.l];
                C = new int[B.length];
                J = new int[B.length];
                Random random = new Random(4711);
                for (n5 = 0; n5 < J.length >> 1; ++n5) {
                    Code.J[n5] = random.nextInt() & 0x7F;
                    Code.J[n5 + (Code.J.length >> 1)] = J[n5] + 64;
                }
            }
            if ((n9 = a.Code(n3) << 6) != P || n2 != O) {
                for (n5 = 0; n5 < C.length; ++n5) {
                    Code.C[n5] = n9 + J[n5] << 24 | n2 & 0xFFFFFF;
                }
                P = n9;
                O = n2;
            }
        }
        while (n6 > 0) {
            n9 = this.H + this.I(n4 * --n / n8);
            n5 = 0;
            if (n == 0) {
                n5 = n6;
                n6 = 0;
            } else {
                while (n6 > 0 && cArray[n6 - 1] != '\n') {
                    ++n5;
                    --n6;
                }
            }
            if (this.g >= 200) {
                a.Code(graphics, n3, cArray, n6, n5, this.G, n9);
            } else {
                int n10 = this.I(a.Code(n3, cArray, n6, n5));
                if ((n10 = Math.min((B.length >> 1) - 1, n10)) <= 0 || n9 < 0) {
                    --n6;
                    continue;
                }
                int n11 = Math.max(n10 / n5, 1);
                if (a.aj || !a.i || a.l) {
                    graphics.setColor(n2);
                    graphics.drawLine(this.G, n9, this.G + n10, n9);
                } else {
                    int n12;
                    int n13 = (n6 << 1 | n5 << 2 ^ n10) % ((B.length >> 1) - n10) & 0xFFFFFFFE;
                    for (n12 = 0; n12 < n10; ++n12) {
                        Code.B[n12] = C[n13 + n12];
                        Code.B[n10 + n12] = C[(C.length >> 1) + n13 + n12];
                    }
                    for (n13 = n6; n13 < n6 + n5; ++n13) {
                        if (cArray[n13] != ' ') continue;
                        for (n12 = 0; n12 < n11; ++n12) {
                            int n14 = (n13 - n6) * n11 + n12;
                            if (n14 < B.length) {
                                Code.B[n14] = 0;
                            }
                            if ((n14 = (n13 - n6) * n11 + n10 + n12) >= B.length) continue;
                            Code.B[n14] = 0;
                        }
                    }
                    n5 = Math.max(0, -this.G);
                    if (n10 > n5) {
                        graphics.drawRGB(B, n5, n10 - n5, this.G + n5, n9, n10 - n5, 2, true);
                    }
                }
            }
            --n6;
        }
    }

    private synchronized int B(int n) {
        int n2 = Integer.MAX_VALUE;
        int n3 = Integer.MAX_VALUE;
        int n4 = 0;
        int n5 = 0;
        this.M = n;
        this.j();
        for (n = this.j(); n > 0; --n) {
            this.p();
            n2 = Math.min(n2, this.G);
            n3 = Math.min(n3, this.H);
            n4 = Math.max(n4, this.G + this.K);
            n5 = Math.max(n5, this.H + this.L);
        }
        this.u();
        this.G = n2;
        this.H = this.J(n3);
        this.K = n4 - n2;
        this.L = n5 - n3;
        return this.M;
    }

    private int I(int n, int n2, int n3) {
        int n4 = n + Math.min(0, n3);
        int n5 = Math.abs(n3) + this.Code.c;
        for (int i = 0; i < this.p; ++i) {
            if (!a.Code(n4, n2 + this.r, n5, this.Code.Code() - 2 * this.r, this.Z[i << 2], this.Z[(i << 2) + 1], this.Z[(i << 2) + 2] - this.Z[i << 2], this.Z[(i << 2) + 3] - this.Z[(i << 2) + 1])) continue;
            if (n3 > 0) {
                if (this.Z[(i << 2) + 2] <= n + this.Code.c) continue;
                n3 = Math.min(n3, Math.max(this.Z[(i << 2) + 2] - n - this.Code.c, this.Z[i << 2] - n));
                continue;
            }
            if (this.Z[i << 2] >= n) continue;
            n3 = Math.max(n3, Math.min(this.Z[i << 2] - n, this.Z[(i << 2) + 2] - n - this.Code.c));
        }
        return n3;
    }

    private int J(int n, int n2) {
        for (int i = 0; i < this.p; ++i) {
            if (!a.Code(n, n2 + this.r, 1, this.Code.Code() - 2 * this.r, this.Z[i << 2], this.Z[(i << 2) + 1], this.Z[(i << 2) + 2] - this.Z[i << 2], this.Z[(i << 2) + 3] - this.Z[(i << 2) + 1])) continue;
            return i;
        }
        return -1;
    }

    public final boolean Code(boolean bl) {
        if (!this.Code.I(2) && this.Code.I == null && this.Code != null && !this.Code.startsWith("myopera:") && !this.Code.startsWith("sk:") && !this.Code.startsWith("server:") && (!this.Code() || a.Code(this.Code) == 90 && a.Code(this.Code(this.n, 76), "bookmark:") || a.Code(this.Code) == 91 && a.Code(this.Code(this.n, 76), "speeddial:") || a.Code(this.Code) == 109 && a.Code(this.Code(this.n, 76), "search:")) && a.aG) {
            this.b(bl);
            this.Code.Code("menu:" + a.I(123), null, null);
            return true;
        }
        return false;
    }

    private int B(int n, int n2) {
        if (n >= 0) {
            if (this.Z[(n << 2) + 2] > n2 + this.Code.c && n2 < this.Z[n << 2]) {
                n2 = Math.min(this.Z[n << 2], this.Z[(n << 2) + 2] - this.Code.c);
            }
            if (this.Z[n << 2] < n2 && n2 + this.Code.c > this.Z[(n << 2) + 2]) {
                n2 = Math.max(this.Z[n << 2], this.Z[(n << 2) + 2] - this.Code.c);
            }
        }
        return n2;
    }

    public final void J(int n, int n2) {
        this.as = n;
        this.at = n2;
        this.k = this.Code(this.V, n2, this.g);
        this.j = this.U + (this.k >= this.am ? this.Z(n) : n);
        this.ax = this.n = !this.I() || this.k < this.am ? this.Code(this.j, this.k, 1, 1, -1, false) : 0;
        this.k = false;
        this.l = false;
        this.m = false;
        if (this.ax > 0 && a.Code(this.Code(this.ax, 76), "menu:")) {
            this.B(false);
            return;
        }
        this.Code.f();
    }

    public final void d() {
        if (!this.l && this.ax >= 0) {
            this.k = true;
            if (a.g) {
                this.m = this.l = this.Code(true);
                this.k = this.l;
            }
        }
    }

    public final void e() {
        if (!this.l && this.ax == this.n) {
            if (this.k) {
                this.Code(true);
            } else if (this.I() && this.k >= this.am) {
                this.I(false);
            } else {
                Browser.Code.Code(this.Code);
                this.B(this.Code.I(2) ^ true);
            }
        }
        this.ax = 0;
    }

    public final void B(int n, int n2) {
        if (!this.m) {
            if (this.Code.I(2)) {
                this.n = this.Code(n + this.U, n2 + this.V, 1, 1, -1, false);
                if (this.n > 0 && this.n != this.ax) {
                    Browser.Code.Code(this.Code);
                    this.ax = this.n;
                    if (a.Code(this.Code(this.ax, 76), "menu:")) {
                        this.B(false);
                        return;
                    }
                }
            } else {
                int n3;
                int n4;
                if (this.ax >= 0 && (Math.abs(this.as - n) > 6 || Math.abs(this.at - n2) > 6)) {
                    Vector vector;
                    if ((a.Code(this.Code) == 90 && a.Code(this.Code(this.n, 76), "bookmark:") ? a.Code : (vector = a.Code(this.Code) == 109 && a.Code(this.Code(this.n, 76), "search:") ? a.Z : null)) != null && this.ax > 0) {
                        n4 = a.I(this.Code(this.ax, 76));
                        n3 = 0;
                        if (vector == a.Z) {
                            n3 = 1;
                        }
                        this.B(this.ax);
                        if (n4 >= n3 && (n2 < this.H && n4 > n3 || n2 > this.H + this.L && n4 + 1 < vector.size())) {
                            this.Code.Code(vector, n2 < this.H ? -1 : 1);
                            this.Code.Code.ax = this.Code.Code.n;
                            this.Code.Code.as = n;
                            this.Code.Code.at = n2;
                        }
                        this.Code.Code.l = true;
                    } else {
                        this.n = 0;
                        this.ax = -1;
                        if (a.g) {
                            this.as = n;
                            this.at = n2;
                        }
                        this.au = this.av = this.W;
                        this.aw = this.at;
                    }
                }
                if (this.ax < 0) {
                    int n5 = this.as - n << 1;
                    n4 = this.Code(this.X, this.at - n2 << 1, this.g);
                    n3 = this.W;
                    if (this.au == this.av) {
                        if (Math.abs(this.at - this.aw) > 20) {
                            this.av = -1;
                        } else if (n5 > 40) {
                            this.av += this.I(n3, n4, this.e - this.Code.c - n3);
                        } else if (n5 < -40) {
                            this.au += this.I(n3, n4, -n3);
                        }
                    }
                    if (this.av < this.au) {
                        int n6 = n4;
                        int n7 = n3;
                        Code code = this;
                        int n8 = n7 + Math.min(0, 0 * code.Code.c);
                        int n9 = (Math.abs(0) + 1) * code.Code.c;
                        int n10 = -1;
                        int n11 = Integer.MAX_VALUE;
                        for (int i = 0; i < code.p; ++i) {
                            if (!a.Code(n8, n6 + code.r, n9, code.Code.Code() - 2 * code.r, code.Z[i << 2], code.Z[(i << 2) + 1], code.Z[(i << 2) + 2] - code.Z[i << 2], code.Z[(i << 2) + 3] - code.Z[(i << 2) + 1]) || Math.abs(code.B(i, n7) - n7) >= n11) continue;
                            n10 = i;
                            n11 = Math.abs(code.B(i, n7) - n7);
                        }
                        int n12 = n6 = Math.abs(n11) <= code.Code.c - code.q ? n10 : -1;
                        if (Math.abs(n3 - this.B(n6, n3)) < this.d() >> 1) {
                            n3 = this.B(n6, n3);
                        }
                    }
                    if (this.av > this.au) {
                        n3 = a.Code(n3 + n5, this.au, this.av);
                        this.as = n;
                    }
                    this.e(n3, n4);
                    if (Math.abs(this.U - this.W) <= Math.abs(n5)) {
                        this.U = this.W;
                    }
                    this.V = this.X;
                    this.at = n2;
                }
            }
            this.k = this.Code(this.V, n2, this.g);
            this.j = this.U + (this.k >= this.am ? this.Z(n) : n);
            this.Code.f();
        }
    }
}

