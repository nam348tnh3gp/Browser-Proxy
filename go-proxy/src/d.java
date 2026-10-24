/*
 * Decompiled with CFR 0.152.
 */
import java.io.DataInputStream;

/*
 * Duplicate member names - consider using --renamedupmembers true
 */
public final class d {
    private int[] f;
    public byte[] Code;
    public int d;
    public byte[][] Code;
    public short[][] Code;
    public short[] Code;
    public int Code;
    public int[] B;
    public int I;
    public int Z;
    public int[] b;
    public int[] Code;
    private static short[] I;
    private static short[] Z;
    private static short[] J;
    public static int[] I;
    public static int[] Z;
    public int J;
    public int B;
    public int C;
    private int u;
    private int r;
    private int s;
    private int t;
    private int[] e;
    public int[] J;
    public int a;
    public int b;
    public int c;
    public int[] C;
    public int[] a;
    public int e = new int[3];
    public int f;
    public int g;
    public int h;
    public int i;
    public int j;
    public int k;
    public int[] c;
    public int l;
    public int m;
    public int[] d;
    public int[][] Code;
    public int[][] I;
    public byte[][] I;
    public int n;
    public byte[][] Z;
    public int o;
    public int[][] Z;
    public int p = -1;
    public int q;

    /*
     * Unable to fully structure code
     */
    public final int Code(byte[] var1_1, int var2_6, int var3_9, int var4_10, boolean var5_14) {
        this.Code = var1_1;
        this.J = var2_6;
        this.B = var3_9;
        this.Code = var4_10;
        var1_2 = 0;
        block17: while (var1_2 == 0) {
            while (this.r > 0) {
                if (this.r >= this.B) {
                    this.r -= this.B;
                    return 0;
                }
                this.J += this.r;
                this.B -= this.r;
                this.r = 0;
            }
            switch (this.C) {
                default: {
                    continue block17;
                }
                case 1: 
                case 2: {
                    if (this.B < 2) {
                        return this.B;
                    }
                    block19: while (this.B >= 2 && var1_2 == 0 && this.r == 0 && (this.C == 1 || this.C == 2)) {
                        if ((this.Code[this.J] & 255) != 255) {
                            ++this.J;
                            --this.B;
                            continue;
                        }
                        switch (this.Code[this.J + 1] & 255) {
                            case 192: 
                            case 193: {
                                if (this.B < 4) {
                                    return this.B;
                                }
                                this.s = (this.Code[this.J + 2] & 255) << 8 | this.Code[this.J + 3] & 255;
                                if (this.B < 10) {
                                    return this.B;
                                }
                                this.J += 4;
                                this.B -= 4;
                                this.s -= 2;
                                this.l = (this.Code[this.J + 3] & 255) << 8 | this.Code[this.J + 4] & 255;
                                this.m = (this.Code[this.J + 1] & 255) << 8 | this.Code[this.J + 2] & 255;
                                if (var5_14) {
                                    this.C = 0;
                                    return this.l << 16 | this.m;
                                }
                                this.I = this.l >> this.Code;
                                this.Z = this.m >> this.Code;
                                this.c = new int[this.I * this.Z];
                                this.t = this.Code[this.J + 5] & 255;
                                var2_7 = this;
                                for (var3_9 = 0; var3_9 < 3; ++var3_9) {
                                    var2_7.d[var3_9 << 1] = 0;
                                    var2_7.d[(var3_9 << 1) + 1] = 4;
                                }
                                var2_7.Z();
                                this.J += 6;
                                this.B -= 6;
                                this.s -= 6;
                                this.C = 3;
                                continue block19;
                            }
                            case 218: {
                                if (this.B < 4) {
                                    return this.B;
                                }
                                this.s = (this.Code[this.J + 2] & 255) << 8 | this.Code[this.J + 3] & 255;
                                if (this.B < this.s + 2) {
                                    return this.B;
                                }
                                this.d = this.Code[this.J + 4] & 255;
                                this.J += 5;
                                this.B -= 5;
                                this.s -= 3;
                                for (var2_6 = 0; var2_6 < this.d; ++var2_6) {
                                    var3_9 = this.Code[this.J] & 255;
                                    this.a[var2_6] = var2_6;
                                    for (var4_10 = 0; var4_10 < this.t; ++var4_10) {
                                        if (var3_9 != this.e[var4_10]) continue;
                                        this.a[var2_6] = var4_10;
                                        var4_10 = this.t;
                                    }
                                    var3_9 = this.Code[this.J + 1] & 255;
                                    this.d[this.a[var2_6] << 1] = var3_9 >> 4;
                                    this.d[(this.a[var2_6] << 1) + 1] = 4 + (var3_9 & 15);
                                    this.J += 2;
                                    this.B -= 2;
                                    this.s -= 2;
                                }
                                this.Z();
                                this.f = 0;
                                this.e = 0;
                                this.g = 0;
                                this.k = 0;
                                this.I();
                                this.J += this.s;
                                this.B -= this.s;
                                this.C = 4;
                                continue block19;
                            }
                            case 196: {
                                var1_3 = this;
                                if (var1_3.B < 4) {
                                    var1_2 = -2;
                                    continue block19;
                                }
                                var2_6 = (var1_3.Code[var1_3.J + 2] & 255) << 8 | var1_3.Code[var1_3.J + 3] & 255;
                                if (var1_3.B < var2_6 + 2) {
                                    var1_2 = -2;
                                    continue block19;
                                }
                                var2_6 -= 2;
                                var1_3.J += 4;
                                var1_3.B -= 4;
                                while (var2_6 > 0) {
                                    var3_9 = var1_3.Code[var1_3.J] & 15 | (var1_3.Code[var1_3.J] & 16) >> 2;
                                    ++var1_3.J;
                                    --var1_3.B;
                                    --var2_6;
                                    var4_10 = 0;
                                    var6_15 = new short[16];
                                    var7_18 = 0;
                                    for (var8_22 = 0; var8_22 < 16; ++var8_22) {
                                        var6_15[var8_22] = var1_3.Code[var1_3.J + var8_22] & 255;
                                        var7_18 += var6_15[var8_22];
                                    }
                                    var8_23 = new int[var7_18 + 1];
                                    var7_19 = new int[var7_18];
                                    for (var9_24 = 0; var9_24 < 16; ++var9_24) {
                                        var10_25 = 1;
                                        while (var10_25 <= var6_15[var9_24]) {
                                            var8_23[var4_10] = var9_24 + 1;
                                            ++var10_25;
                                            ++var4_10;
                                        }
                                    }
                                    var8_23[var4_10] = 0;
                                    var9_24 = 0;
                                    var10_25 = 0;
                                    var11_26 = var8_23[0];
                                    while (var8_23[var9_24] != 0) {
                                        while (var8_23[var9_24] == var11_26) {
                                            var7_19[var9_24] = var10_25++;
                                            ++var9_24;
                                        }
                                        if (var8_23[var9_24] == 0) continue;
                                        while (var8_23[var9_24] != var11_26) {
                                            var10_25 <<= 1;
                                            ++var11_26;
                                        }
                                    }
                                    var1_3.I[var3_9] = new int[16];
                                    var1_3.Code[var3_9] = new int[16];
                                    var1_3.Code[var3_9] = new short[16];
                                    var8_22 = 0;
                                    var9_24 = 0;
                                    while (var9_24 < 16) {
                                        if (var6_15[var9_24] == 0) {
                                            var1_3.Code[var3_9][var9_24] = -1;
                                            ++var9_24;
                                            continue;
                                        }
                                        var1_3.Code[var3_9][var9_24] = (short)var8_22;
                                        var1_3.I[var3_9][var9_24] = var7_19[var8_22];
                                        var1_3.Code[var3_9][var9_24] = var7_19[var8_22 += var6_15[var9_24] - 1];
                                        ++var8_22;
                                        ++var9_24;
                                    }
                                    var1_3.J += 16;
                                    var1_3.B -= 16;
                                    var2_6 -= 16;
                                    var1_3.I[var3_9] = new byte[var4_10];
                                    for (var8_22 = 0; var8_22 < var4_10; ++var8_22) {
                                        var1_3.I[var3_9][var8_22] = var1_3.Code[var1_3.J + var8_22];
                                    }
                                    var1_3.J += var4_10;
                                    var1_3.B -= var4_10;
                                    var2_6 -= var4_10;
                                    var1_3.Z[var3_9] = new byte[512];
                                    for (var4_10 = 0; var4_10 < 256; ++var4_10) {
                                        var1_3.Z[var3_9][var4_10] = 9;
                                    }
                                    var4_10 = 0;
                                    for (var8_22 = 1; var8_22 <= 8; ++var8_22) {
                                        for (var9_24 = 0; var9_24 < var6_15[var8_22 - 1]; ++var9_24) {
                                            var10_25 = var7_19[var4_10] << 8 - var8_22;
                                            for (var11_26 = 0; var11_26 < 1 << 8 - var8_22; ++var11_26) {
                                                var1_3.Z[var3_9][var10_25 | var11_26] = (byte)var8_22;
                                                var1_3.Z[var3_9][256 + (var10_25 | var11_26)] = var1_3.I[var3_9][var4_10];
                                            }
                                            ++var4_10;
                                        }
                                    }
                                }
                                var1_2 = 0;
                                continue block19;
                            }
                            case 219: {
                                var1_4 = this;
                                if (var1_4.B < 4) {
                                    var1_2 = -2;
                                    continue block19;
                                }
                                var2_6 = (var1_4.Code[var1_4.J + 2] & 255) << 8 | var1_4.Code[var1_4.J + 3] & 255;
                                if (var1_4.B < var2_6 + 2) {
                                    var1_2 = -2;
                                    continue block19;
                                }
                                var1_4.J += 4;
                                var1_4.B -= 4;
                                var2_6 -= 2;
                                while (var2_6 > 0) {
                                    v0 = var1_4.Code[var1_4.J] & 240;
                                    var3_9 = (v0 | -v0) >>> 31;
                                    var4_10 = var1_4.Code[var1_4.J] & 15;
                                    ++var1_4.J;
                                    --var1_4.B;
                                    --var2_6;
                                    var1_4.Z[var4_10] = new int[64];
                                    if (var3_9 != 0) {
                                        for (var3_9 = 0; var3_9 < 64; ++var3_9) {
                                            var6_16 = d.I[var3_9];
                                            var1_4.Z[var4_10][var3_9] = ((var1_4.Code[var1_4.J + (var6_16 << 1)] & 255) << 8 | var1_4.Code[var1_4.J + (var6_16 << 1) + 1] & 255) * d.Z[var3_9];
                                        }
                                        var1_4.J += 128;
                                        var1_4.B -= 128;
                                        var2_6 -= 128;
                                        continue;
                                    }
                                    for (var3_9 = 0; var3_9 < 64; ++var3_9) {
                                        var6_16 = d.I[var3_9];
                                        var1_4.Z[var4_10][var3_9] = (var1_4.Code[var1_4.J + var6_16] & 255) * d.Z[var3_9];
                                    }
                                    var1_4.J += 64;
                                    var1_4.B -= 64;
                                    var2_6 -= 64;
                                }
                                var1_2 = 0;
                                continue block19;
                            }
                            case 221: {
                                if (this.B < 4) {
                                    return this.B;
                                }
                                this.r = (this.Code[this.J + 2] & 255) << 8 | this.Code[this.J + 3] & 255;
                                if (this.B < 6) {
                                    return this.B;
                                }
                                this.h = (this.Code[this.J + 4] & 255) << 8 | this.Code[this.J + 5] & 255;
                                this.g = 0;
                                this.J += 6;
                                this.B -= 6;
                                this.r -= 4;
                                continue block19;
                            }
                            case 217: {
                                this.J += 2;
                                this.B -= 2;
                                this.C = 6;
                                continue block19;
                            }
                            case 0: 
                            case 255: {
                                ++this.J;
                                --this.B;
                                continue block19;
                            }
                        }
                        if (this.B < 4) {
                            return this.B;
                        }
                        this.r = (this.Code[this.J + 2] & 255) << 8 | this.Code[this.J + 3] & 255;
                        this.J += 4;
                        this.B -= 4;
                        this.r -= 2;
                    }
                    continue block17;
                }
                case 3: {
                    if (this.B < this.t * 3) {
                        return this.B;
                    }
                    for (var2_6 = 0; var2_6 < this.t; ++var2_6) {
                        this.e[var2_6] = this.Code[this.J] & 255;
                        var3_9 = this.Code[this.J + 1] & 255;
                        var4_10 = var3_9 & 15;
                        if ((var3_9 >>>= 4) > this.b) {
                            this.b = var3_9;
                        }
                        if (var4_10 > this.a) {
                            this.a = var4_10;
                        }
                        this.B[var2_6] = var3_9;
                        this.J[var2_6] = var4_10;
                        this.f[var2_6] = this.Code[this.J + 2] & 255;
                        this.J += 3;
                        this.B -= 3;
                        this.s -= 3;
                    }
                    if (this.s > 0) {
                        this.r = this.s;
                    }
                    var2_6 = 0;
                    while (var2_6 < this.t) {
                        this.C[var2_6] = (this.l * this.B[var2_6] + this.b - 1) / this.b + 7 >> 3;
                        if (this.Code == 0) {
                            this.Code[var2_6] = new byte[64 * this.C[var2_6] * this.J[var2_6]];
                            ++var2_6;
                            continue;
                        }
                        this.Code[var2_6] = new byte[64 * this.J[var2_6] * this.B[var2_6]];
                        ++var2_6;
                    }
                    this.k = 0;
                    if (this.t > 1) {
                        this.u = (this.b << 3) * ((this.l + 8 * this.b - 1) / (this.b << 3));
                        this.c = (this.a << 3) * ((this.m + 8 * this.a - 1) / (this.a << 3));
                    }
                    this.C = 2;
                    continue block17;
                }
                case 5: {
                    while (this.C == 5) {
                        if (this.B < 2) {
                            return this.B;
                        }
                        if ((this.Code[this.J] & 255) == 255 && this.Code[this.J + 1] != 0) {
                            if ((this.Code[this.J + 1] & 255) >= 208 && (this.Code[this.J + 1] & 255) <= 215) {
                                this.C = 4;
                                this.J += 2;
                                this.B -= 2;
                                continue;
                            }
                            this.C = 2;
                            continue;
                        }
                        ++this.J;
                        --this.B;
                    }
                    continue block17;
                }
                case 4: {
                    var1_5 = this;
                    var2_8 = new short[64];
                    if (var1_5.Code != 0 && var1_5.Code == null) {
                        var1_5.Code = new int[var1_5.a * var1_5.b << 6];
                    }
                    do {
                        var3_9 = var1_5.a[var1_5.e];
                        for (var4_10 = 0; var4_10 < 64; ++var4_10) {
                            var2_8[var4_10] = 0;
                        }
                        var4_11 = var2_8;
                        var6_17 = var3_9;
                        var7_20 = var1_5;
                        var8_22 = var7_20.B;
                        var9_24 = var7_20.n;
                        var10_25 = var7_20.o;
                        var11_26 = var7_20.d[var6_17 << 1];
                        if ((var11_26 = var7_20.I(var11_26)) >= 0) ** GOTO lbl328
                        var7_20.J -= var8_22 - var7_20.B;
                        var7_20.B = var8_22;
                        var7_20.n = var9_24;
                        var7_20.o = var10_25;
                        v1 = var11_26;
                        ** GOTO lbl371
lbl328:
                        // 1 sources

                        var12_27 = var7_20.Code(var11_26);
                        if (var12_27 >= 0) ** GOTO lbl336
                        var7_20.J -= var8_22 - var7_20.B;
                        var7_20.B = var8_22;
                        var7_20.n = var9_24;
                        var7_20.o = var10_25;
                        v1 = var12_27;
                        ** GOTO lbl371
lbl336:
                        // 1 sources

                        var11_26 = d.Code(var11_26, var12_27);
                        var4_11[0] = (short)(var11_26 + var7_20.Code[var6_17]);
                        var11_26 = var7_20.d[(var6_17 << 1) + 1];
                        var12_27 = 1;
                        while (var12_27 < 64) {
                            var13_28 = var7_20.I(var11_26);
                            if (var13_28 >= 0) ** GOTO lbl349
                            var7_20.J -= var8_22 - var7_20.B;
                            var7_20.B = var8_22;
                            var7_20.n = var9_24;
                            var7_20.o = var10_25;
                            v1 = var13_28;
                            ** GOTO lbl371
lbl349:
                            // 1 sources

                            var14_29 = var13_28 & 15;
                            var13_28 >>= 4;
                            if (var14_29 == 0) {
                                if (var13_28 != 15) break;
                                var12_27 += var13_28;
                                ++var12_27;
                                continue;
                            }
                            var15_30 = var7_20.Code(var14_29);
                            if (var15_30 >= 0) ** GOTO lbl364
                            var7_20.J -= var8_22 - var7_20.B;
                            var7_20.B = var8_22;
                            var7_20.n = var9_24;
                            var7_20.o = var10_25;
                            v1 = var15_30;
                            ** GOTO lbl371
lbl364:
                            // 1 sources

                            if ((var12_27 += var13_28) >= 64) {
                                var12_27 = 63;
                            }
                            var4_11[var12_27] = (short)d.Code(var14_29, var15_30);
                            ++var12_27;
                        }
                        var7_20.Code[var6_17] = var4_11[0];
                        v1 = var4_12 = 0;
lbl371:
                        // 5 sources

                        if (v1 != 0) {
                            var1_2 = var4_12;
                            continue block17;
                        }
                        var4_12 = var1_5.b[var3_9] / (var1_5.J[var3_9] * var1_5.B[var3_9]);
                        var6_17 = var1_5.b[var3_9] % (var1_5.J[var3_9] * var1_5.B[var3_9]);
                        if ((var4_12 = var1_5.B[var3_9] * var4_12 + var6_17 % var1_5.B[var3_9]) < var1_5.C[var3_9]) {
                            if (var1_5.Code == 0) {
                                var4_12 = var4_12 + (var6_17 / var1_5.B[var3_9] * var1_5.C[var3_9] << 3) << 3;
                                var1_5.Code(var3_9, var2_8, var1_5.Code[var3_9], var4_12, var1_5.C[var3_9] << 3);
                            } else {
                                var4_12 = var6_17 / var1_5.B[var3_9] * (var1_5.B[var3_9] << 6) + (var6_17 % var1_5.B[var3_9] << 3);
                                var1_5.Code(var3_9, var2_8, var1_5.Code[var3_9], var4_12, var1_5.B[var3_9] << 3);
                            }
                        }
                        v2 = var3_9;
                        var1_5.b[v2] = var1_5.b[v2] + 1;
                        --var1_5.i;
                        if (var1_5.i % var1_5.j == 0 && var1_5.Code != 0) {
                            var1_5.Code(true);
                            var4_13 = var1_5;
                            var6_17 = var4_13.p % var4_13.l;
                            var7_21 = var4_13.p / var4_13.l;
                            var6_17 >>= var4_13.Code;
                            var6_17 = (var7_21 >>= var4_13.Code) * var4_13.I + var6_17;
                            var8_22 = 0;
                            for (var9_24 = 0; var9_24 < var4_13.a << 3 >> var4_13.Code && var9_24 + var7_21 < var4_13.Z; ++var9_24) {
                                for (var10_25 = 0; var10_25 < var4_13.q >> var4_13.Code; ++var10_25) {
                                    var4_13.c[var6_17 + var10_25] = var4_13.Code[var8_22 + (var10_25 << var4_13.Code)];
                                }
                                var6_17 += var4_13.I;
                                var8_22 += var4_13.b << 3 << var4_13.Code;
                            }
                            var1_5.p = -1;
                        }
                        if (var1_5.i == 0) {
                            if (var1_5.Code == 0) {
                                var1_5.Code(false);
                            }
                            ++var1_5.k;
                            if (var1_5.d > 1 && var1_5.k >= var1_5.c / (8 * var1_5.a) || var1_5.d == 1 && var1_5.k >= (var1_5.J[var3_9] * var1_5.m + var1_5.a - 1) / var1_5.a + 7 >> 3) {
                                var1_5.C = 2;
                                var1_2 = 0;
                                continue block17;
                            }
                            var1_5.I();
                        }
                        ++var1_5.f;
                        if (var1_5.f < var1_5.J[var3_9] * var1_5.B[var3_9] && var1_5.d != 1) continue;
                        var1_5.f = 0;
                        ++var1_5.e;
                        if (var1_5.e < var1_5.d) continue;
                        var1_5.e = 0;
                        ++var1_5.g;
                    } while (var1_5.g != var1_5.h || var1_5.h <= 0);
                    var1_5.g = 0;
                    var1_5.Z();
                    var1_5.C = 5;
                    var1_2 = 0;
                    continue block17;
                }
                case 6: {
                    return 0;
                }
                case 0: 
            }
            if (this.B < 2) {
                return this.B;
            }
            this.J += 2;
            this.B -= 2;
            this.C = 1;
        }
        if (var1_2 == -2) {
            return this.B;
        }
        return var1_2;
    }

    private int Code(int n) {
        if (n == 0) {
            return 0;
        }
        if (this.o < n) {
            this.J();
            if (this.o < n) {
                return -2;
            }
        }
        int n2 = this.n >>> 32 - n;
        this.n <<= n;
        this.o -= n;
        return n2;
    }

    public d() {
        this.J = new int[3];
        this.B = new int[3];
        this.C = new int[3];
        this.a = new int[3];
        this.b = new int[3];
        this.Code = new byte[3][];
        this.d = new int[6];
        this.Code = new int[16][];
        this.I = new int[16][];
        this.Code = new short[16][];
        this.I = new byte[16][];
        this.Z = new byte[16][];
        this.Code = new short[3];
        this.f = new int[3];
        this.Z = new int[4][];
        d.Code();
        for (int i = 0; i < 3; ++i) {
            this.Code[i] = null;
            this.Code[i] = null;
            this.I[i] = null;
            this.Code[i] = null;
            this.I[i] = null;
            this.Z[i] = null;
        }
    }

    private void Code(boolean bl) {
        int n = this.a << 3;
        int n2 = n * this.k;
        int[] nArray = new int[3];
        int[] nArray2 = new int[3];
        int[] nArray3 = new int[3];
        int[] nArray4 = new int[3];
        if (this.t == 3) {
            nArray[0] = 0;
            nArray2[0] = (this.J[0] << 11) / this.a;
            nArray4[0] = (this.B[0] << 11) / this.b;
            nArray[1] = 0;
            nArray2[1] = (this.J[1] << 11) / this.a;
            nArray4[1] = (this.B[1] << 11) / this.b;
            nArray[2] = 0;
            nArray2[2] = (this.J[2] << 11) / this.a;
            nArray4[2] = (this.B[2] << 11) / this.b;
        }
        if (!bl) {
            this.q = this.l;
        } else {
            int n3 = this.q = this.i == 0 ? this.l % (this.b << 3) : this.b << 3;
            if (this.q == 0) {
                this.q = this.b << 3;
            }
        }
        nArray4 = bl ? this.Code : this.c;
        for (int i = 0; i < n; ++i) {
            int n4;
            if (n2 + i >= this.m || this.t != 3) continue;
            int n5 = (n2 + i) * this.l;
            if (bl) {
                n5 += ((this.C[0] + 1 & 0xFFFFFFFE) << 3) - ((this.i / this.j + 1) * this.b << 3);
                if (this.p == -1) {
                    this.p = n5;
                }
                n5 = i * this.b << 3;
            }
            int n6 = (nArray[0] >> 11) * (bl ? this.B[0] << 3 : this.C[0] << 3);
            nArray[0] = nArray[0] + nArray2[0];
            nArray3[0] = 0;
            int n7 = (nArray[1] >> 11) * (bl ? this.B[1] << 3 : this.C[1] << 3);
            nArray[1] = nArray[1] + nArray2[1];
            nArray3[1] = 0;
            int n8 = (nArray[2] >> 11) * (bl ? this.B[2] << 3 : this.C[2] << 3);
            nArray[2] = nArray[2] + nArray2[2];
            nArray3[2] = 0;
            if (this.B[0] != 2 || this.B[1] != 1 || this.B[2] != 1) continue;
            for (n4 = 0; n4 < this.q >> 1; ++n4) {
                int n9 = this.Code[0][n6 + (n4 << 1)] & 0xFF;
                int n10 = this.Code[1][n7 + n4] & 0xFF;
                int n11 = this.Code[2][n8 + n4] & 0xFF;
                short s = Z[n11];
                short s2 = J[n10];
                n10 -= 128;
                n10 = 705 * n10 + 1463 * (n11 -= 128) + 1024 >> 11;
                n11 = I[n9 + s + 256];
                short s3 = I[n9 - n10 + 256];
                n9 = I[n9 + s2 + 256];
                nArray4[n5++] = n11 << 16 | s3 << 8 | n9;
                n9 = this.Code[0][n6 + (n4 << 1) + 1] & 0xFF;
                n11 = I[n9 + s + 256];
                n10 = I[n9 - n10 + 256];
                n9 = I[n9 + s2 + 256];
                nArray4[n5++] = n11 << 16 | n10 << 8 | n9;
            }
            if ((this.l & 1) == 0 || this.i != 0) continue;
            n4 = bl ? 8 * this.b : this.l;
            n6 = this.Code[0][n6 + n4 - 1] & 0xFF;
            n7 = this.Code[1][n7 + (n4 - 1 >> 1)] & 0xFF;
            n8 = this.Code[2][n8 + (n4 - 1 >> 1)] & 0xFF;
            n4 = I[n6 + J[n7] + 256];
            n7 -= 128;
            n7 = I[n6 - (705 * n7 + 1463 * (n8 -= 128) + 1024 >> 11) + 256];
            n6 = I[n6 + Z[n8 + 128] + 256];
            nArray4[n5] = n6 << 16 | n7 << 8 | n4;
        }
    }

    private static int Code(int n, int n2) {
        if (n2 < 1 << n - 1) {
            return n2 + (-1 << n) + 1;
        }
        return n2;
    }

    private static int Z(int n, int n2) {
        return (n >> 8) * n2;
    }

    private static synchronized void Code() {
        if (I == null) {
            DataInputStream dataInputStream = null;
            try {
                int n;
                dataInputStream = a.Code(1);
                I = new short[768];
                Z = new short[256];
                J = new short[256];
                I = new int[64];
                Z = new int[64];
                short s = 0;
                for (n = 0; n < 768; ++n) {
                    d.I[n] = s = (short)(s + dataInputStream.readShort());
                }
                s = 0;
                for (n = 0; n < 256; ++n) {
                    d.Z[n] = s = (short)(s + dataInputStream.readShort());
                }
                s = 0;
                for (n = 0; n < 256; ++n) {
                    d.J[n] = s = (short)(s + dataInputStream.readShort());
                }
                s = 0;
                for (n = 0; n < 64; ++n) {
                    s = (short)(s + dataInputStream.readShort());
                    d.I[n] = s;
                }
                s = 0;
                for (n = 0; n < 64; ++n) {
                    s = (short)(s + dataInputStream.readShort());
                    d.Z[n] = s;
                }
            }
            catch (Throwable throwable) {
                a.Code((Object)dataInputStream);
            }
            a.Code((Object)dataInputStream);
            return;
        }
    }

    private void Z() {
        for (int i = 0; i < 3; ++i) {
            this.Code[i] = 0;
        }
        this.n = 0;
        this.o = 0;
    }

    private void I() {
        if (this.d == 1) {
            int n = this.a[this.e];
            this.i = (this.B[n] * this.l + this.b - 1) / this.b + 7 >> 3;
            this.b[this.a[this.e]] = 0;
            return;
        }
        this.i = 0;
        for (int i = 0; i < this.d; ++i) {
            this.i += this.J[this.a[i]] * this.B[this.a[i]];
            this.b[this.a[i]] = 0;
        }
        this.j = this.i;
        this.i *= this.u / (8 * this.b);
    }

    private int I(int n) {
        int n2;
        byte by;
        if (this.o < 8) {
            this.J();
            if (this.o < 8) {
                return this.I(n, 1);
            }
        }
        if ((by = this.Z[n][n2 = this.n >>> 24]) <= 8) {
            this.n <<= by;
            this.o -= by;
            return this.Z[n][256 + n2] & 0xFF;
        }
        return this.I(n, 9);
    }

    private int I(int n, int n2) {
        int n3 = n2 - 1;
        if ((n2 = this.Code(n2)) < 0) {
            return n2;
        }
        while (n3 < 16 && n2 > this.Code[n][n3]) {
            ++n3;
            int n4 = this.Code(1);
            if (n4 < 0) {
                return n4;
            }
            n2 = n2 << 1 | n4;
        }
        n2 = this.Code[n][n3] + (n2 - this.I[n][n3]);
        return this.I[n][n2] & 0xFF;
    }

    private void J() {
        while (this.o <= 24 && this.B > 0) {
            int n = this.Code[this.J] & 0xFF;
            if (n == 255) {
                if (this.B < 2 || this.Code[this.J + 1] != 0) {
                    return;
                }
                ++this.J;
                --this.B;
            }
            ++this.J;
            --this.B;
            this.n |= n << 24 - this.o;
            this.o += 8;
        }
    }

    private void Code(int n, short[] sArray, byte[] byArray, int n2, int n3) {
        int n4;
        int n5;
        int n6;
        int n7;
        int n8;
        int n9;
        int[] nArray = new int[64];
        int n10 = 0;
        while (n10 < 8) {
            if (sArray[I[n10 + 8]] == 0 && sArray[I[n10 + 16]] == 0 && sArray[I[n10 + 24]] == 0 && sArray[I[n10 + 32]] == 0 && sArray[I[n10 + 40]] == 0 && sArray[I[n10 + 48]] == 0 && sArray[I[n10 + 56]] == 0) {
                nArray[n10] = n9 = sArray[I[n10]] * this.Z[this.f[n]][n10];
                nArray[n10 + 8] = n9;
                nArray[n10 + 16] = n9;
                nArray[n10 + 24] = n9;
                nArray[n10 + 32] = n9;
                nArray[n10 + 40] = n9;
                nArray[n10 + 48] = n9;
                nArray[n10 + 56] = n9;
                ++n10;
                continue;
            }
            nArray[n10] = sArray[I[n10]] * this.Z[this.f[n]][n10];
            nArray[n10 + 8] = sArray[I[n10 + 8]] * this.Z[this.f[n]][n10 + 8];
            nArray[n10 + 16] = sArray[I[n10 + 16]] * this.Z[this.f[n]][n10 + 16];
            nArray[n10 + 24] = sArray[I[n10 + 24]] * this.Z[this.f[n]][n10 + 24];
            nArray[n10 + 32] = sArray[I[n10 + 32]] * this.Z[this.f[n]][n10 + 32];
            nArray[n10 + 40] = sArray[I[n10 + 40]] * this.Z[this.f[n]][n10 + 40];
            nArray[n10 + 48] = sArray[I[n10 + 48]] * this.Z[this.f[n]][n10 + 48];
            nArray[n10 + 56] = sArray[I[n10 + 56]] * this.Z[this.f[n]][n10 + 56];
            n9 = nArray[n10];
            n8 = nArray[n10 + 32];
            n7 = nArray[n10 + 16] - nArray[n10 + 48];
            n6 = nArray[n10 + 16] + nArray[n10 + 48];
            n5 = n9 - n8;
            n7 = d.Z(n7, 362) - n6;
            n4 = n5 - n7;
            n7 = n5 + n7;
            n9 += n8;
            n8 = n9 - n6;
            n9 += n6;
            n6 = nArray[n10 + 40] - nArray[n10 + 24];
            n5 = nArray[n10 + 8] + nArray[n10 + 56];
            int n11 = nArray[n10 + 24] + nArray[n10 + 40];
            int n12 = n5 + n11;
            n5 = d.Z(n5 - n11, 362);
            n11 = nArray[n10 + 8] - nArray[n10 + 56];
            int n13 = d.Z(n6 + n11, 196);
            n6 = d.Z(-n6, 277) - n13;
            n11 = d.Z(n11, 669) - n13 - n12;
            n5 = n11 - n5;
            nArray[n10] = n9 + n12;
            nArray[n10 + 8] = n7 + n11;
            nArray[n10 + 16] = n4 - n5;
            nArray[n10 + 24] = n8 - (n6 -= n5);
            nArray[n10 + 32] = n8 + n6;
            nArray[n10 + 40] = n4 + n5;
            nArray[n10 + 48] = n7 - n11;
            nArray[n10 + 56] = n9 - n12;
            ++n10;
        }
        for (int i = 0; i < 64; i += 8) {
            if (nArray[i + 1] == 0 && nArray[i + 2] == 0 && nArray[i + 3] == 0 && nArray[i + 4] == 0 && nArray[i + 5] == 0 && nArray[i + 6] == 0 && nArray[i + 7] == 0) {
                byArray[n2] = n = (int)((byte)I[(nArray[i] + 1024 >> 11) + 128 + 256]);
                byArray[n2 + 1] = n;
                byArray[n2 + 2] = n;
                byArray[n2 + 3] = n;
                byArray[n2 + 4] = n;
                byArray[n2 + 5] = n;
                byArray[n2 + 6] = n;
                byArray[n2 + 7] = n;
            } else {
                n = nArray[i];
                int n14 = nArray[i + 4];
                n10 = nArray[i + 2] - nArray[i + 6];
                n9 = nArray[i + 2] + nArray[i + 6];
                n8 = n - n14;
                n10 = d.Z(n10, 362) - n9;
                n7 = n8 - n10;
                n10 = n8 + n10;
                n += n14;
                n14 = n - n9;
                n += n9;
                n9 = nArray[i + 5] - nArray[i + 3];
                n8 = nArray[i + 1] + nArray[i + 7];
                n6 = nArray[i + 3] + nArray[i + 5];
                n5 = n8 + n6;
                n8 = d.Z(n8 - n6, 362);
                n6 = nArray[i + 1] - nArray[i + 7];
                n4 = d.Z(n9 + n6, 196);
                n9 = d.Z(-n9, 277) - n4;
                n6 = d.Z(n6, 669) - n4 - n5;
                n8 = n6 - n8;
                byArray[n2] = (byte)I[(n + n5 + 1024 >> 11) + 128 + 256];
                byArray[n2 + 1] = (byte)I[(n10 + n6 + 1024 >> 11) + 128 + 256];
                byArray[n2 + 2] = (byte)I[(n7 - n8 + 1024 >> 11) + 128 + 256];
                byArray[n2 + 3] = (byte)I[(n14 - (n9 -= n8) + 1024 >> 11) + 128 + 256];
                byArray[n2 + 4] = (byte)I[(n14 + n9 + 1024 >> 11) + 128 + 256];
                byArray[n2 + 5] = (byte)I[(n7 + n8 + 1024 >> 11) + 128 + 256];
                byArray[n2 + 6] = (byte)I[(n10 - n6 + 1024 >> 11) + 128 + 256];
                byArray[n2 + 7] = (byte)I[(n - n5 + 1024 >> 11) + 128 + 256];
            }
            n2 += n3;
        }
    }
}

