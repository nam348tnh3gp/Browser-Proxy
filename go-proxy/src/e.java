/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.microedition.io.Connector
 *  javax.wireless.messaging.Message
 *  javax.wireless.messaging.MessageConnection
 *  javax.wireless.messaging.TextMessage
 */
import javax.microedition.io.Connector;
import javax.wireless.messaging.Message;
import javax.wireless.messaging.MessageConnection;
import javax.wireless.messaging.TextMessage;

public final class e
implements Runnable {
    private String Code = null;
    private String I = null;

    public e(String string, String string2) {
        StringBuffer stringBuffer = new StringBuffer();
        if (string != null) {
            for (int i = 0; i < string.length(); ++i) {
                if (!Character.isDigit(string.charAt(i)) && (string.charAt(i) != '+' || i != 0)) continue;
                stringBuffer.append(string.charAt(i));
            }
        }
        string = stringBuffer.toString();
        this.Code = "sms://" + string;
        this.I = string2;
    }

    public final void Code() {
        a.Code(this);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public final void run() {
        MessageConnection messageConnection = null;
        try {
            messageConnection = (MessageConnection)Connector.open((String)this.Code);
            TextMessage textMessage = (TextMessage)messageConnection.newMessage("text");
            textMessage.setPayloadText(this.I);
            messageConnection.send((Message)textMessage);
            f.Code("SMS", a.Code(71));
            return;
        }
        catch (Exception exception) {
            f.Code("SMS", a.Code(55));
            return;
        }
        finally {
            a.Code(messageConnection);
        }
    }
}

