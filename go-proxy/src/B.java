/*
 * Decompiled with CFR 0.152.
 */
import java.io.ByteArrayOutputStream;

public final class B
extends ByteArrayOutputStream {
    public B(int n) {
        super(n);
    }

    public B() {
    }

    public final byte[] Code() {
        return this.buf;
    }

    public final void Code(int n) {
        this.count = n;
    }
}

