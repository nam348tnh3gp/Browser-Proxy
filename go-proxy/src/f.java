/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Alert
 *  javax.microedition.lcdui.AlertType
 *  javax.microedition.lcdui.Canvas
 *  javax.microedition.lcdui.Command
 *  javax.microedition.lcdui.CommandListener
 *  javax.microedition.lcdui.Displayable
 *  javax.microedition.lcdui.Font
 *  javax.microedition.lcdui.Graphics
 *  javax.microedition.lcdui.Image
 *  javax.microedition.lcdui.List
 *  javax.microedition.lcdui.Screen
 *  javax.microedition.lcdui.TextBox
 *  javax.microedition.lcdui.game.Sprite
 *  javax.microedition.rms.RecordStore
 */
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Screen;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.rms.RecordStore;

/*
 * Duplicate member names - consider using --renamedupmembers true
 */
public final class f
extends Canvas
implements CommandListener,
Runnable {
    private int s;
    public static int e;
    public int Code;
    public static Vector Code;
    public static Vector I;
    private boolean e;
    public static String Code;
    private Image j;
    public static int c;
    public static int[] Code;
    private String C;
    public static g Code;
    private Object Code;
    public static boolean I;
    public static String J;
    public static long I;
    public static String I;
    public static boolean Code;
    public static int p;
    public static int i;
    public static String Z;
    public static int j;
    public static String B;
    public static int I;
    public static int Z;
    public static boolean Z;
    public static boolean J;
    public static int J;
    public static int B;
    public static boolean B;
    public static boolean d;
    public static g J;
    public static final Command[] Code;
    public static byte[] Code;
    public static int C;
    public static int a;
    public int f;
    public int h;
    public static int b;
    public boolean C;
    public static int d;
    public TimerTask Code;
    public static boolean a;
    private static final int[] Z;
    private int r;
    public long Code;
    public int g;
    public g I;
    private boolean f;
    private static final int[] J;
    public int[] I;
    private int[] B;
    public Timer Code;
    public int q;
    public g Z;
    private static long J;
    private static boolean g;
    private static Vector Z;
    private boolean h;
    private TimerTask I;
    public static int k;
    public boolean b;
    private static int v;
    public static Image i;
    public static int l;
    public Image b;
    public static int m;
    private static Sprite Code;
    public static int n;
    public static int o;
    public J Code;
    private static Graphics Code;
    public Image Code;
    public static boolean c;
    private int t;
    public static Image e;
    public Image Z;
    public Image I;
    public Image J;
    public Image B;
    public Image C;
    public Image a;
    public Image c;
    public static Image d;
    public static Image f;
    public static Image g;
    public static Image h;
    public static long Z;
    private static boolean i;
    private static boolean j;
    private static int u;
    private static Graphics I;
    public TextBox Code;
    private static TextBox I;

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public final void Code(boolean bl) {
        if (bl != a.F) {
            a.F = bl;
            try {
                Browser.Code.Code();
                Browser.Code.sizeChanged(-1, -1);
                this.g();
            }
            catch (Throwable throwable) {
                a.H = (bl && (!a.f || a.L)) ^ (a.K && n > 300) || a.M && n > o;
                throw throwable;
            }
        }
        if ((bl && (!a.f || a.L)) ^ (a.K && n > 300) || a.M && n > o) {
            a.H = true;
            return;
        }
        a.H = false;
    }

    /*
     * Unable to fully structure code
     */
    public f() {
        block148: {
            super();
            this.Code = 0;
            this.C = false;
            this.g = -1;
            this.B = new int[f.J.length];
            this.h = 0;
            this.t = 0;
            this.Code = null;
            this.C = "";
            this.Code = new Timer();
            Browser.Code = this;
            a.Z();
            a.I = Browser.Code.getAppProperty("Operette-UA");
            var1_1 = a.I == null ? "" : a.I.toLowerCase();
            var2_3 = a.Code("microedition.platform");
            var2_3 = var2_3 == null ? "" : var2_3.toLowerCase();
            var3_4 = a.Code("device.model");
            var3_4 = var3_4 == null ? "" : var3_4.toLowerCase();
            var4_7 = Browser.Code.numAlphaLevels();
            a.i = var4_7 > 2;
            a.j = var4_7 > 4;
            a.f = Browser.Code.hasPointerEvents();
            a.g = Browser.Code.hasPointerMotionEvents();
            a.az = a.Code(new char[]{'\u05e0', '\u05e1', '\u05e2'});
            if (!a.az) {
                a.az = a.Code(new char[]{'\u0628', '\u062a', '\u062b'});
            }
            a.X = System.getProperty("microedition.io.file.FileConnection.version") != null;
            a.p = Browser.Code.checkPermission("javax.microedition.io.Connector.file.read");
            a.q = Browser.Code.checkPermission("javax.microedition.io.Connector.file.write");
            if (var1_1.indexOf("sagem") >= 0) {
                a.g = 5;
                a.V = true;
            }
            if (var2_3.startsWith("vs")) {
                a.g = 8;
            }
            if (var2_3.startsWith("rim wireless")) {
                a.g = 7;
            } else if (var2_3.indexOf("nokia") >= 0 || var2_3.equals("siemens_sx1") || var2_3.equals("sendo x") || var2_3.equals("panasonic x700")) {
                a.g = 0;
            } else if (var2_3.indexOf("palm") >= 0 && !var2_3.startsWith("windows ce")) {
                a.g = 6;
            } else if (a.Code("com.siemens.mp.lcdui.Image")) {
                a.g = 2;
            } else if (a.Code("com.motorola.phonebook.PhoneBookRecord") || a.Code("com.motorola.Dialer") || a.Code("com.motorola.phone.Dialer") || a.Code("com.motorola.graphics.j3d.Light") || a.Code("com.motorola.lwt.ComponentScreen") || a.Code("com.motorola.game.GameScreen") || a.Code("com.motorola.funlight.FunLight") || a.Code("com.motorola.multimedia.Lighting") || a.Code("com.motorola.io.ConnectorEvent") || a.Code("com.motorola.extensions.ScalableJPGImage") || a.Code("batterylevel") != null || a.Code("BatteryLevel") != null || a.Code("com.mot.iden.multimedia.Lighting")) {
                a.g = 1;
            } else if (var2_3.indexOf("sonyericsson") >= 0 || var2_3.equals("symbian os") || var2_3.startsWith("w302") || a.Code("com.sonyericsson.IMEI") != null || a.Code("com.sonyericsson.imei") != null) {
                a.g = 3;
            } else if (a.Code("com.samsung.util.AudioClip") || a.Code("com.samsung.util.LCDLight") || a.Code("com.samsung.util.SM") || a.Code("com.samsung.util.SMS") || a.Code("com.samsung.util.Vibration") || var3_4.startsWith("sgh")) {
                a.g = 4;
            } else if (var2_3.startsWith("sagem")) {
                a.V = false;
                a.g = 5;
            }
            var5_9 = a.Code("com.jblend.util.Case");
            var6_12 = a.Code("microedition.profiles");
            var7_13 = Browser.Code.getWidth();
            var8_14 = Browser.Code.getHeight();
            var9_15 = Browser.Code.numColors();
            var10_16 = a.Code("microedition.configuration");
            v0 = var10_17 = var10_16 != null && var10_16.indexOf("CLDC-1.1") >= 0;
            if (var7_13 <= 128 && a.g != 3) {
                a.ay = true;
            }
            var11_18 = false;
            switch (a.g) {
                case 3: {
                    if (var2_3.indexOf("t270") == -1 && var2_3.indexOf("t280") == -1) {
                        a.R = true;
                    }
                    a.n = 3;
                    if (var2_3.indexOf("symbian") >= 0) {
                        a.n = 0;
                    } else if (var2_3.indexOf("p910") >= 0) {
                        a.n = 0;
                    } else if (var2_3.indexOf("m600") >= 0 || var2_3.indexOf("p1") >= 0 || var2_3.indexOf("p9") >= 0) {
                        a.n = 10;
                    }
                    break block148;
                }
                case 0: {
                    a.n = 4;
                    if (var2_3.indexOf("e61") != -1 || var2_3.indexOf("e62") != -1 || var2_3.indexOf("e63") != -1 || var2_3.indexOf("e71") != -1 || var2_3.startsWith("nokia9300/") || var2_3.startsWith("nokia9300i/") || var2_3.startsWith("nokia9500/")) {
                        a.n = 10;
                        a.R = true;
                    }
                    if (var2_3.startsWith("nokian") || var2_3.startsWith("nokiae") || System.getProperty("com.nokia.mid.countrycode") != null || var2_3.startsWith("nokia6124") || var2_3.startsWith("nokia6120") || var2_3.startsWith("nokia6121") || var2_3.startsWith("nokia5700") || var2_3.startsWith("nokia6110") || var2_3.startsWith("nokia5500")) {
                        a.R = true;
                    }
                    if (var2_3.startsWith("nokiae90-1/")) {
                        a.R = false;
                    }
                    if (a.f) {
                        break;
                    }
                    break block148;
                }
                case 1: {
                    a.n = a.Code("com.mot.iden.multimedia.Lighting") != false ? 6 : 5;
                    if (a.f) {
                        break;
                    }
                    break block148;
                }
                case 2: {
                    a.n = 7;
                    if (var2_3.startsWith("sk6r")) {
                        a.n = 0;
                    }
                    break block148;
                }
                case 4: {
                    if (var2_3.startsWith("gt-m8800")) {
                        a.o = 1;
                    }
                    if (var3_4.endsWith("sgh-l700")) {
                        a.w = 1;
                    }
                    a.n = 8;
                    if (var1_1.indexOf("sgh-d900") != -1) {
                        a.R = true;
                    }
                    break block148;
                }
                case 7: {
                    a.n = 0;
                    a.R = true;
                    break block148;
                }
                case 6: {
                    a.n = 13;
                    a.R = true;
                    break block148;
                }
                default: {
                    a.n = 2;
                    if (a.f && var2_3.indexOf("emulator") == -1) {
                        a.o = 1;
                    }
                    if (!var2_3.startsWith("lg-kc550")) ** GOTO lbl130
                    a.o = 0;
                    a.R = true;
                    break block148;
lbl130:
                    // 1 sources

                    if (a.g == 8 && var2_3.startsWith("vs")) break block148;
                    if (var2_3.indexOf("motoq") < 0) ** GOTO lbl137
                    a.R = false;
                    a.n = 10;
                    a.o = 1;
                    a.P = true;
                    break block148;
lbl137:
                    // 1 sources

                    if (var2_3.indexOf("palm treo 700w") < 0) ** GOTO lbl142
                    a.R = true;
                    a.n = 10;
                    a.P = true;
                    break block148;
lbl142:
                    // 1 sources

                    if (!var5_9 && !var2_3.equals("jbed-fastdac") && var2_3.indexOf("windows") < 0 && var2_3.indexOf("intent") < 0 && (!var2_3.equals("jbed") || var8_14 <= 128 || var7_13 <= 128)) break block148;
                }
            }
            a.o = 1;
        }
        if (a.Code("com.sprintpcs.util.System") && a.n == 2) {
            a.n = 9;
            a.R = true;
        }
        if (a.g == 0) {
            if (var7_13 < 176) {
                a.I = true;
            }
            if (var7_13 == 176) {
                a.J = true;
                a.R = true;
                a.h = 150;
            }
            if (var7_13 == 240 && var8_14 == 248 && var10_17) {
                a.b = true;
            }
            if (var2_3.indexOf("3220") >= 0) {
                a.d = true;
            }
            if (var2_3.startsWith("nokiae65-1/") != false && a.Code((String)var2_3, "/0633.18.02") <= 0 || var2_3.startsWith("nokia5500d/") != false && a.Code((String)var2_3, "/04.60") <= 0 || var2_3.startsWith("nokiae50-1/") != false && a.Code((String)var2_3, "/07.13.0.0") <= 0 || var2_3.startsWith("nokiae61i-1/") && a.Code((String)var2_3, "/0633.22.05") <= 0) {
                a.W = true;
            }
            if (var2_3.startsWith("nokia3230/")) {
                a.s = true;
            }
            if (var2_3.startsWith("nokia6260/")) {
                a.q = true;
                a.ak = true;
            }
            if (var2_3.startsWith("nokia6280/")) {
                a.o = true;
            }
            if (var2_3.startsWith("nokia6600/")) {
                a.s = true;
                a.n = true;
                a.q = true;
                a.ak = true;
            }
            if (var2_3.startsWith("nokia6620/")) {
                a.q = true;
                a.ak = true;
            }
            if (var2_3.startsWith("nokia6630/")) {
                a.t = true;
                a.o = true;
                if (a.Code((String)var2_3, "/6.03.40") < 0) {
                    a.X = false;
                }
            }
            if (var2_3.startsWith("nokia6670/")) {
                a.q = true;
                a.ak = true;
            }
            if (var2_3.startsWith("nokia6680/")) {
                a.ab = true;
            }
            if (var2_3.startsWith("nokia7610/")) {
                a.s = true;
                a.q = true;
                a.ak = true;
            }
            if (var2_3.startsWith("nokia7710/")) {
                a.s = true;
                a.q = true;
                a.ak = true;
            }
            if (var2_3.startsWith("nokia9300/") || var2_3.startsWith("nokia9300i/")) {
                a.q = true;
                a.ak = true;
                a.K = true;
            }
            if (var2_3.startsWith("nokia9500/")) {
                a.q = true;
                a.ak = true;
                a.K = true;
            }
            if (var2_3.startsWith("nokiae90-1/")) {
                a.K = true;
            }
            if (var2_3.startsWith("nokiae66-1/") || var2_3.startsWith("nokian82/")) {
                a.M = true;
                a.al = true;
            }
            if (var2_3.startsWith("nokian72/") || var2_3.startsWith("nokian70/") || var2_3.startsWith("nokian70-1/")) {
                a.p = true;
            }
            a.Z = "rtyfghvbnmuj";
            a.J = "1234567890*#";
        }
        if (a.g == 3) {
            if (var7_13 <= 176) {
                a.I = true;
            }
            if (var2_3.indexOf("p990") >= 0 || var2_3.indexOf("m600") >= 0 || var2_3.indexOf("w950") >= 0) {
                a.T = true;
                if (var2_3.indexOf("p990") < 0) {
                    a.U = true;
                }
            }
            if (var2_3.indexOf("p910") >= 0) {
                a.z = true;
                a.l = true;
                a.ak = true;
            }
            if (var2_3.indexOf("symbian") >= 0) {
                a.l = true;
                a.ak = true;
            }
            if (a.Code("javax.wireless.messaging.MultipartMessage") && !a.Code("javax.microedition.amms.GlobalManager")) {
                a.j = 2475;
            }
            if (var2_3.startsWith("sonyericssonp1") || var2_3.startsWith("sonyericssonm600")) {
                a.Z = "ertyuidfghjkcxvbnm \ufffd\ufffc";
                a.J = "1122334455667788990*#";
            }
            if (var7_13 <= 176) {
                a.r = true;
            }
        }
        if (a.g == 1) {
            a.X = a.X != false && a.p != 0 && a.q != 0;
            a.S = true;
            a.j = 1024;
            a.r = 290;
            if (var7_13 <= 176) {
                a.B = true;
            }
            if (var7_13 >= 240) {
                a.G = true;
            }
            if (var2_3.indexOf("symbian") >= 0) {
                a.l = true;
                a.ak = true;
            }
            if (var5_9) {
                a.i = false;
                a.j = false;
                a.Z = "ertdfgxcv+zb";
                a.J = "1234567890*#";
            }
            if (a.Code("com.mot.iden.multimedia.Lighting")) {
                a.Code = true;
                a.Z = true;
                a.ak = true;
                a.aj = true;
                a.c = true;
                a.e = true;
                a.h = true;
                a.k = true;
            }
        }
        if (a.g == 4) {
            if (var3_4.startsWith("sgh-f700")) {
                a.Q = true;
                a.w = 2;
            }
            if (var3_4.endsWith("sgh-gt-b2700")) {
                a.f = false;
                a.g = false;
            }
            if (var2_3.equals("j2me") && var4_7 == 2 && var10_17 && var7_13 == 240 && var8_14 == 286 && var9_15 == 65536) {
                a.x = true;
                a.j = 256;
            }
            if (var7_13 == 240 && var8_14 == 279 && var9_15 == 65536 && var4_7 == 256 && var10_17) {
                a.w = true;
            }
            if (var7_13 <= 176 && var4_7 == 2) {
                a.af = true;
            }
            a.v = true;
            if (var7_13 == 320 && var8_14 == 186 && var9_15 == 65536 && var4_7 == 65536) {
                a.ac = true;
            }
            a.Z = true;
            if (var7_13 == 128 && var8_14 == 144 || var7_13 == 176 && var8_14 == 204 || var7_13 == 220 && var8_14 == 160) {
                a.Code = true;
            }
            a.G = true;
            if (var2_3.startsWith("gt-m8800")) {
                a.L = true;
            }
            if (var2_3.startsWith("sgh-p310") || var1_1.indexOf("x820") != -1 || var1_1.indexOf("sghd830") != -1) {
                a.X = false;
            }
            if (var1_1.indexOf("sgh-e250") != -1 || var1_1.indexOf("sgh-e370") != -1 || var1_1.indexOf("sgh-d900") != -1 || var1_1.indexOf("sphm500") != -1 || var1_1.indexOf("sphm610") != -1 || var1_1.indexOf("spha800") != -1 || var1_1.indexOf("spha900") != -1) {
                a.P = true;
            }
        }
        if (var2_3.indexOf("sxg75") >= 0 || var2_3.indexOf("ef81") >= 0) {
            a.b = true;
            a.g = 2;
        } else if (a.g == 2) {
            a.C = true;
            a.c = true;
            a.ak = true;
            a.P = true;
            a.A = true;
            if (var7_13 < 176) {
                a.I = true;
            }
            if (var2_3.indexOf("s68") >= 0) {
                a.u = true;
            }
            if (var2_3.indexOf("el71") >= 0 || var2_3.indexOf("e71") >= 0) {
                a.O = true;
            }
            if (var2_3.indexOf("c6") >= 0 || var2_3.indexOf("cx6") >= 0 || var2_3.indexOf("m6") >= 0 || var2_3.indexOf("sl75") >= 0) {
                a.al = true;
            }
        }
        if (a.g == 8 && var2_3.startsWith("vs")) {
            a.S = true;
            a.ah = true;
        }
        if (a.g == 6) {
            a.P = true;
            a.ae = true;
            a.N = true;
            a.i = 2000;
            a.ac = true;
            a.al = true;
            a.B = true;
            a.Y = true;
            a.d = true;
            a.h = 79;
            if (!Browser.Code.isDoubleBuffered()) {
                a.A = true;
            }
        }
        if (a.g == 7) {
            a.Z = "wersdfzxc\u0089aq";
            a.J = "1234567890*#";
            a.ag = true;
            a.ah = true;
            a.N = true;
            a.al = true;
            var11_18 = true;
        }
        if (a.g == -1) {
            if (var2_3.startsWith("wx320kr")) {
                a.ah = true;
            }
            if (var2_3.startsWith("windows ce")) {
                a.b = true;
                if (!Browser.Code.hasPointerEvents() || var2_3.indexOf("palm treo 700w") >= 0) {
                    a.ah = true;
                }
                a.ae = true;
            }
            if (var5_9) {
                a.S = true;
                if (var2_3.equals("j2me") != false && var7_13 == 320 && var8_14 == 186 || var7_13 == 240 && var8_14 == 268 || var7_13 == 320 && var8_14 == 250) {
                    var3_4 = Font.getFont((int)64, (int)0, (int)8);
                    var5_10 = Font.getFont((int)64, (int)1, (int)8);
                    var12_19 = Font.getFont((int)64, (int)1, (int)16);
                    if (var5_10.stringWidth(a.Code(7)) <= var3_4.stringWidth(a.Code(7))) {
                        if (var5_10.stringWidth(a.Code(7)) <= var12_19.stringWidth(a.Code(7))) {
                            a.aJ = true;
                            a.Code = new byte[6][2048];
                            for (var3_5 = 0; var3_5 < 6; ++var3_5) {
                                for (var5_11 = 0; var5_11 < 2048; ++var5_11) {
                                    a.Code[var3_5][var5_11] = -1;
                                }
                            }
                        } else {
                            a.e = true;
                        }
                    }
                }
            } else if (var2_3.equals("j2me")) {
                a.b = true;
                if (var7_13 == 128 && var8_14 == 129) {
                    a.m = true;
                }
            }
            if (var2_3.equals("jbed")) {
                a.y = true;
                a.ad = true;
                if (var7_13 == 128 && (var8_14 == 110 || var8_14 == 142)) {
                    a.m = true;
                }
                if (var1_1.startsWith("semc-venus")) {
                    a.w = 1;
                    a.ao = true;
                }
            }
            if (var2_3.equals("jbed-fastdac")) {
                a.ah = true;
                a.b = true;
                a.j = 260;
            }
            if (var2_3.equals("intent jte")) {
                if (!Browser.Code.hasPointerEvents()) {
                    a.ah = true;
                }
                a.ai = true;
            }
            if (var2_3.equals("jbed-fastbcc") && var7_13 == 240 && var8_14 == 325 && var9_15 == 65536 && var4_7 == 256 && var10_17) {
                a.w = 2;
                a.f = true;
                a.g = true;
            }
            if (var2_3.equals("microemulator-android")) {
                a.c = true;
                a.q = true;
                a.w = 1;
                a.n = 0;
                a.ah = true;
            }
            a.G = var1_1.indexOf("lg-kg280") < 0;
            if (var2_3.startsWith("maui") || var2_3.startsWith("fly-sx210")) {
                a.al = true;
            }
            if (var2_3.startsWith("lg-kc910")) {
                a.L = true;
            }
            if (var2_3.startsWith("lg-kc550")) {
                a.f = false;
                a.g = false;
            }
            if (var2_3.startsWith("htc touch diamond p3700") || var2_3.startsWith("htc touch pro t7272") || var2_3.startsWith("htc touch hd t8282")) {
                a.w = 2;
            }
            if (var2_3.startsWith("htc s740")) {
                a.ao = true;
                a.an = true;
                a.al = true;
            }
            if (var2_3.startsWith("htc touch pro t7272")) {
                a.ao = true;
            }
            if (var2_3.startsWith("amoi8512")) {
                a.ap = true;
            }
        }
        if (a.Code("com.sprintpcs.util.System") || var6_12 != null && var6_12.indexOf("SPRINTPCS-1.0") >= 0) {
            if (var2_3.endsWith("sph-m800") || var2_3.endsWith("sph-m810")) {
                a.n = 0;
                a.b = true;
                a.N = true;
            }
            a.ad = true;
            var11_18 = true;
            if (a.g == -1) {
                if (var7_13 == 176 && (var8_14 == 177 || var8_14 == 182) || var7_13 == 128 && var8_14 == 136) {
                    a.S = true;
                }
                if (var7_13 == 176 && var8_14 == 185 && var9_15 == 65536 && var4_7 == 256) {
                    a.j = false;
                }
                if (Browser.Code.hasRepeatEvents() && var4_7 == 256) {
                    a.P = true;
                }
            }
        }
        if (!var11_18) {
            v1 = a.aa = a.Code("javax.wireless.messaging.MessageConnection") != false && a.Code("javax.wireless.messaging.TextMessage") != false;
        }
        if (var1_1.indexOf("spha900") != -1 || var1_1.indexOf("spha920") != -1) {
            a.am = true;
        } else {
            var1_1 = Image.createImage((int)10, (int)20);
            var2_3 = var1_1.getGraphics();
            var3_6 = Image.createImage((int)10, (int)10);
            var4_8 = new Sprite(var3_6);
            var3_6 = var3_6.getGraphics();
            var4_8.setTransform(5);
            var3_6.setColor(0);
            var3_6.fillRect(0, 0, 5, 5);
            var4_8.setPosition(0, 0);
            var4_8.paint((Graphics)var2_3);
            var3_6.setColor(0xFFFFFF);
            var3_6.fillRect(0, 0, 10, 10);
            var3_6.setColor(0);
            var3_6.fillRect(5, 0, 5, 5);
            var4_8.setPosition(0, 10);
            var4_8.paint((Graphics)var2_3);
            var2_3 = new int[1];
            var1_1.getRGB((int[])var2_3, 0, 1, 9, 19, 1, 1);
            if ((var2_3[0] & 0xFFFFFF) != 0) {
                a.al = true;
            }
        }
        a.J();
        if (!a.aD) {
            a.I("eurls");
            a.I("b");
            a.I("se");
        }
        a.C();
        a.B();
        if (a.X) {
            try {
                this.Code = new J();
            }
            catch (Throwable v2) {
                a.X = false;
            }
        }
        f.Code = a.f + " hifi" + (a.g.length() > 0 ? " " : "") + a.g + (a.h.length() > 0 ? " " : "") + a.h + " v. 4.2.13328, 20081204";
        f.I = a.f + "/4.2.13328/hifi/" + a.g + "/" + a.h;
        this.b = a.f;
        var1_2 = a.e ^ 1;
        a.Code = new Font[6];
        a.Code[0] = Font.getFont((int)64, (int)0, (int)8);
        a.Code[1] = Font.getFont((int)64, (int)var1_2, (int)8);
        a.Code[2] = Font.getFont((int)64, (int)0, (int)0);
        a.Code[3] = Font.getFont((int)64, (int)var1_2, (int)0);
        a.Code[4] = Font.getFont((int)64, (int)0, (int)16);
        a.Code[5] = Font.getFont((int)64, (int)var1_2, (int)16);
        if (!a.z && !a.ah) {
            this.setFullScreenMode(true);
        }
        Browser.Code.setCurrent((Displayable)this);
        new Thread(this).start();
    }

    private void h() {
        int n;
        int n2 = l;
        if (a.r) {
            int n3 = n2 * 10;
            int[] nArray = new int[n3];
            for (int i = 0; i < n3; ++i) {
                nArray[i] = -1;
            }
            Image.createRGBImage((int[])nArray, (int)n2, (int)10, (boolean)false);
        }
        int[] nArray = new int[n2 << 2];
        for (n = 0; n < n2; ++n) {
            nArray[n] = -1291845632;
            nArray[n + n2] = 0x73000000;
            nArray[n + 2 * n2] = 0x33000000;
            nArray[n + 3 * n2] = 0x1A000000;
        }
        this.b = Image.createRGBImage((int[])nArray, (int)n2, (int)4, (boolean)true);
        nArray = new int[n2];
        for (n = 0; n < n2; ++n) {
            nArray[n] = 0x73000000;
        }
        this.c = Image.createRGBImage((int[])nArray, (int)n2, (int)1, (boolean)true);
    }

    public static void b() {
        if (!i) {
            i = true;
            Browser.Code.repaint();
            if (a.g == 6) {
                Browser.Code.serviceRepaints();
            }
        }
        j = true;
    }

    public static void Code(f f2, int n) {
        f2.J(n);
    }

    public final synchronized void Code(String string, String object, String string2) {
        if (string != null) {
            String string3 = a.I(string);
            if (string3.equals(a.I(66))) {
                d = false;
            } else if (string3.startsWith("rtsp://") || string.startsWith("rtsp://")) {
                object = new g(6);
                new g(6).Code = string.startsWith("rtsp://") ? string : string3;
                a.Code((Runnable)object);
                return;
            }
            if (!a.aG && (string.equals(a.I(65)) || string.equals(a.j))) {
                a.aG = true;
                a.Code(false);
            }
            f.Code().Code(string, (String)object, string2);
        }
    }

    private static String Code(Command command) {
        if (command == Code[0]) {
            return "menu:" + a.I(66);
        }
        if (command == Code[11]) {
            return "dialog:" + a.I(32);
        }
        if (command == Code[2]) {
            return "menu:" + a.I(68);
        }
        if (command == Code[4]) {
            return a.I(81);
        }
        if (command == Code[3]) {
            return a.I(110);
        }
        if (command == Code[9] || command == Code[36]) {
            return a.I(113);
        }
        if (command == Code[10]) {
            return a.I(114);
        }
        if (command == Code[1] || command == Code[29]) {
            return a.I(115);
        }
        if (command == Code[7]) {
            return a.I(116);
        }
        if (command == Code[5]) {
            return a.I(121);
        }
        if (command == Code[6]) {
            return a.I(122);
        }
        if (command == Code[12] || command == Code[38]) {
            return a.I(117);
        }
        if (command == Code[13]) {
            return a.j;
        }
        if (command == Code[20]) {
            return a.I(92);
        }
        if (command == Code[22]) {
            return a.I(96);
        }
        if (command == Code[8]) {
            return a.I(93);
        }
        if (command == Code[21]) {
            return a.I(94);
        }
        if (command == Code[17]) {
            return a.I(83);
        }
        if (command == Code[37]) {
            return "openp:" + a.m;
        }
        if (command == Code[27]) {
            return a.I(59);
        }
        if (command == Code[24]) {
            return a.I(51);
        }
        if (command == Code[25]) {
            return "menu:" + a.I(128);
        }
        if (command == Code[26]) {
            return a.I(132);
        }
        return null;
    }

    public static Code Code() {
        return f.Code().Code;
    }

    public static boolean Code(int n, boolean bl) {
        a a2 = f.Code(a.I(n));
        if (a2 != null && (bl || a2.I == null)) {
            a2.aR = true;
            a2.c();
            return true;
        }
        return false;
    }

    private static boolean a() {
        Object object = f.Code(1);
        return object != null && ((a)object).Code != null && ((object = ((a)object).Code.Z()) == null || !((String)object).startsWith("myopera:///b/g/"));
    }

    private static boolean B() {
        a a2 = f.Code();
        return a2 != null && a2.I(16);
    }

    /*
     * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static void I(int var0, int var1_1) {
        block12: {
            if (var0 < 2 && var0 >= 0) ** GOTO lbl45
            try {
                var2_4 = new Vector<E>();
                var3_6 = new Vector();
                for (var4_7 = f.I.size() - 1; var4_7 >= 0; --var4_7) {
                    f.Code(var0, var2_4, var3_6, (Object[])f.I.elementAt(var4_7));
                }
                for (var4_7 = 0; var4_7 < f.Z.size(); ++var4_7) {
                    var5_9 = (a)f.Z.elementAt(var4_7);
                    for (var6_10 = var5_9.B.size() - 1; var6_10 >= 0; --var6_10) {
                        f.Code(var0, (Vector)var2_4, (Vector)var3_6, (Object[])var5_9.B.elementAt(var6_10));
                    }
                }
                var0 = var3_6.size() - 1;
lbl15:
                // 3 sources

                while (var0 >= 0) {
                    var4_8 /* !! */  = (Object[])var3_6.elementAt(var0);
                    var5_9 = (Code)var4_8 /* !! */ [8];
                    if (var2_4.contains(var5_9)) {
                        var5_9.Code(2);
                        var4_8 /* !! */ [8] = null;
                        --var0;
                        continue;
                    }
                    ** GOTO lbl-1000
                }
                ** GOTO lbl45
            }
            catch (OutOfMemoryError v0) {
                for (var0 = 0; var0 < f.I.size(); ++var0) {
                    var1_2 = (Object[])f.I.elementAt(var0);
                    var2_4 = (Code)var1_2[8];
                    if (var2_4 == null || var2_4.Code.Code == var2_4) continue;
                    var1_2[8] = null;
                }
                for (var0 = 0; var0 < f.Z.size(); ++var0) {
                    var1_3 = (a)f.Z.elementAt(var0);
                    for (var2_5 = 0; var2_5 < var1_3.B.size(); ++var2_5) {
                        var3_6 = (Object[])var1_3.B.elementAt(var2_5);
                        var4_8 /* !! */  = (Code)var3_6[8];
                        if (var4_8 /* !! */  == null || var4_8 /* !! */ .Code.Code == var4_8 /* !! */ ) continue;
                        var3_6[8] = null;
                    }
                }
                break block12;
            }
lbl-1000:
            // 1 sources

            {
                var5_9.Code(var1_1 + 1);
                --var0;
                ** GOTO lbl15
lbl45:
                // 2 sources

                var0 = f.Z.size();
                while (var0-- > 0) {
                    ((a)f.Z.elementAt(var0)).Z(var1_1);
                }
                if (var1_1 < 1) break block12;
                a.Code(null);
            }
        }
        System.gc();
    }

    private static void Code(int n, Vector vector, Vector vector2, Object[] objectArray) {
        Code code = (Code)objectArray[8];
        if (code != null && !vector2.contains(objectArray) && code.Code.Code != code) {
            vector2.addElement(objectArray);
            if (!vector.contains(code)) {
                if (n < 0) {
                    for (n = 0; n < vector.size() && ((Code)vector.elementAt((int)n)).Code <= code.Code; ++n) {
                    }
                    vector.insertElementAt(code, n);
                    if (vector.size() > 5) {
                        vector.removeElementAt(vector.size() - 1);
                        return;
                    }
                } else {
                    vector.addElement(code);
                }
            }
        }
    }

    public final void I() {
        block10: {
            byte[] byArray = a.Code(8);
            Object[] objectArray = new Object[3];
            a.Code(byArray, 0, byArray.length, 256, objectArray);
            if (i < 18) {
                objectArray[0] = a.Code((int[])objectArray[0], (int)((Integer)objectArray[1]), (int)((Integer)objectArray[2]), 12, 1, true);
                objectArray[1] = new Integer(((int[])objectArray[0]).length / 12);
                objectArray[2] = new Integer(12);
            }
            f2.C = Image.createRGBImage((int[])((int[])objectArray[0]), (int)((Integer)objectArray[1]), (int)((Integer)objectArray[2]), (boolean)true);
            f2.q = (i - f2.C.getHeight() >> 1) - 1;
            f2.a = a.Code(objectArray, f2.I, -f2.q);
            if (Code.I == null) {
                Code.I = new Image[4];
                f f2 = a.Code(2);
                if (f2 != null) {
                    Code.I[0] = a.Code((Image)f2, 0, 0, 11, 10, 0);
                    Code.I[1] = a.Code((Image)f2, 0, 10, 11, 10, 0);
                    Code.I[2] = a.Code((Image)f2, 0, 20, 11, 10, 0);
                    Code.I[3] = a.Code((Image)f2, 0, 30, 11, 10, 0);
                    if (a.i) {
                        d = a.Code((Image)f2, 0, 40, 4, 4, 0);
                        h = a.Code((Image)f2, 4, 40, 4, 4, 0);
                        f = a.Code((Image)f2, 0, 44, 4, 4, 0);
                        g = a.Code((Image)f2, 4, 44, 1, 4, 0);
                        e = a.Code((Image)f2, 4, 44, 1, 4, 6);
                    }
                }
            }
            if (Code.Code == null) {
                Code.Code = new Image[6];
                if (a.j) {
                    int n = 0;
                    while (true) {
                        int n2 = ++n;
                        if (a.q || a.l ? n2 < 5 : n2 < 6) {
                            Code.Code[n] = a.Code(71 + n);
                            continue;
                        }
                        break block10;
                        break;
                    }
                }
                for (int i = 0; i < 5; ++i) {
                    Code.Code[i] = a.Code(77 + i);
                }
            }
        }
        if (Code == null) {
            Code = a.Code(3);
        }
    }

    public final synchronized void commandAction(Command object, Displayable object2) {
        if (this.C) {
            this.C = false;
            return;
        }
        if (object == Code[3] && f.Code().aS) {
            f.Code().f();
            return;
        }
        if (a.T && object == Code[4] && this.r == -11) {
            this.C = true;
        }
        if (object == Code[14]) {
            Browser.Code();
            return;
        }
        if (this.C()) {
            return;
        }
        if (object2 == this) {
            this.I(f.Code((Command)object));
            return;
        }
        f f2 = this;
        if (object == Code[4] || object == Code[10]) {
            if (object2 instanceof List) {
                f2 = this.Code;
            }
        } else if (object == Code[9] || object == List.SELECT_COMMAND || object == Code[19]) {
            if (object2 instanceof List) {
                object = (List)object2;
                int n = object.getSelectedIndex();
                if (n >= 0) {
                    this.Code.setString(object.getString(n));
                }
                f2 = this.Code;
            } else if (object2 instanceof TextBox) {
                block44: {
                    CharSequence charSequence = ((TextBox)object2).getString();
                    if (f.Code() != null && f.Code().w != 97 && ((String)charSequence).indexOf(10) != -1 || ((String)charSequence).indexOf(8232) != -1 || ((String)charSequence).indexOf(8233) != -1) {
                        charSequence = new StringBuffer((String)charSequence);
                        a.Code((StringBuffer)charSequence, 8233, 10);
                        a.Code((StringBuffer)charSequence, 8232, 10);
                        if (f.Code().w != 97) {
                            a.Code((StringBuffer)charSequence, 10, object2 == this.Code ? 0 : 32);
                        }
                        charSequence = ((StringBuffer)charSequence).toString();
                    }
                    if (object == Code[19]) {
                        this.I(a.I(47));
                    } else if (f.Code().I(16)) {
                        f.Code().Z((String)charSequence);
                    } else if (a.Code(f.Code().Code) == 109) {
                        this.I((String)a.Code(a.Z, a.A)[0], (String)charSequence);
                    } else if (f.Code().ao == 127) {
                        try {
                            object = f.Code();
                            object2 = f.Code();
                            if (((Code)object).Code((String)charSequence) == 0) {
                                ((a)object2).Code("overlay:" + a.I(46), null, null);
                                break block44;
                            }
                            ((Code)object).I(1);
                            ((a)object2).aS = true;
                            ((Code)object).Z = ((Code)object).Code;
                            ((Code)object).J = ((Code)object).I;
                            ((Code)object).Code = 24;
                            ((Code)object).I = 3;
                        }
                        catch (Throwable throwable) {}
                    } else if (a.Code(f.Code().Code) == 60) {
                        object = charSequence;
                        a.Code((Runnable)new a(this, (String)object));
                    } else if (!f.Code().Code((String)charSequence, false)) {
                        this.Z((String)charSequence);
                    }
                }
                if (a.t) {
                    object = new Alert(a.f, a.Code(22), null, AlertType.INFO);
                    object.setTimeout(50);
                    Browser.Code.setCurrent((Alert)object, (Displayable)this);
                    f2 = null;
                }
            }
        } else {
            f2 = null;
            if (object == Code[15]) {
                object = this;
                if (a.s) {
                    object.setFullScreenMode(false);
                }
                object2 = new List(a.Code(6), 3);
                for (int i = Code.size() - 1; i >= 0; --i) {
                    String string = (String)Code.elementAt(i);
                    object2.append(string, null);
                }
                object2.setSelectCommand(Code[9]);
                object2.addCommand(Code[4]);
                f.Code((Screen)object2);
                object2.setCommandListener((CommandListener)object);
                Browser.Code.setCurrent((Displayable)object2);
            } else if (object == Code[18]) {
                try {
                    object = f.Code(1);
                    if (a.Code(((a)object).Code.Code) == -1) {
                        this.Code.setString(((a)object).Code.Code);
                    }
                }
                catch (Throwable throwable) {
                    this.Code.setString("www.");
                }
            } else if (object == Code[11] && this.Code != null && Browser.Code.getCurrent() == this.Code) {
                this.Code.setString("www.");
            }
        }
        if (a.s && f2 == this) {
            this.setFullScreenMode(true);
        }
        if (f2 != null) {
            Browser.Code.setCurrent((Displayable)f2);
        }
    }

    public final void J(String string) {
        if (a.s) {
            this.setFullScreenMode(false);
        }
        if (a.n) {
            this.Code = f.Code();
        } else {
            this.Code.removeCommand(Code[9]);
            this.Code.removeCommand(Code[10]);
            this.Code.removeCommand(Code[15]);
            this.Code.removeCommand(Code[18]);
            this.Code.removeCommand(Code[14]);
            this.Code.removeCommand(Code[11]);
        }
        this.Code.addCommand(Code[9]);
        this.Code.addCommand(Code[10]);
        this.Code.addCommand(Code[11]);
        if (Code.size() > 0) {
            this.Code.addCommand(Code[15]);
        }
        if (a.Code(f.Code((int)1).Code.Code) == -1) {
            this.Code.addCommand(Code[18]);
        }
        f.Code((Screen)this.Code);
        this.Code.setCommandListener((CommandListener)this);
        if (string == null) {
            this.Code.setString("www.");
        } else {
            this.Code.setString(string);
        }
        Browser.Code.setCurrent((Displayable)this.Code);
        a.Code(this.Code);
    }

    public final void a() {
        if (this.I != null) {
            this.I.cancel();
            this.I = null;
        }
    }

    private static void Code(Screen screen) {
        if (screen != null && a.l) {
            screen.addCommand(Code[14]);
        }
    }

    public final void C() {
        if (this.I == null) {
            Z = System.currentTimeMillis();
            this.I = new g(1);
            this.Code.schedule(this.I, 0L, (long)500);
        }
    }

    private static String Code() {
        Code code = f.Code();
        if (code != null) {
            return code.Code;
        }
        return null;
    }

    public static void Code(String string) {
        f.Code(a.Code(37), string);
    }

    public final boolean I(a a2) {
        boolean bl = false;
        if (a2.I(16)) {
            a2.Code.h();
        }
        if (Z.size() > 1) {
            a2.aT = true;
            Z.removeElement(a2);
            this.C();
            bl = true;
        }
        this.repaint();
        return bl;
    }

    public final void I(boolean bl) {
        if (!bl && C != 1) {
            this.removeCommand(Code[1]);
        } else if (!(C == 36 || b != 23 || f.Code().I(8) && C == 7)) {
            this.addCommand(Code[1]);
        }
        a = bl;
    }

    public final void run() {
        this.setCommandListener(this);
        a.a();
        l = n = this.getWidth();
        m = o = this.getHeight();
        if (a.F) {
            l = o;
            m = n;
        }
        this.Code(a.F);
        int n = a.aA;
        f f2 = this;
        if (n != 0) {
            if (f2.Code == null) {
                f2.Code = new g(2);
                n = Calendar.getInstance().get(13);
                f2.Code.scheduleAtFixedRate(f2.Code, (61 - n) * 1000, 60000L);
                f2.e();
            }
        } else if (f2.Code != null) {
            f2.Code.cancel();
            f2.Code = null;
            f2.B();
        }
        ++this.Code;
        if (a.s < 0) {
            this.j = Image.createImage((int)80, (int)80);
            if (a.ak) {
                a.s = 3;
            } else {
                long l = System.currentTimeMillis() + (long)1000;
                do {
                    this.j = a.Code(this.j, 0, 80 - (a.s << 1), 1, true);
                } while (System.currentTimeMillis() < l && ++a.s < 10);
            }
            if (a.s < a.t) {
                a.aj = true;
                a.aw = false;
            }
        }
        if (a.u < 0) {
            this.j = Image.createImage((int)f.n, (int)o);
            Graphics graphics = Image.createImage((int)f.n, (int)o).getGraphics();
            long l = System.currentTimeMillis() + (long)500;
            do {
                ++a.u;
                graphics.drawImage(this.j, 0, 0, 20);
            } while (System.currentTimeMillis() < l);
            int n2 = 10;
            if (a.g == 0) {
                n2 = 87;
            }
            if (a.u < n2) {
                a.aw = false;
            }
        }
        this.repaint();
        this.serviceRepaints();
        f f3 = this;
        if (a.ag) {
            if (Code[10] != null) {
                f3.removeCommand(Code[10]);
            }
            if (Code[7] != null) {
                f3.removeCommand(Code[7]);
            }
        }
        int n3 = 4;
        int n4 = 3;
        int n5 = 2;
        int n6 = 6;
        int n7 = 8;
        switch (a.g) {
            case 3: {
                n4 = 2;
                break;
            }
            case 2: {
                n4 = 8;
            }
        }
        if (a.ah) {
            n7 = 1;
            n5 = 1;
            n6 = 1;
            n4 = 1;
            n3 = 8;
        }
        if (a.ag) {
            n6 = 3;
            n5 = 3;
            n4 = 3;
        }
        if (a.U) {
            n4 = 8;
        }
        if (a.Q) {
            n3 = 3;
            n4 = 8;
        }
        f.Code[0] = new Command(a.Code(28), 1, 1);
        f.Code[1] = new Command(a.Code(43), 1, 1);
        f.Code[2] = new Command(a.Code(70), 1, 1);
        f.Code[10] = new Command(a.Code(1), n4, 10);
        f.Code[3] = new Command(a.Code(3), n6, 20);
        f.Code[15] = new Command(a.Code(6), 8, 20);
        f.Code[18] = new Command(a.Code(106), 8, 20);
        f.Code[11] = new Command(a.Code(19), n7, 20);
        f.Code[17] = new Command(a.Code(5), 1, 20);
        f.Code[12] = new Command(a.Code(4), n5, 10);
        f.Code[14] = new Command(a.Code(10), 7, 10);
        f.Code[4] = new Command(a.Code(4), n5, 10);
        f.Code[5] = new Command(a.Code(11), n3, 1);
        f.Code[6] = new Command(a.Code(12), n4, 10);
        f.Code[8] = new Command(a.Code(62), 1, 1);
        f.Code[9] = new Command(a.Code(0), n3, 1);
        f.Code[7] = new Command(a.Code(17), 1, 1);
        f.Code[16] = new Command(a.Code(66), 1, 10);
        f.Code[13] = new Command(a.Code(104), 1, 1);
        f.Code[19] = new Command(a.Code(108), 8, 20);
        f.Code[20] = new Command(a.Code(21), 1, 1);
        f.Code[21] = new Command(a.Code(109), 1, 1);
        f.Code[22] = new Command(a.Code(135), 1, 1);
        f.Code[37] = new Command(a.Code(35), 1, 1);
        f.Code[38] = new Command(a.Code(3), n5, 1);
        f.Code[27] = new Command(a.Code(174), 1, 1);
        f.Code[28] = new Command("", 1, 1);
        f.Code[29] = new Command(a.Code(15), n3, 1);
        f.Code[24] = new Command(a.Code(142), 1, 1);
        f.Code[25] = new Command(a.Code(28), 1, 1);
        f.Code[26] = new Command(a.Code(14), n4, 10);
        f.Code[36] = new Command(a.Code(160), n3, 1);
        if (Code[23] == null) {
            f.Code[23] = new Command("", 2, 1);
        }
        C = 23;
        a = 23;
        f3.Code = f.Code();
        this.Code();
        a.i();
        if (a.f) {
            a.ai = Math.max(6, Math.min(f.n, o) / 26);
        }
        Z.addElement(new a(1, null));
        I = a.Code(true, "h1", 0);
        if (I == null) {
            I = new Vector(30);
        }
        a.Code = a.Code(true, "b1", 0);
        a.I = a.Code(true, "sp", 0);
        a.Z = a.Code(true, "u", 0);
        if (a.Code == null) {
            a.Code = new Vector();
        }
        boolean bl = false;
        if (a.I == null) {
            a.I = new Vector();
            bl = true;
        }
        while (a.I.size() < 9) {
            Object[] objectArray = a.Code();
            Object[] objectArray2 = objectArray;
            objectArray[5] = new Integer(83);
            objectArray2[6] = new Integer(0);
            a.I.addElement(objectArray2);
        }
        if (a.Z == null) {
            a.Z = a.Code(false, null, 67);
        } else if (a.as) {
            Vector vector = a.Code(false, null, 67);
            if (vector.size() > 0) {
                a.Z.setElementAt(vector.elementAt(0), 0);
            }
            a.Code(a.Z, false);
        }
        if (!a.ar) {
            Vector vector = a.Code(false, null, 66);
            n4 = 0;
            block7: for (n5 = 0; n5 < vector.size(); ++n5) {
                Object[] objectArray;
                block53: {
                    objectArray = (Object[])vector.elementAt(n5);
                    if (a.Code((String)objectArray[0]) != -1) continue;
                    String string = a.Z((String)objectArray[0]);
                    for (int i = 0; i < a.Code.size(); ++i) {
                        if (!a.Z((String)((Object[])a.Code.elementAt(i))[0]).equals(string)) continue;
                        if (true) continue block7;
                        break block53;
                    }
                    if (false) continue;
                }
                if (a.Code(objectArray, 1)) {
                    if (!bl) continue;
                    a.I.setElementAt(objectArray, n4++);
                    continue;
                }
                a.Code.addElement(objectArray);
            }
            a.Code(a.Code, true);
            a.Code(a.I, true);
        }
        a.ar = true;
        if (!a.at) {
            bl = false;
            for (int i = 0; i < a.Code.size() - 1; ++i) {
                Object[] objectArray = (Object[])a.Code.elementAt(i);
                byte[] byArray = (byte[])objectArray[7];
                for (n6 = i + 1; n6 < a.Code.size(); ++n6) {
                    Object[] objectArray3 = (Object[])a.Code.elementAt(n6);
                    if (!a.Code(byArray, (byte[])objectArray3[7])) continue;
                    bl = true;
                    if (objectArray != null) {
                        if (a.Code(objectArray, 5) != 97 && a.Code(objectArray, 5) != 82) {
                            objectArray[5] = new Integer(65);
                        }
                        objectArray[7] = a.Code();
                        objectArray = null;
                    }
                    if (a.Code(objectArray3, 5) != 97 && a.Code(objectArray3, 5) != 82) {
                        objectArray3[5] = new Integer(65);
                    }
                    objectArray3[7] = a.Code();
                }
            }
            if (bl) {
                a.at = true;
                a.Code(a.Code, false);
                a.Code(false);
            }
        }
        a.I();
        if (a.Code > 200000L) {
            a.J = true;
        }
        if (a.Code < (long)((this.getWidth() * this.getHeight() << 2) + 50000)) {
            a.al = true;
        }
        ++this.Code;
        this.repaint();
        this.serviceRepaints();
        bl = false;
        if (!a.aL) {
            bl = true;
            a.aG = false;
            this.f();
            while (c) {
                a.Code(this, 0);
            }
            if (!a.aL) {
                this.e = true;
                this.repaint();
                return;
            }
        }
        if (!a.b) {
            Image image = a.Code(6);
            a.a = image != null && image.getWidth() == 1;
        }
        this.I();
        ++this.Code;
        this.repaint();
        this.serviceRepaints();
        if (!bl) {
            this.f();
        }
        this.e = true;
        this.repaint();
        this.serviceRepaints();
        a.Code(false);
    }

    public final void Code() {
        byte[] byArray;
        int n;
        a.J[a.Code((int)0)] = -128;
        a.Z[a.Code((int)0)] = -128;
        a.J[a.Code((int)1)] = -128;
        a.Z[a.Code((int)1)] = -128;
        for (n = 0; n <= 14; ++n) {
            a.Code(a.Code(0), Code[n].getLabel());
            a.Code(a.Code(1), Code[n].getLabel());
        }
        for (n = 20; n <= 23; ++n) {
            a.Code(a.Code(0), a.Code(n));
            a.Code(a.Code(1), a.Code(n));
        }
        a.Z[6] = a.J[a.Code(0)];
        a.Z[7] = a.J[a.Code(1)];
        a.J[6] = a.J[a.Code(0)];
        a.J[7] = a.J[a.Code(1)];
        for (n = 0; n < 6; ++n) {
            a.J[n] = -128;
            a.Z[n] = -128;
            a.Code(n, "\u00c5\u8ba4\u5343I[jg");
        }
        a.Z[6] = (byte)(a.Z[6] - a.J[a.Code(0)]);
        a.Z[7] = (byte)(a.Z[7] - a.J[a.Code(1)]);
        a.J[6] = (byte)(a.J[6] - a.J[a.Code(0)]);
        a.J[7] = (byte)(a.J[7] - a.J[a.Code(1)]);
        i = Math.max(a.Z(0), a.Z(1)) + 3;
        j = Math.max(10, a.J(1)) + 3;
        if (a.f && a.g != 6) {
            i += a.Q * 6 + 4;
        }
        i = Math.max(i, i < 16 ? 14 : 18);
        Code = new int[47];
        this.I = new int[j];
        Image image = null;
        if (!a.aI && (byArray = a.Code(a.E, "b")) != null) {
            image = Image.createImage((byte[])byArray, (int)0, (int)byArray.length);
        }
        if (image == null) {
            image = a.Code("/s.png");
        }
        if (image == null) {
            image = a.Code(68);
        }
        if (image != null) {
            image.getRGB(Code, 0, Code.length, 0, 21, Code.length, 1);
            for (int i = 0; i < Code.length; ++i) {
                f.Code[i] = Code[i] & 0xFFFFFFFF;
            }
            if (image.getWidth() > 62) {
                this.J = a.Code(Image.createImage((Image)image, (int)62, (int)0, (int)20, (int)22, (int)0), 1, j, 1, false);
            }
            if (image.getWidth() > 82) {
                this.B = a.Code(Image.createImage((Image)image, (int)82, (int)0, (int)20, (int)22, (int)0), 1, j, 1, false);
            }
            this.Z = a.Code(Image.createImage((Image)image, (int)42, (int)0, (int)20, (int)20, (int)0), 0, i - 1, 1, false);
            image = a.Code(Image.createImage((Image)image, (int)0, (int)0, (int)42, (int)22, (int)0), 1, j, 1, false);
            Image.createImage((Image)image, (int)0, (int)0, (int)1, (int)j, (int)0).getRGB(this.I, 0, 1, 0, 0, 1, j);
        }
        this.I = image;
        a.g = Code;
        k = Math.min(16, j - 4);
        a.Code(null);
        f.k();
    }

    private static void Code(Graphics graphics, int n, String string, int n2, int n3) {
        int n4 = m - (i >> 1) - 1 - (a.J(n) + a.Z[n + 6] >> 1);
        a.Code(graphics, n, string, n2, n4, n3);
    }

    private void Z(int n) {
        for (int i = 1; i < this.B.length; ++i) {
            this.B[i - 1] = this.B[i];
        }
        this.B[this.B.length - 1] = n;
        n = 0;
        while (this.B[n] == J[n]) {
            if (n == this.B.length - 1) {
                a.au = !a.au;
                try {
                    RecordStore.deleteRecordStore((String)"g");
                }
                catch (Throwable throwable) {}
                Browser.Code.vibrate(500);
                return;
            }
            ++n;
        }
    }

    private void j() {
        this.g = -1;
    }

    private static boolean J() {
        return !a.aG && a.k != null && a.k.length() > 0;
    }

    public final void I(String string) {
        this.Code(string, null, null);
    }

    public final void I(int n) {
        f.I(n - 1, n);
    }

    private static int I(int n, int n2) {
        if (a.F) {
            return f.n - n;
        }
        return n2;
    }

    private int Code(int n) {
        int n2;
        boolean bl = f.B();
        if (bl && a.Code(n, -1, false) != -1) {
            return n;
        }
        int n3 = -7;
        if (a.F) {
            n3 = -6;
        }
        int n4 = n3;
        if (C == 2 || bl) {
            n3 = -8;
        }
        String string = null;
        try {
            string = this.getKeyName(n);
        }
        catch (Throwable throwable) {}
        if (a.Z != null && (n2 = a.Z.indexOf((char)n)) >= 0) {
            return a.J.charAt(n2);
        }
        if (a.g == 3) {
            if (n == -14) {
                return -6;
            }
            if (n == -13) {
                return -7;
            }
            if (n == -20 || n == -22) {
                return 0;
            }
            if (n == -11) {
                if (f.Code() != null && f.Code().Code.equals(a.I(60))) {
                    return n;
                }
                return n4;
            }
        }
        if (string != null) {
            if ((string = string.toLowerCase()).equals("sk2(left)")) {
                return -6;
            }
            if (string.equals("sk1(right)")) {
                return -7;
            }
            if (a.V && n == -7) {
                return -6;
            }
            if (a.V && n == -6) {
                return -7;
            }
            if (n == 105) {
                return -6;
            }
            if (a.ap && n == 42 && string.equals("start")) {
                return 42;
            }
            if (n == -20 && string.equals("symbol")) {
                return -11;
            }
            if (string.indexOf("soft") >= 0) {
                if (string.charAt(string.length() - 1) == '1' || string.startsWith("left")) {
                    return -6;
                }
                if (string.charAt(string.length() - 1) == '2' || string.charAt(string.length() - 1) == '4' || string.startsWith("right")) {
                    return -7;
                }
            }
            if (string.equals("clear") || string.equals("backspace") || string.equals("back")) {
                return n3;
            }
            if (string.equals("select") || string.equals("ok") || string.equals("send") || string.equals("fire") || string.equals("navi-center") || string.equals("start") || string.equals("enter")) {
                return -5;
            }
            if (string.equals("up") || string.equals("navi-up") || string.equals("up arrow")) {
                return 1;
            }
            if (string.equals("down") || string.equals("navi-down") || string.equals("down arrow")) {
                return 6;
            }
            if (string.equals("left") || string.equals("navi-left") || string.equals("left arrow")) {
                return 2;
            }
            if (string.equals("right") || string.equals("navi-right") || string.equals("right arrow")) {
                return 5;
            }
            if (string.equals("sidedown") || string.equals("vol. down")) {
                return -10;
            }
            if (string.equals("sideup") || string.equals("vol. up")) {
                return -9;
            }
            if (a.ag) {
                if (this.h == 0 && (string.equals("0") || n == 32)) {
                    return 56;
                }
                if (n == -8 || n == 10) {
                    return -5;
                }
                if (n == 27) {
                    return n;
                }
                if (n == 8) {
                    return n3;
                }
            }
            if (string.equals("q") || string.equals("w")) {
                return -6;
            }
            if (string.equals("o") || string.equals("p")) {
                return -7;
            }
        }
        if (n == -11 && a.g == 0) {
            return 0;
        }
        if (n == -22 && a.g == 2) {
            return 0;
        }
        if (n == -6 || n == -21 || n == 21 || n == 65 || n == 66 || n == -202 || n == 113 || n == 57345) {
            return -6;
        }
        if (n == -7 || n == -22 || n == 22 || n == 67 || n == 68 || n == 106 || n == -203 || n == 112 || n == 57346) {
            return -7;
        }
        if (n == -5 || n == -10 || n == -20 || n == 20 || n == 23 || n == -14 || n == -26 || n == -200 || n == 13) {
            return -5;
        }
        if (n == -8 || n == -11 || n == -16 || n == -19 || n == -204) {
            return n3;
        }
        switch (n) {
            case 35: 
            case 42: 
            case 48: 
            case 49: 
            case 50: 
            case 51: 
            case 52: 
            case 53: 
            case 54: 
            case 55: 
            case 56: 
            case 57: {
                return n;
            }
        }
        try {
            int n5 = this.getGameAction(n);
            if (n5 != 0) {
                if (n5 == 8) {
                    return -5;
                }
                return n5;
            }
            return n;
        }
        catch (Throwable throwable) {
            return n;
        }
    }

    public static void Code(int n) {
        f.I(n, n - 1);
    }

    private static int Code(int n, int n2) {
        if (a.F) {
            return n2;
        }
        return n;
    }

    public final void B() {
        if (a.u) {
            this.repaint();
            return;
        }
        int n = m - i;
        int n2 = l;
        int n3 = i;
        if (a.N) {
            g.Code();
            return;
        }
        if (!this.e || c) {
            this.repaint();
            return;
        }
        int n4 = Math.max(0, 0);
        int n5 = Math.max(n, 0);
        if (a.F) {
            this.repaint(n4, 0, n + n3 - n5, n2 - n4);
            return;
        }
        this.repaint(n4, n5, n2 - n4, n + n3 - n5);
    }

    private void i() {
        this.Code((Graphics)null);
    }

    private void f() {
        if (f.J()) {
            if (!a.ba) {
                a.ba = true;
                a.aO = -1;
            }
            this.I(a.k);
            return;
        }
        this.I(a.I(65));
    }

    private void g() {
        a a2 = f.Code(1);
        if (a2 != null && a2.Code != null) {
            if (a2.Code.B) {
                if (a2.Code.a != l) {
                    a2.aR = true;
                    a2.c();
                    return;
                }
            } else if (a2.Code.a > l && a.aq) {
                this.I("overlay:" + a.I(126));
            }
        }
    }

    private int I(int n) {
        if (f.B() && a.Code(n, -1, false) != -1) {
            return n;
        }
        if (a.M && f.n > o) {
            if (n == -6) {
                return -7;
            }
            if (n == -7) {
                return -6;
            }
            if (this.Z == null) {
                switch (n) {
                    case 50: {
                        return 52;
                    }
                    case 56: {
                        return 54;
                    }
                    case 52: {
                        return 56;
                    }
                    case 54: {
                        return 50;
                    }
                    default: {
                        return n;
                    }
                }
            }
        } else if (a.F) {
            switch (n) {
                case 1: {
                    return 2;
                }
                case 6: {
                    return 5;
                }
                case 2: {
                    return 6;
                }
                case 5: {
                    return 1;
                }
            }
            if (this.Z == null) {
                switch (n) {
                    case 50: {
                        return 52;
                    }
                    case 56: {
                        return 54;
                    }
                    case 52: {
                        return 56;
                    }
                    case 54: {
                        return 50;
                    }
                }
            }
            if (!a.K || f.n <= 300) {
                if (n == -6) {
                    return -7;
                }
                if (n == -7) {
                    return -6;
                }
            }
        }
        return n;
    }

    private boolean C() {
        boolean bl = false;
        this.h = 0;
        if (this.Z != null) {
            this.Z.cancel();
            this.Z = null;
        }
        if (f.Code().I(8) && f.Code().Code.equals(a.I(76))) {
            this.I(f.Code());
            bl = true;
        }
        this.B();
        return bl;
    }

    private void Code(int n, int n2) {
        int n3;
        if (!this.e || c) {
            return;
        }
        this.i();
        this.b = false;
        if (J != null) {
            J.cancel();
            J = null;
        }
        if (a.g == 3 && n == -11) {
            this.commandAction(Code[4], (Displayable)this);
            return;
        }
        a a2 = f.Code();
        if (a2 == null) {
            return;
        }
        a2.ao = -1;
        if (a2.I(1) || a2.I(8) && ((n3 = n) == 1 || n3 == 6 || n3 == 2 || n3 == 5 ? !true : !false)) {
            n3 = this.h;
            if (a.Q && "szy".indexOf(n) != -1) {
                switch (n) {
                    case 115: {
                        n = 42;
                        break;
                    }
                    case 121: 
                    case 122: {
                        n = 35;
                    }
                }
            }
            if (n3 != 0) {
                if (a.aG && !c) {
                    String string = a.Code(n3, n, false);
                    this.C();
                    this.I(string);
                }
                return;
            }
            if (n == 35 || n == 42) {
                if (a.aG && !c && !this.Code((Graphics)null) && a2.I(1) && a.Code(f.Code()) != 73 && !f.Code().startsWith("sk:")) {
                    this.h = n;
                    this.B();
                    v0.Z = new g(4);
                    this.Code.schedule((TimerTask)this.Z, 1500);
                }
                return;
            }
        }
        if (n == -6) {
            if (a.ah) {
                return;
            }
            this.commandAction(Code[C], (Displayable)this);
        } else if (n == -7) {
            if (a.ah) {
                return;
            }
            this.commandAction(Code[a], (Displayable)this);
        } else if (n == -8 && a2.Code != null && a2.I() != -1) {
            n3 = a.Code(a2.Code.Code);
            int n4 = -1;
            switch (n3) {
                case 90: {
                    n4 = 99;
                    break;
                }
                case 91: {
                    n4 = 50;
                    break;
                }
                case 109: {
                    if (a2.I() > 0) {
                        n4 = 43;
                        break;
                    }
                    n4 = -1;
                    break;
                }
                case 136: {
                    n4 = 139;
                }
            }
            if (n4 != -1) {
                this.I("dialog:" + a.I(n4));
            }
        } else if (n == -5 && b > 0 && b != 23 && b != 29) {
            if (b == 28 && C > -1) {
                this.commandAction(Code[C], (Displayable)this);
                return;
            }
            this.commandAction(Code[b], (Displayable)this);
            return;
        }
        a2.Code(n, n2);
        if (a.N) {
            g.Code();
            return;
        }
        this.repaint();
    }

    private static boolean Code(int n) {
        if (f.B()) {
            return n != -5 && n != -6 && (n != -7 || a == 26);
        }
        int[] nArray = Z;
        for (int i = 0; i < nArray.length; ++i) {
            if (nArray[i] != n) continue;
            return true;
        }
        return false;
    }

    public final synchronized void Z() {
        long l = System.currentTimeMillis();
        if (this.g >= 0 && l >= this.Code) {
            this.Code = l + (long)40;
            this.f = true;
            f.b();
            return;
        }
        if (l >= this.Code + (long)20000) {
            this.I.cancel();
            this.I = null;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public final synchronized void keyPressed(int n) {
        block11: {
            a.a();
            a.I(n);
            if (a.aM && !f.Code(this.Code(n))) {
                return;
            }
            if (this.Code != null) {
                return;
            }
            if (a.W && n == -6 && J + (long)100 > System.currentTimeMillis()) {
                g = true;
                return;
            }
            if (a.g == -1 && n == this.r + 100) {
                this.getKeyName(n);
            }
            if (a.ai && n == 59440) {
                return;
            }
            this.r = n;
            if (n != -11 || a.g != 3) break block11;
            a.S = true;
        }
        try {
            n = this.I(this.Code(n));
            this.j();
            if (a.S) {
                if (!f.Code(n)) return;
            }
            this.Z(n);
            this.Code(n, 0);
            if (!f.Code(n)) return;
            f f2 = this;
            this.f = n;
            f2.g = 0;
            f2.Code = System.currentTimeMillis() + (long)a.l;
            if (a.P) return;
            if (f2.I != null) return;
            f2.I = new g(0);
            f2.Code.schedule((TimerTask)f2.I, 40, (long)40);
            return;
        }
        catch (Throwable throwable) {
            f.Code(2);
        }
    }

    public final boolean Code(a a2) {
        if (a.G >= 0) {
            return false;
        }
        boolean bl = false;
        a a3;
        while ((a3 = (a)Z.elementAt(Z.size() - 1)) != a2) {
            this.I(a3);
            bl = true;
        }
        return bl;
    }

    private synchronized void J(int n) {
        if (n == this.s && this.Code != null) {
            ((a)this.Code).d();
        }
    }

    public final synchronized void pointerReleased(int n, int n2) {
        a.a();
        a.I(n);
        a.I(n2);
        a.Code(1);
        if (a.aM) {
            return;
        }
        int n3 = f.Code(n, n2);
        int n4 = f.I(n, n2);
        this.pointerDragged(n, n2);
        Object object = this.Code;
        boolean bl = object instanceof a;
        if (bl) {
            bl = bl;
            object = object;
            if (((a)object).Code != 1) {
                bl = false;
            }
        }
        if (bl) {
            ((a)this.Code).Z(n3, n4);
        } else if (this.Code(n3, n4) == this.Code && this.Code instanceof Command) {
            this.commandAction((Command)this.Code, (Displayable)this);
            this.B();
        }
        this.Code = null;
    }

    public final synchronized void pointerPressed(int n, int n2) {
        this.j();
        this.b = true;
        ++this.s;
        a.a();
        a.I(n);
        a.I(n2);
        a.Code(1);
        if (a.aM) {
            return;
        }
        int n3 = f.Code(n, n2);
        int n4 = f.I(n, n2);
        this.Code = this.Code(n3, n4);
        if (this.Code instanceof Command) {
            if (a.Code(f.Code((Command)this.Code), "menu:")) {
                this.pointerReleased(n, n2);
                this.Code = f.Code();
            }
            this.B();
            return;
        }
        Object object = this.Code;
        boolean bl = object instanceof a;
        if (bl) {
            bl = bl;
            object = object;
            if (((a)object).Code != 1) {
                bl = false;
            }
        }
        if (bl) {
            if (!this.Code((a)this.Code) || ((a)this.Code).I(2)) {
                this.Code.schedule((TimerTask)new h(this), 1000);
                ((a)this.Code).I(n3, n4);
                return;
            }
            this.Code = null;
        }
    }

    public final synchronized void pointerDragged(int n, int n2) {
        a.a();
        a.I(n);
        a.I(n2);
        a.Code(1);
        if (a.aM) {
            return;
        }
        int n3 = f.Code(n, n2);
        n = f.I(n, n2);
        Object object = this.Code;
        boolean bl = object instanceof a;
        if (bl) {
            bl = bl;
            object = object;
            if (((a)object).Code != 1) {
                bl = false;
            }
        }
        if (bl) {
            Object object2;
            Object object3 = object2 = this.Code(n3, n);
            boolean bl2 = object2 instanceof a;
            if (bl2) {
                bl2 = bl2;
                object3 = object3;
                if (((a)object3).Code != 1) {
                    bl2 = false;
                }
            }
            if (bl2 && ((a)object2).I(2)) {
                this.Code = object2;
            }
            ((a)this.Code).J(n3, n);
            return;
        }
        if (this.Code(n3, n) != this.Code) {
            this.Code = null;
            this.B();
        }
    }

    public final synchronized void keyReleased(int n) {
        a.a();
        a.I(n);
        a.Code(8);
        if (a.aM && !f.Code(this.Code(n))) {
            return;
        }
        if (this.Code != null) {
            return;
        }
        if (a.W) {
            J = -1;
            if (n == -6) {
                if (g) {
                    g = false;
                    return;
                }
                J = System.currentTimeMillis();
            }
            g = false;
        }
        if (n != this.r) {
            return;
        }
        this.r = Integer.MAX_VALUE;
        this.j();
        n = this.I(this.Code(n));
        if (a.S && !f.Code(n)) {
            this.Z(n);
            this.Code(n, 0);
            if (n == -11 && a.g == 3) {
                a.S = false;
            }
        }
    }

    public static void Code(String string, String string2) {
        StringBuffer stringBuffer = new StringBuffer();
        a.Code(stringBuffer.append("a="), string);
        if (string2 != null) {
            a.Code(stringBuffer.append("&b="), string2);
        }
        f.Code().Code("overlay:" + a.I(70), null, stringBuffer.toString());
    }

    public final void Z(String string) {
        if (!(string = string.trim()).equals("www.") && string.length() > 0) {
            if (Code.contains(string)) {
                Code.removeElement(string);
            }
            if (Code.size() > 30) {
                Code.removeElement(Code.firstElement());
            }
            Code.addElement(string);
            this.I("ext:" + string);
        }
    }

    public static void Z(boolean bl) {
        if (I == null) {
            return;
        }
        if (bl && v > I.getMaxSize()) {
            Browser.Code.I("overlay:" + a.I(144));
            return;
        }
        Browser.Code.setCurrent((Displayable)I);
        a.Code(I);
        if (!a.E) {
            a.E = true;
            a.Code(false);
        }
    }

    public final void e() {
        this.C = f.Code(-1, true, null);
    }

    public final void Code(String string, String string2, int n, a a2) {
        a a3 = new a(n, a2);
        Z.addElement(a3);
        a3.Code(string, null, string2);
        a3.b();
        if (a.N) {
            g.Code();
            return;
        }
        this.repaint();
    }

    public final synchronized void keyRepeated(int n) {
        if (a.aM) {
            return;
        }
        if (this.Code != null) {
            return;
        }
        if (a.P && f.Code(this.I(this.Code(n)))) {
            this.f = true;
            f.b();
        }
    }

    public static int Code(int n, int n2, int n3, int n4) {
        if (!a.aw) {
            return n2;
        }
        n4 = Math.max(0, u - n4);
        if (n == n2 || n4 >= 160) {
            return n2;
        }
        i = true;
        j = true;
        return n3 + (int)((long)(n2 - n3) * (long)n4 * (long)(160 * (160 + n4) - n4 * n4) / 4096000L);
    }

    /*
     * Unable to fully structure code
     * Could not resolve type clashes
     */
    private boolean Code(Graphics var1_2) {
        block62: {
            block61: {
                block60: {
                    block59: {
                        block58: {
                            var2_3 = true;
                            var3_4 /* !! */  = 0;
                            var4_6 = 0;
                            var5_7 = new StringBuffer();
                            var6_10 = 0;
                            f.C = 23;
                            f.a = 23;
                            if (!a.aM) break block58;
                            var3_4 /* !! */  = 160;
                            var4_6 = a.T;
                            ** GOTO lbl65
                        }
                        if (a.G < 0) break block59;
                        var3_4 /* !! */  = a.F;
                        var4_6 = a.G;
                        var6_10 = 2;
                        ** GOTO lbl65
                    }
                    if (!a.X || Browser.Code.Code.Code < 0 && a.aS < 0) break block60;
                    var7_12 = a.aS > Browser.Code.Code.Code;
                    var3_4 /* !! */  = var7_12 != false ? a.aR : Browser.Code.Code.I;
                    var4_6 = var7_12 != false ? a.aS : Browser.Code.Code.Code;
                    f.a = 3;
                    var7_13 = var7_12 != false ? a.Code(172) : a.Code(173);
                    var5_7.append((String)var7_13).append(" ").append(var4_6 / 1000).append('/').append(var3_4 /* !! */  / 1000).append(" kB");
                    ** GOTO lbl65
                }
                if (f.Code(1) == null) {
                    return false;
                }
                var7_13 = f.Code(1).Code();
                if (var7_13 == null && f.Code(4) != null) {
                    var7_13 = f.Code(4).Code();
                }
                if (!(var7_13 != null && var7_13.d == false) || var7_13 == null) break block61;
                var3_5 = f.Code(8);
                if (var3_5 != null && var3_5.Code != null) {
                    f.C = var3_5.Code.Code;
                    f.a = var3_5.Code.I;
                } else {
                    f.a = var7_13.Z() != false && a.aC != false ? 38 : 3;
                }
                var3_4 /* !! */  = var7_13.Code();
                var4_6 = var7_13.I();
                switch (var7_13.b) {
                    case 1: {
                        var5_7.append(a.Code(20));
                        var6_10 = 1;
                        ** GOTO lbl65
                    }
                    case 2: {
                        var5_7.append(a.Code(22));
                        var6_10 = 1;
                        ** GOTO lbl65
                    }
                    case 3: 
                    case 4: {
                        var5_7.append(a.Code(23));
                        var5_7.append(' ').append(var7_13.Z() / 1000).append('/').append(var3_4 /* !! */  / 1000).append(" kB");
                        ** GOTO lbl65
                    }
                    default: {
                        if (!a.ah) break block62;
                    }
                }
            }
            var2_3 = false;
            f.Code().e();
lbl65:
            // 7 sources

            if (!a.ah) break block62;
            if (f.C != f.c || f.a != f.d || f.b != f.e) {
                var7_13 = this;
                if (!f.c) {
                    if (a.ah) {
                        for (var8_15 = 0; var8_15 < f.Code.length; ++var8_15) {
                            if (f.Code[var8_15] == null || var8_15 == 23 || var8_15 == 28) continue;
                            var7_13.removeCommand(f.Code[var8_15]);
                            if (f.C != var8_15 && f.a != var8_15 && f.b != var8_15) continue;
                            if (var8_15 == 1) {
                                if (a.ag) {
                                    f.a = true;
                                    continue;
                                }
                                if (var8_15 == f.b) continue;
                            } else if (!a.ag && var8_15 == f.b) continue;
                            var7_13.addCommand(f.Code[var8_15]);
                        }
                        f.c = f.C;
                        f.d = f.a;
                        f.e = f.b;
                    }
                    if (a.ag) {
                        var7_13.addCommand(f.Code[14]);
                        var7_13.I(f.a);
                    }
                }
            }
            if (!var2_3) {
                return false;
            }
        }
        if (var1_2 == null) {
            return var2_3;
        }
        var7_14 = 0;
        var8_15 = 0;
        var9_16 = f.m - f.i;
        var10_18 = f.l;
        var11_19 = f.i;
        if (!a.ah) {
            var1_2.setColor(f.Code[26]);
            var1_2.drawLine(0, var9_16, var10_18 - 1, var9_16);
            var7_14 = Math.max(0, var11_19 - f.j);
            if (var7_14 > 0) {
                a.Code((Graphics)var1_2, 0, var9_16 + 1, f.l, var7_14, this.I[0]);
            }
            ++var7_14;
            for (var8_15 = 0; var8_15 < f.j; ++var8_15) {
                var1_2.setColor(this.I[var8_15]);
                var1_2.drawLine(0, var9_16 + var8_15 + var7_14, var10_18 - 1, var9_16 + var8_15 + var7_14);
            }
            var11_20 = f.Code[f.C].getLabel();
            if (f.C == 21 && f.a()) {
                var11_20 = "";
            }
            var12_21 = f.Code[f.a].getLabel();
            var13_23 = f.Code[f.b].getLabel();
            if (a.H) {
                var11_20 = "";
            }
            var7_14 = a.Code(0, var11_20) + (f.C == 23 || a.H != false ? 0 : 4);
            v0 = f.a - 23;
            var8_15 = a.Code(0, var12_21) + ((v0 | -v0) >>> 31 << 2);
            v1 = f.b - 23;
            var14_24 = a.Code(1, var13_23) + ((v1 | -v1) >>> 31 << 2);
            if (var7_14 >= var10_18 - var14_24 >> 1 || var8_15 >= var10_18 - var14_24 >> 1) {
                var11_20 = a.Code(0, var11_20, (var10_18 - var14_24 >> 1) - 1, ".");
                var7_14 = a.Code(0, var11_20);
                var12_21 = a.Code(0, var12_21, (var10_18 - var14_24 >> 1) - 1, ".");
                var8_15 = a.Code(0, var12_21);
            }
            v2 = var15_25 = f.a == 12 || f.a == 38 || f.a == 3 && f.Code().Code() != null && f.Code().Code().Z() != false && a.aC != false;
            if (this.C != null) {
                v3 = var15_25;
                var15_25 = this.C.getWidth() + 1 + var8_15 < (var10_18 - var14_24 >> 1) - 1 ? v3 & true : v3 & false;
            }
            if (var15_25) {
                var8_15 += this.C.getWidth();
            }
            var1_2.setColor(f.Code[1]);
            if (this.Code == f.Code[f.C] && f.Code[f.C] != f.Code[23]) {
                a.I((Graphics)var1_2, 0, var9_16, var7_14, f.i - 1);
                var1_2.setColor(f.Code[2]);
            }
            f.Code((Graphics)var1_2, 0, var11_20, 2, 20);
            var1_2.setColor(f.Code[1]);
            if (this.Code == f.Code[f.a] && f.Code[f.a] != f.Code[23]) {
                a.I((Graphics)var1_2, var10_18 - var8_15, var9_16, var8_15, f.i - 1);
                var1_2.setColor(f.Code[2]);
                if (var15_25) {
                    var1_2.drawImage(this.C, var10_18 - var8_15 + 1, f.m - this.C.getHeight() - this.q, 20);
                }
            } else if (var15_25) {
                var1_2.drawImage(this.a, var10_18 - var8_15 + 1, f.m - this.C.getHeight() - this.q, 20);
            }
            f.Code((Graphics)var1_2, 0, var12_21, var10_18 - 2, 24);
            if (this.Code == f.Code[f.b] && f.Code[f.b] != f.Code[23]) {
                a.I((Graphics)var1_2, (var10_18 >> 1) - (var14_24 >> 1), var9_16, var14_24, f.i - 1);
                var1_2.setColor(f.Code[2]);
            } else {
                var1_2.setColor(f.Code[1]);
            }
            if (!a.H) {
                f.Code((Graphics)var1_2, 1, var13_23, var10_18 >> 1, 17);
            }
        }
        if (var2_3) {
            v4 = var1_2;
            v5 = var7_14;
            var0_1 = var6_10;
            var1_2 = var5_7;
            var5_8 = f.i;
            var6_10 = f.l - var8_15 - var7_14;
            var7_14 = f.m - f.i;
            var8_15 = v5;
            var9_17 = v4;
            var4_6 = a.Code((int)var4_6, 0, var3_4 /* !! */ );
            var9_17.setColor(f.Code[26]);
            var9_17.drawRect(var8_15, var7_14, var6_10 - 1, var5_8);
            var10_18 = var3_4 /* !! */  != 0 ? (var6_10 -= 2) * var4_6 / var3_4 /* !! */  : 0;
            a.Code(var9_17, ++var8_15, ++var7_14, var10_18, var5_8, f.Code[38], f.Code[39]);
            var11_19 = var6_10 - var10_18;
            a.Code(var9_17, var8_15 + var10_18, var7_14, var11_19, var5_8, f.Code[29], f.Code[30]);
            if (var11_19 >= 1) {
                var9_17.setColor(f.Code[32]);
                var9_17.drawLine(var8_15 + var10_18, var7_14, var8_15 + var10_18, var7_14 + var5_8 - 2);
            }
            if (var11_19 >= 2) {
                var9_17.setColor(f.Code[33]);
                var9_17.drawLine(var8_15 + 1 + var10_18, var7_14, var8_15 + 1 + var10_18, var7_14 + var5_8 - 2);
                var9_17.drawLine(var8_15 + 1 + var10_18, var7_14, var6_10, var7_14);
            }
            if (var11_19 >= 3) {
                var9_17.setColor(f.Code[34]);
                var9_17.drawLine(var8_15 + 2 + var10_18, var7_14 + 1, var8_15 + 2 + var10_18, var7_14 + var5_8 - 2);
                var9_17.drawLine(var8_15 + 2 + var10_18, var7_14 + 1, var6_10, var7_14 + 1);
            }
            var9_17.setColor(f.Code[28]);
            if (var0_1 == 2) {
                var1_2.append(' ').append(var4_6 * 100 / Math.max(var3_4 /* !! */ , 1)).append('%');
            }
            var3_4 /* !! */  = var1_2.length();
            var4_6 = (char)a.Code(0, var1_2.toString());
            if (var0_1 == 1) {
                var5_8 = (int)(System.currentTimeMillis() - f.Z >> 9) & 3;
                var1_2.append("...".substring(0, var5_8));
            }
            if ((var6_10 - var4_6 >> 1) + (var7_14 = a.Code(0, var5_9 = var1_2.toString())) > var6_10) {
                var1_2.delete(0, var3_4 /* !! */ );
                if (var0_1 != 1) {
                    var1_2.insert(0, "...");
                }
                var5_9 = a.Code(0, var5_9, var6_10, var1_2.toString());
                var4_6 = (char)a.Code(0, var5_9);
            }
            f.Code(var9_17, 0, var5_9, var8_15 + (var6_10 >> 1) - (var4_6 >> 1), 20);
        } else if (f.b == 23 && var7_14 + ((var3_4 /* !! */  = a.Code(0, this.C)) >> 1) + 1 < var10_18 >> 1 && var8_15 + (var3_4 /* !! */  >> 1) + 1 < var10_18 >> 1) {
            var3_4 /* !! */  = this.h;
            var4_6 = 32;
            if (var3_4 /* !! */  == 0 && (var5_7 = f.Code().Code) != null && var5_7.Code() && (var5_7 = f.Code().Code.I()) != null) {
                var6_11 = new char[2];
                if (a.Code((String)var5_7, "speeddial:")) {
                    var5_7 = var5_7.substring(12);
                }
                block7: for (var7_14 = 0; var7_14 < 2; ++var7_14) {
                    var8_15 = "#*".charAt(var7_14);
                    for (var11_19 = 0; var11_19 < 12; ++var11_19) {
                        var12_22 = "1234567890#*".charAt(var11_19);
                        var13_23 = a.Code(var8_15, (int)var12_22, false);
                        if (var13_23 == null || !var13_23.equals(var5_7) && !var13_23.equals("ext:" + (String)var5_7) && !var13_23.equals("nint:" + (String)var5_7) && (!var13_23.startsWith("feed:start") || !var5_7.startsWith("feed:start"))) continue;
                        var6_11[0] = var8_15;
                        var6_11[1] = var12_22;
                        break block7;
                    }
                }
                var5_7 = var6_11;
                if (!f.Code().I(2) || f.d) {
                    var3_4 /* !! */  = (int)var5_7[0];
                    var4_6 = (char)var5_7[1];
                } else if (var5_7[0] != false && f.J == null) {
                    f.J = new g(24);
                    this.Code.schedule((TimerTask)f.J, 1500);
                }
            }
            if (var3_4 /* !! */  != 0) {
                var1_2.drawImage(this.Z, (var10_18 >> 1) - 1, var9_16 + 1, 24);
                var1_2.drawImage(this.Z, var10_18 >> 1, var9_16 + 1, 20);
                var1_2.setColor(f.Code[31]);
                f.Code((Graphics)var1_2, 1, String.valueOf((char)var3_4 /* !! */ ), (var10_18 - this.Z.getWidth() >> 1) - 1, 17);
                f.Code((Graphics)var1_2, 1, String.valueOf(var4_6), var10_18 + this.Z.getWidth() >> 1, 17);
            } else {
                var1_2.setColor(f.Code[1]);
                f.Code((Graphics)var1_2, 0, this.C, var10_18 >> 1, 17);
            }
        }
        return var2_3;
    }

    private Object Code(int n, int n2) {
        int n3 = Z.size();
        while (n3-- > 0) {
            a a2 = (a)Z.elementAt(n3);
            if (!a2.Code(n, n2)) continue;
            return a2;
        }
        this.i();
        if (this.Z() && (n2 > m - i || a.H && n2 < i)) {
            int n4;
            n3 = a.Code(1, Code[C].getLabel()) + 4;
            int n5 = n4 = a == 12 ? l - a.Code(1, Code[a].getLabel()) - this.C.getWidth() - 6 : l - a.Code(1, Code[a].getLabel()) - 4;
            if (n3 < l / 3) {
                n3 = l / 3;
            }
            if (n4 > l / 3 << 1) {
                n4 = l / 3 << 1;
            }
            if (a.H) {
                if (n > n4 && n2 > m - i) {
                    return Code[a];
                }
                if (n > l - n3 && n2 < i) {
                    return Code[C];
                }
            } else if (n2 > m - i) {
                if (n > n4) {
                    return Code[a];
                }
                if (n >= n3 && n <= n4) {
                    return Code[b];
                }
                if (n < n3) {
                    return Code[C];
                }
            }
        }
        return null;
    }

    public static a Code() {
        if (Z.isEmpty()) {
            return null;
        }
        return (a)Z.lastElement();
    }

    public static a Code(int n) {
        int n2 = Z.size();
        while (n2-- > 0) {
            a a2 = (a)Z.elementAt(n2);
            if (!a2.I(n)) continue;
            return a2;
        }
        return null;
    }

    public static a Code(String string) {
        int n = Z.size();
        while (n-- > 0) {
            a a2 = (a)Z.elementAt(n);
            if (a2.Code == null || !string.equals(a2.Code.Code)) continue;
            return a2;
        }
        return null;
    }

    private static void k() {
        for (int i = 0; i < Z.size(); ++i) {
            ((a)Z.elementAt(i)).b();
        }
    }

    public static void c() {
        for (int i = 0; i < I.size(); ++i) {
            ((Object[])f.I.elementAt((int)i))[8] = null;
        }
        a a2 = (a)Z.firstElement();
        for (int i = 0; i < a2.B.size(); ++i) {
            ((Object[])a2.B.elementAt((int)i))[8] = null;
        }
    }

    public final boolean Code() {
        return this.b || f.Code().Code == null;
    }

    public final boolean I() {
        return !a.D || a.H && this.Z();
    }

    public final boolean Z() {
        return (!a.D || f.B() || Z.size() > 0 && (f.Code().Code > 0 || f.Code().I > 0)) && !a.ah || this.Code((Graphics)null);
    }

    public final void J() {
        a.D ^= true;
        f.k();
        this.repaint();
    }

    public final synchronized void sizeChanged(int n, int n2) {
        if (f.n == 0 || o == 0 || n == f.n && n2 == o) {
            return;
        }
        n = this.getWidth();
        n2 = this.getHeight();
        if (a.F) {
            l = n2;
            m = n;
        } else {
            m = n2;
            l = n;
        }
        f.n = n;
        o = n2;
        this.c = null;
        this.b = null;
        this.Code(a.F);
        for (n = 0; n < Z.size(); ++n) {
            a a2 = (a)Z.elementAt(n);
            if (a2.Code == null) continue;
            a2.Code.B();
        }
        f.k();
        if (a.ao) {
            this.g();
        }
        if (i != null && (i.getWidth() != l || i.getHeight() != m)) {
            n = i.getWidth() * i.getHeight() << 1;
            if (Browser.Code.numColors() > 65536) {
                n <<= 1;
            }
            a.I -= (long)n;
            Code = null;
            i = null;
        }
        this.repaint();
    }

    public final synchronized void showNotify() {
        this.j();
        this.repaint();
    }

    public final synchronized void hideNotify() {
        this.j();
    }

    /*
     * Unable to fully structure code
     */
    public final synchronized void paint(Graphics var1_1) {
        if (!this.e || f.c) {
            var2_2 = this;
            a.Code(var1_1, var1_1.getClipX(), var1_1.getClipY(), var1_1.getClipWidth(), var1_1.getClipHeight(), 0xFFFFFF);
            if (var2_2.Code == null) {
                var2_2.Code = a.Code(69);
            }
            if (var2_2.Code != null) {
                var1_1.drawImage(var2_2.Code, var2_2.getWidth() >> 1, var2_2.getHeight() >> 1, 3);
            }
            var3_4 = 1280;
            var4_6 = 256 * (1 + var2_2.Code);
            if (f.J()) {
                var5_8 = new StringBuffer(a.Code(113));
                var3_4 = 1280 + 512 * a.Z.length;
                if (a.aE) {
                    var4_6 += 512 * a.Z.length;
                } else {
                    var4_6 += 512 * Math.max(0, a.aO);
                    if (a.aO != f.p || a.aO == -1) {
                        f.I = System.currentTimeMillis();
                        f.p = a.aO;
                    } else {
                        var4_6 += 512 * a.Code((int)(System.currentTimeMillis() - f.I), 0, 30000) / 30000;
                    }
                }
            } else {
                var5_8 = new StringBuffer(a.Code(134));
            }
            var6_10 = var2_2.getHeight() - a.Code[0].getHeight() - 15;
            var1_1.setColor(11277844);
            var1_1.fillRect(var2_2.getWidth() / 10, var6_10, (var2_2.getWidth() << 3) * var4_6 / (10 * var3_4), 10);
            var1_1.setColor(0);
            var1_1.drawRect(var2_2.getWidth() / 10, var6_10, (var2_2.getWidth() << 3) / 10, 10);
            var1_1.setColor(0);
            var1_1.setFont(a.Code[0]);
            var1_1.drawString(var5_8.toString(), var2_2.getWidth() >> 1, var2_2.getHeight() - 2, 33);
            return;
        }
        this.Code = null;
        if (!a.A && !a.F) ** GOTO lbl53
        if (f.i != null) ** GOTO lbl50
        var2_3 = f.n * f.o << 1;
        try {
            if (Browser.Code.numColors() > 65536) {
                var2_3 <<= 1;
            }
            if (a.F) {
                f.i = Image.createImage((int)(a.G != false ? Math.min(f.o, f.n) : f.o), (int)f.n);
                f.Code = new Sprite(f.i);
                f.Code.setTransform(5);
                f.Code.setPosition(0, 0);
            } else {
                f.i = Image.createImage((int)f.n, (int)f.o);
            }
            f.Code = f.i.getGraphics();
            a.I += (long)var2_3;
lbl50:
            // 2 sources

            f.I = var1_1;
            var1_1 = f.Code;
            var1_1.setClip(0, 0, f.i.getWidth(), f.i.getHeight());
lbl53:
            // 2 sources

            f.j = false;
            if (this.f && this.g >= 0) {
                this.Code(this.f, ++this.g);
            }
            this.f = false;
            f.u = (int)System.currentTimeMillis();
            for (var2_3 = 0; var2_3 < f.l; var2_3 += var3_5) {
                if (a.F) {
                    var3_5 = var2_3 == 0 && f.l != f.i.getWidth() ? f.l % f.i.getWidth() : f.i.getWidth();
                    var1_1.translate(-var2_3 - var1_1.getTranslateX(), -var1_1.getTranslateY());
                    var1_1.setClip(-var1_1.getTranslateX(), 0, var3_5, f.i.getHeight());
                } else {
                    var3_5 = f.l;
                }
                if (this.h != this.Z()) {
                    this.h ^= true;
                    f.k();
                }
                var4_7 = -1;
                var5_9 = 0;
                var6_11 = null;
                for (var7_12 = f.Z.size() - 1; var7_12 >= 0; --var7_12) {
                    var8_14 = (a)f.Z.elementAt(var7_12);
                    if (var4_7 < 0 && var8_14.I(21)) {
                        var4_7 = var7_12;
                    }
                    var8_14.Z(this.g >= 0);
                    if (var6_11 != null && var8_14.Code(var6_11) && !var6_11.Code()) continue;
                    var5_9 = var7_12;
                    var6_11 = var8_14;
                }
                while (var5_9 < f.Z.size()) {
                    var6_11 = (a)f.Z.elementAt(var5_9);
                    var6_11.I(var1_1);
                    if (var4_7 == var5_9) {
                        if (this.h) {
                            if (var1_1.getClipY() + var1_1.getClipHeight() > f.m - f.i) {
                                this.Code(var1_1);
                            }
                            if (!var6_11.I() && var1_1.getClipY() + var1_1.getClipHeight() > f.m - f.i - 1 && a.i) {
                                if (this.c == null) {
                                    this.h();
                                }
                                var1_1.drawImage(this.c, 0, f.m - f.i - 1, 20);
                            }
                        }
                        if (this.I()) {
                            if (var1_1.getClipY() < f.j) {
                                var6_11 = var1_1;
                                var7_13 = this;
                                var8_14 = null;
                                var9_15 = null;
                                var10_17 = 0;
                                var11_18 = 2;
                                var12_19 = 0;
                                var13_20 = f.Code((int)5).Code;
                                var14_21 = f.l;
                                for (var15_22 = 0; var15_22 < f.j; ++var15_22) {
                                    var6_11.setColor(var7_13.I[var15_22]);
                                    var6_11.drawLine(0, var15_22, var14_21, var15_22);
                                }
                                if (a.H && !a.ah) {
                                    var15_23 = f.Code[f.C].getLabel();
                                    if (f.C == 21 && f.a()) {
                                        var15_23 = "";
                                    }
                                    var6_11.setColor(f.Code[1]);
                                    a.Code((Graphics)var6_11, 1, var15_23, var14_21 - 2, 1, 24);
                                    var14_21 -= a.Code(1, var15_23) + 4;
                                }
                                if (var13_20 != null) {
                                    var9_15 = var13_20.J;
                                    var8_14 = var13_20.Code();
                                    var12_19 = var13_20.c;
                                    if (var8_14 != null) {
                                        var11_18 = 2 + (4 + f.k);
                                    }
                                }
                                if (!a.F && a.g == 0) {
                                    switch (var7_13.getHeight()) {
                                        case 160: {
                                            var10_17 = 13;
                                            break;
                                        }
                                        case 208: {
                                            var10_17 = 17;
                                            break;
                                        }
                                        case 320: {
                                            var10_17 = 22;
                                            break;
                                        }
                                        case 416: {
                                            var10_17 = 28;
                                        }
                                    }
                                }
                                var11_18 += var10_17;
                                if (var7_13.I != null) {
                                    var15_22 = var6_11.getClipX();
                                    var16_24 = var6_11.getClipY();
                                    var17_25 = var6_11.getClipWidth();
                                    a.Code((Graphics)var6_11, var15_22, var16_24, var17_25, f.j);
                                    var6_11.drawImage(var7_13.I, var14_21, 0, 24);
                                    var14_21 -= var7_13.I.getWidth();
                                    if (var12_19 > 1) {
                                        var6_11.drawImage(var7_13.J, var14_21, f.j - var7_13.J.getHeight() >> 1, 24);
                                        if (var12_19 < 3) {
                                            var6_11.setColor(0);
                                            var6_11.drawLine(var14_21 - 1, 0, var14_21 - var7_13.J.getWidth(), var7_13.J.getHeight() - 2);
                                            var6_11.drawLine(var14_21 + 1, 0, var14_21 - var7_13.J.getWidth() + 2, var7_13.J.getHeight() - 2);
                                            var6_11.setColor(0xFFFF00);
                                            var6_11.drawLine(var14_21, 0, var14_21 - var7_13.J.getWidth() + 1, var7_13.J.getHeight() - 2);
                                        }
                                        var14_21 -= var7_13.J.getWidth();
                                    }
                                    a.Code((Graphics)var6_11);
                                }
                                if (f.B || f.J) {
                                    var6_11.drawImage(var7_13.B, var14_21, f.j - var7_13.B.getHeight() >> 1, 24);
                                    var14_21 -= var7_13.B.getWidth();
                                }
                                if (var9_15 != null) {
                                    var7_13 = a.Code(1, var9_15, var14_21 - var11_18, null);
                                    var6_11.setColor(f.Code[0]);
                                    a.Code((Graphics)var6_11, 1, (String)var7_13, var11_18, 1, 20);
                                    if (a.Code(var13_20.Code) != -1 && var7_13.equals(a.f) && (var9_16 = var7_13.indexOf(32)) != -1) {
                                        var6_11.setColor(f.Code[37]);
                                        a.Code((Graphics)var6_11, 1, var7_13.substring(0, var9_16), var11_18, 1, 20);
                                    }
                                }
                                if (var8_14 != null) {
                                    var6_11.drawImage((Image)var8_14, var10_17 + 2, f.j - f.k - 1 >> 1, 20);
                                }
                            }
                            if (var1_1.getClipY() < f.j + 4 && a.i) {
                                if (this.b == null) {
                                    this.h();
                                }
                                var1_1.drawImage(this.b, 0, f.j, 20);
                            }
                        }
                    }
                    ++var5_9;
                }
                if (!this.Code()) {
                    f.Code().Z(var1_1);
                }
                if (!a.A && !a.F) continue;
                if (a.F) {
                    f.I.setClip(0, 0, f.n, f.o);
                    f.Code.setPosition(0, var2_3);
                    f.Code.paint(f.I);
                    continue;
                }
                f.I.drawImage(f.i, 0, 0, 20);
            }
            if (f.j) {
                if (a.N) {
                    g.Code();
                    return;
                }
                this.repaint();
                return;
            }
            if (f.i) {
                f.i = false;
                g.I = false;
                g.Code(f.Code());
                return;
            }
        }
        catch (Throwable v0) {
            f.Code(2);
        }
    }

    private static TextBox Code() {
        try {
            return new TextBox(a.Code(39), "www.", 400, 524292);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return new TextBox(a.Code(39), "www.", 200, 0);
        }
    }

    public static void Code(String string, String string2, int n, boolean bl) {
        int n2 = string2.length();
        Code code = f.Code();
        if (code != null) {
            code.f();
        }
        int n3 = Math.max(5524, string2.length());
        if (a.j > 0) {
            n3 = a.j;
        }
        while (n3 > 255) {
            block8: {
                TextBox textBox;
                if (string2.length() > n3) {
                    string2 = string2.substring(0, n3);
                }
                try {
                    textBox = new TextBox("", string2, n3, n);
                }
                catch (Throwable throwable) {
                    break block8;
                }
                if (string == null) {
                    string = a.Code(40);
                }
                textBox.setTitle(string + " (" + textBox.getMaxSize() + ")");
                textBox.addCommand(Code[9]);
                textBox.addCommand(Code[10]);
                if (bl) {
                    textBox.addCommand(Code[19]);
                }
                f.Code((Screen)textBox);
                Browser.Code.C = a.m;
                textBox.setCommandListener((CommandListener)Browser.Code);
                I = textBox;
                v = n2;
                f.Z(true);
                return;
            }
            n3 -= n3 / 10;
        }
    }

    public static int Code(f f2) {
        return f2.s;
    }

    public final void I(String string, String string2) {
        if (string.startsWith("http://")) {
            string2 = a.Code(string2, true);
        }
        this.I("ext:" + a.Code(string, "%s", string2));
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static void d() {
        try {
            for (int i = 0; i < I.size(); ++i) {
                Code code = (Code)((Object[])I.elementAt(i))[8];
                if (code == null) continue;
                code.Code = null;
            }
            a a2 = (a)Z.firstElement();
            if (a2 != null && a2.B != null) {
                for (int i = 0; i < a2.B.size(); ++i) {
                    Code code = (Code)((Object[])a2.B.elementAt(i))[8];
                    if (code == null) continue;
                    code.Code = null;
                }
            }
            return;
        }
        catch (Throwable throwable) {
            return;
        }
    }

    public static String Code(int n, boolean bl, Calendar calendar) {
        Calendar calendar2;
        Calendar calendar3 = calendar2 = calendar != null ? calendar : a.Code(false, bl);
        if (n < 0 || n > 23) {
            n = calendar2.get(11);
        }
        int n2 = calendar2.get(12);
        if (a.aB) {
            return (n < 10 ? "0" : "") + n + ":" + (n2 < 10 ? "0" : "") + n2;
        }
        boolean bl2 = true;
        if (n >= 12) {
            bl2 = false;
            n -= 12;
        }
        return (n == 0 ? 12 : n) + ":" + (n2 < 10 ? "0" : "") + n2 + (bl2 ? " AM" : " PM");
    }

    static {
        Z = "";
        J = "";
        B = "0";
        I = 1;
        B = 3;
        Code = new Command[39];
        Z = new int[]{1, 6, 2, 5, 50, 52, 54, 56};
        J = new int[]{54, 54, 55, 55, 52, 55, 50, 55, 50, 52, 51};
        J = -1;
        Z = new Vector();
        c = true;
        p = -1;
        Code = new Vector();
    }
}

