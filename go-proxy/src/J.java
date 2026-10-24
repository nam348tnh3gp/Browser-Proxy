/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.io.Connector
 *  javax.microedition.io.ContentConnection
 *  javax.microedition.io.InputConnection
 *  javax.microedition.io.file.FileConnection
 *  javax.microedition.io.file.FileSystemRegistry
 *  javax.microedition.lcdui.Image
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.ContentConnection;
import javax.microedition.io.InputConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Image;

/*
 * Duplicate member names - consider using --renamedupmembers true
 */
public final class J {
    public static ContentConnection Code;
    public static String Code;
    public static String I;
    public static String Z;
    public static String J;
    public int I;
    public int Code;
    public boolean Code;
    public static boolean I;
    private Image Code = -1;
    public static Vector Code;

    /*
     * Loose catch block
     */
    public static void Z(String string) {
        String string2;
        String string3;
        FileConnection fileConnection = null;
        DataInputStream dataInputStream = null;
        try {
            fileConnection = (FileConnection)Connector.open((String)("file:///" + string + "/"), (int)1);
            Enumeration enumeration = fileConnection.list("*.obml", false);
            fileConnection.close();
            a.J.removeAllElements();
            while (enumeration.hasMoreElements()) {
                string3 = (String)enumeration.nextElement();
                string2 = string + "/" + string3;
            }
        }
        catch (Throwable throwable) {
            a.Code(fileConnection);
            return;
        }
        {
            fileConnection = (FileConnection)Connector.open((String)("file:///" + string2), (int)1);
            dataInputStream = fileConnection.openDataInputStream();
            a.Code((InputStream)dataInputStream, (long)14);
            string2 = dataInputStream.readUTF();
            short s = dataInputStream.readShort();
            byte[] byArray = new byte[s];
            dataInputStream.readFully(byArray, 0, s);
            a.J.addElement(new Object[]{string + "/" + string3, string2 != null ? string2 : "", byArray});
            {
                catch (Throwable throwable) {
                    a.Code(fileConnection);
                    a.Code(dataInputStream);
                    continue;
                }
            }
            a.Code(fileConnection);
            a.Code((Object)dataInputStream);
            continue;
        }
        a.Code(fileConnection);
    }

    /*
     * Loose catch block
     */
    public static int Code(Code objectArray, String string, int n, String string2) {
        FileConnection fileConnection = null;
        DataOutputStream dataOutputStream = null;
        if (objectArray != null && objectArray.I != null && string != null) {
            int n2 = 0;
            while (true) {
                ++n2;
                int n3 = objectArray.Code.hashCode() ^ new Long(System.currentTimeMillis()).hashCode();
                String string3 = Integer.toHexString(n3) + ".obml";
                String string4 = "file:///" + string + "/" + string3;
                fileConnection = (FileConnection)Connector.open((String)string4, (int)2);
                try {
                    fileConnection.create();
                    continue;
                }
                catch (IOException iOException) {
                    fileConnection.close();
                    string4 = null;
                    if (string4 == null && n2 < 5) continue;
                    dataOutputStream = fileConnection.openDataOutputStream();
                    dataOutputStream.writeByte(objectArray.I.length >> 16);
                    dataOutputStream.writeShort(objectArray.I.length);
                    if (a.g == 6) {
                        int n4;
                        for (n2 = 0; n2 < objectArray.I.length; n2 += n4) {
                            n4 = objectArray.I.length - n2;
                            n4 = n4 >= 16384 ? 16384 : n4;
                            dataOutputStream.write(objectArray.I, n2, n4);
                        }
                    } else {
                        ((OutputStream)dataOutputStream).write(objectArray.I);
                    }
                    dataOutputStream.flush();
                    objectArray = new Object[]{string + "/" + string3, string2 != null ? string2 : (objectArray.J != null ? objectArray.J : ""), objectArray.Code != null ? objectArray.Code : new byte[]{}};
                    if (n == -1) {
                        a.J.addElement(objectArray);
                        break;
                    }
                    a.J.insertElementAt(objectArray, n);
                }
                break;
            }
        }
        Code = null;
        a.Code(dataOutputStream);
        a.Code(fileConnection);
        a.Code(false);
        return 0;
        {
            catch (Throwable throwable) {
                try {
                    a.C = "";
                    return 1;
                }
                finally {
                    a.Code(dataOutputStream);
                    a.Code(fileConnection);
                    a.Code(false);
                }
            }
        }
    }

    public static boolean Code() {
        return Code == null && Z == null;
    }

    public static boolean Code(boolean bl) {
        String string = null;
        if (!bl && a.C != null && a.C.length() > 0) {
            return true;
        }
        if (bl && Code != null && Code.length() > 0) {
            string = Code;
        }
        if (string != null) {
            if (string.startsWith("file:///")) {
                string = string.substring(8);
            }
            if (string.endsWith("/")) {
                string = string.substring(0, string.length() - 1);
            }
            a.C = string;
            a.Code(false);
        }
        return a.C.length() > 0;
    }

    private static void Code(Throwable throwable) {
        a a2 = f.Code(8);
        if (a2 != null) {
            Browser.Code.I(a2);
        }
        f.Code(throwable.getMessage());
        Code = null;
    }

    /*
     * Loose catch block
     */
    public static void I(String string) {
        FileConnection fileConnection;
        block3: {
            fileConnection = null;
            if (string != null) {
                if (string.length() <= 0) break block3;
                fileConnection = (FileConnection)Connector.open((String)("file:///" + string), (int)2);
                fileConnection.delete();
            }
        }
        a.Code(fileConnection);
        return;
        catch (Throwable throwable) {
            a.Code(fileConnection);
            return;
        }
    }

    public static String Code(boolean bl) {
        String string;
        if (Code == null || J == null) {
            return null;
        }
        String string2 = string = bl ? ".name" : "";
        if (J.startsWith("image")) {
            string = "fileconn.dir.photos" + string;
        } else if (J.startsWith("video")) {
            string = "fileconn.dir.videos" + string;
        } else if (J.startsWith("audio")) {
            string = "fileconn.dir.music" + string;
        }
        if (string.length() == 0) {
            return null;
        }
        string = System.getProperty(string);
        if (bl && (string == null || string.equals("null")) && Z != null && (string = Z.substring(Z.lastIndexOf(47, Z.length() - 2))).endsWith("/")) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    public static void Code() {
        Z = null;
        a.Code(Code);
        Code = null;
        J = null;
    }

    /*
     * Unable to fully structure code
     */
    public final Vector Code(boolean var1_1) {
        J.Code = new Vector<E>();
        J.I = false;
        if (J.Z == null) ** GOTO lbl7
        try {
            block21: {
                J.Code.addElement(new Object[]{"/", new Integer(5), a.Code(162), null});
                break block21;
lbl7:
                // 1 sources

                if (J.Code == null) {
                    var2_2 = new String[]{"photos", "graphics", "videos", "music", "tones"};
                    for (var3_4 = 0; var3_4 < ((String[])var2_2).length; ++var3_4) {
                        var4_6 = System.getProperty("fileconn.dir." + var2_2[var3_4]);
                        if (var4_6 == null || !var4_6.startsWith("file:///")) continue;
                        var5_9 = System.getProperty("fileconn.dir." + (String)var2_2[var3_4] + ".name");
                        if (var5_9 == null || var5_9.length() == 1) {
                            var5_9 = var4_6.substring(8);
                        }
                        if ((var6_10 = var5_9.lastIndexOf(47, var5_9.length() - 2)) >= 0) {
                            var5_9 = var5_9.substring(var6_10 + 1);
                        }
                        J.Code.addElement(new Object[]{var4_6.substring(8), new Integer(5), var5_9, null});
                    }
                    var2_2 = System.getProperty("fileconn.dir.roots.names");
                    var3_5 = FileSystemRegistry.listRoots();
                    if (!var3_5.hasMoreElements()) {
                        throw new IOException(a.Code(163));
                    }
                    while (var3_5.hasMoreElements()) {
                        var4_6 = (String)var3_5.nextElement();
                        var5_9 = var4_6;
                        if (var2_2 != null && var2_2.length() > 0) {
                            var6_10 = var2_2.indexOf(59);
                            if (var6_10 != -1) {
                                var5_9 = var2_2.substring(0, var6_10);
                                var2_2 = var2_2.substring(var6_10 + 1);
                            } else {
                                var5_9 = var2_2;
                            }
                        }
                        J.Code.addElement(new Object[]{var4_6, new Integer(5), var5_9, null});
                    }
                    if (J.Code.size() == 1) {
                        J.Code = J.I = (String)((Object[])J.Code.firstElement())[0];
                        J.Code = new Vector<E>();
                    }
                }
            }
            if (J.Code.size() == 0) {
                var2_2 = (String[])Connector.open((String)("file:///" + J.Code), (int)1);
                var3_5 = var2_2.list();
                a.Code(var2_2);
                this.Code = this.Code();
                this = new Vector<E>();
                while (var3_5.hasMoreElements()) {
                    var2_2 = (String)var3_5.nextElement();
                    var4_7 = 3 | var2_2.endsWith("/") << 2;
                    if ((var4_7 & 4) == 0) {
                        if (var1_1) continue;
                        J.I = true;
                    }
                    var2_2 = new Object[]{J.Code + (String)var2_2, new Integer(var4_7), var2_2, null};
                    var4_8 = (var4_7 & 4) == 0 ? J.Code : this;
                    var5_9 = ((String)var2_2[2]).toLowerCase();
                    var6_10 = var4_8.size() - 1;
                    if (var6_10 < 0 || var5_9.compareTo(((String)((Object[])var4_8.lastElement())[2]).toLowerCase()) >= 0) {
                        var4_8.addElement(var2_2);
                        continue;
                    }
                    while (var6_10 > 0 && var5_9.compareTo(((String)((Object[])var4_8.elementAt(var6_10 - 1))[2]).toLowerCase()) < 0) {
                        --var6_10;
                    }
                    var4_8.insertElementAt(var2_2, var6_10);
                }
                for (var2_3 = 0; var2_3 < J.Code.size(); ++var2_3) {
                    this.addElement(J.Code.elementAt(var2_3));
                }
                J.Code = this;
                if (!J.Code() && J.Z == null) {
                    if (var1_1) {
                        J.Code.insertElementAt(new Object[]{"+", new Integer(5), "<" + a.Code(171) + ">", null}, 0);
                    }
                    if (J.I == null || !J.I.equals(J.Code)) {
                        J.Code.insertElementAt(new Object[]{"..", new Integer(5), "..", null}, 0);
                    }
                }
            }
            return J.Code;
        }
        catch (Exception v0) {
            J.Code(v0);
            return J.Code;
        }
    }

    public final Image Code() {
        int n = a.J(0) + a.J(1);
        if (this.Code == null || this.Code.getHeight() != n) {
            this.Code = a.Code(a.Code(82), 0, n, 0, true);
        }
        return this.Code;
    }

    public static int Code(String string) {
        FileConnection fileConnection = null;
        try {
            fileConnection = (FileConnection)Connector.open((String)("file:///" + string), (int)1);
            int n = (int)fileConnection.fileSize();
            a.Code(fileConnection);
            return n;
        }
        catch (Exception exception) {
            J.Code(exception);
            return -1;
        }
        finally {
            a.Code(fileConnection);
        }
    }

    public static InputConnection Code(String string) {
        try {
            return (InputConnection)Connector.open((String)("file:///" + string), (int)1);
        }
        catch (Exception exception) {
            J.Code(exception);
            return null;
        }
    }

    public final Image Code(Object[] objectArray) {
        Image image;
        String string;
        FileConnection fileConnection;
        block4: {
            String string2 = (String)objectArray[0];
            fileConnection = null;
            string = null;
            image = j.Code();
            try {
                fileConnection = (FileConnection)Connector.open((String)("file:///" + string2), (int)1);
                string = (fileConnection.fileSize() + (long)1023 >> 10) + " kB";
                if (fileConnection.fileSize() <= 0L) break block4;
                string2 = a.Code(fileConnection.openDataInputStream());
                if (string2 != null) {
                    J j = a.Code((Image)string2, 0, j.Code.getHeight(), 0, false);
                    image = Image.createImage((int)image.getWidth(), (int)image.getHeight());
                    image.getGraphics().fillRect(0, 0, image.getWidth(), image.getHeight());
                    image.getGraphics().drawImage((Image)j, image.getWidth() - j.getWidth() >> 1, 0, 20);
                }
                if (a.Y == -1) break block4;
                string = a.Y + "x" + a.aa + ", " + string;
            }
            catch (Exception exception) {
                a.Code(fileConnection);
            }
            catch (Throwable throwable) {
                a.Code(fileConnection);
                throw throwable;
            }
        }
        a.Code(fileConnection);
        objectArray[3] = string;
        return image;
    }

    public static boolean Code(String string, String string2) {
        try {
            J.Code();
            J = string2;
            Code = (ContentConnection)Connector.open((String)a.Code(string, false));
            Z = J.Code(false);
            return true;
        }
        catch (Exception exception) {
            J.Code(exception);
            return false;
        }
    }

    public final boolean Code(String string) {
        if (string.length() == 0) {
            string = "unnamed";
        }
        if (Code == null) {
            return false;
        }
        this.I = (int)Code.getLength();
        if (this.I == -1) {
            f.Code(a.Code(165));
            return false;
        }
        this.Code = 0;
        Browser.Code.C();
        InputStream inputStream = null;
        FileConnection fileConnection = null;
        OutputStream outputStream = null;
        try {
            inputStream = Code.openInputStream();
            fileConnection = (FileConnection)Connector.open((String)((Z != null ? Z : "file:///" + Code) + string), (int)3);
            byte[] byArray = new byte[512];
            int n = 1;
            String string2 = string;
            String string3 = "";
            int n2 = string.lastIndexOf(46);
            if (n2 > 0) {
                string2 = string.substring(0, n2);
                string3 = string.substring(n2);
            }
            while (fileConnection.exists()) {
                a.Code(fileConnection);
                string = string2 + "-" + n++ + string3;
                fileConnection = (FileConnection)Connector.open((String)((Z != null ? Z : "file:///" + Code) + string), (int)3);
            }
            fileConnection.create();
            outputStream = fileConnection.openOutputStream();
            while (!this.Code && (n = inputStream.read(byArray)) != -1) {
                this.Code += n;
                outputStream.write(byArray, 0, n);
            }
            if (this.Code) {
                f.Code(a.Code(166), string + " (" + (this.Code + 1023 >> 10) + " kB)");
            } else {
                f.Code(a.Code(167), string + " (" + (this.Code + 1023 >> 10) + " kB)");
            }
            a.Z += (long)this.Code;
        }
        catch (Exception exception) {
            J.Code(exception);
            return false;
        }
        finally {
            a.Z += (long)this.Code;
            a.J += (long)this.Code;
            this.Code = -1;
            this.Code = false;
            a.Code(outputStream);
            a.Code(fileConnection);
            a.Code(inputStream);
            J.Code();
            Browser.Code.a();
            Browser.Code.B();
        }
        a.J += (long)this.Code;
        this.Code = -1;
        this.Code = false;
        a.Code(outputStream);
        a.Code(fileConnection);
        a.Code(inputStream);
        J.Code();
        Browser.Code.a();
        Browser.Code.B();
        return true;
    }

    public static void Code(String string) {
        FileConnection fileConnection = null;
        try {
            if (!string.endsWith("/")) {
                string = string + "/";
            }
            if (!(fileConnection = (FileConnection)Connector.open((String)("file:///" + Code + string), (int)3)).exists()) {
                fileConnection.mkdir();
            }
            a.Code(fileConnection);
            return;
        }
        catch (Exception exception) {
            J.Code(exception);
            return;
        }
        finally {
            a.Code(fileConnection);
        }
    }
}

