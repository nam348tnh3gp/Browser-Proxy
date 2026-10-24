/*
 * Decompiled with CFR 0.152.
 */
import java.io.IOException;
import java.io.InputStream;

/*
 * Duplicate member names - consider using --renamedupmembers true
 */
public final class I
extends InputStream {
    public a Code;
    public int Code;
    public boolean Code;
    public boolean I;
    public int I;
    public byte[] Code;
    public boolean Z;
    public int Z;
    public int J;
    public int B;
    private byte[] I = new byte[1];

    public final int Code() {
        int n = this.I;
        if (a.ba) {
            return n | 0x10;
        }
        return n;
    }

    public final int read() throws IOException {
        this.read(this.I, 0, 1);
        return this.I[0] & 0xFF;
    }

    public final int read(byte[] byArray, int n, int n2) throws IOException {
        this.Z = n;
        this.J = n2;
        this.Code = byArray;
        a.I(this);
        while (true) {
            if (this.Code && this.Code != null) {
                throw new IOException();
            }
            if (this.Code == null) {
                return this.J;
            }
            this.Code.Code(this);
            a.Code((Object)this, 1000);
        }
    }

    public final void close() throws IOException {
        this.Code.B(this.Code, 6);
    }

    public final void Code(boolean bl) throws IOException {
        this.Code = true;
        if (bl) {
            this.close();
        }
    }

    public final void Code() throws IOException {
        this.close();
        this.I = 32;
        a.I(true);
    }

    public final void I() {
        this.Code.l();
    }

    public final String Code() {
        if (this.Z) {
            return "Timeout";
        }
        return this.Code.c;
    }

    public final boolean Code() {
        return this.Code.aZ;
    }
}

