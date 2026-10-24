/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.lcdui.Display
 *  javax.microedition.midlet.MIDlet
 */
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/*
 * Duplicate member names - consider using --renamedupmembers true
 */
public class Browser
extends MIDlet {
    private boolean I;
    public static Browser Code;
    private static boolean Z;
    public static boolean Code;
    public static f Code;
    public static Display Code;

    public static void Code() {
        Code.destroyApp(false);
        Code.notifyDestroyed();
    }

    public void pauseApp() {
        this.I = true;
    }

    public void startApp() {
        Code = this;
        Code = Display.getDisplay((MIDlet)this);
        if (this.I) {
            this.I = false;
            return;
        }
        if (Z) {
            return;
        }
        Z = true;
        new f();
    }

    public void destroyApp(boolean bl) {
        Code = true;
        if (Code != null) {
            if (a.ba) {
                a.aO = -1;
            }
            a.Code(true);
            a.Code("h1", f.I);
            if (!a.ai) {
                Browser.Code.Code.cancel();
            }
        }
        Z = false;
    }
}

