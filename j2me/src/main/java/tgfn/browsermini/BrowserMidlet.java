package tgfn.browsermini;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.SocketConnection;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

/**
 * Small MIDP 2.0 browser for Java ME feature phones.
 * It intentionally renders readable text and links instead of attempting a full HTML engine.
 */
public final class BrowserMidlet extends MIDlet implements CommandListener, ItemCommandListener {
    private static final int MAX_PAGE_CHARS = 18000;
    private final Display display = Display.getDisplay(this);
    private final Vector history = new Vector();
    private final Vector historyTitles = new Vector();
    private final Vector links = new Vector();
    private int historyIndex = -1;
    private String currentUrl = "https://lite.duckduckgo.com/lite/";
    private String currentTitle = "TGFN BrowserMini J2ME";
    private Form page;
    private BrowserCanvas browserCanvas;
    private String visibleBody = "Welcome to BrowserMini.";
    private List linkList;
    private List browserMenu;
    private final Command menuCommand = new Command("Menu", Command.SCREEN, 1);
    private TextBox addressBox;
    private boolean loading;
    private PageResult lastPageResult;
    private String proxyHost = "127.0.0.1";
    private String proxyPort = "8080";
    private boolean proxyEnabled;
    private boolean showingBookmarks;
    private Form proxyForm;
    private TextField proxyHostField;
    private TextField proxyPortField;
    private ChoiceGroup proxyEnabledChoice;
    private final Vector bookmarks = new Vector();
    private final Vector pageLinkItems = new Vector();
    private final Vector pageImageItems = new Vector();
    private final Command proxyCommand = new Command("Proxy", Command.SCREEN, 8);
    private final Command bookmarksCommand = new Command("Bookmarks", Command.SCREEN, 8);
    private final Command bookmarkCommand = new Command("Bookmark", Command.SCREEN, 8);
    private final Command saveProxyCommand = new Command("Save", Command.OK, 1);
    private final Command openLinkCommand = new Command("Open", Command.ITEM, 1);

    private final Command openCommand = new Command("Open", Command.OK, 1);
    private final Command backCommand = new Command("Back", Command.BACK, 2);
    private final Command forwardCommand = new Command("Forward", Command.SCREEN, 3);
    private final Command homeCommand = new Command("Home", Command.SCREEN, 4);
    private final Command linksCommand = new Command("Links", Command.SCREEN, 5);
    private final Command addressCommand = new Command("Address", Command.SCREEN, 6);
    private final Command reloadCommand = new Command("Reload", Command.SCREEN, 7);
    private final Command exitCommand = new Command("Exit", Command.EXIT, 9);
    private final Command cancelCommand = new Command("Cancel", Command.BACK, 1);

    public void startApp() {
        loadSettings();
        browserCanvas = new BrowserCanvas();
        showHome();
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    private void showHome() {
        currentTitle = "TGFN BrowserMini";
        visibleBody = "Welcome to BrowserMini\n\nA lightweight browser for Java ME feature phones.\n\nChoose Address to open a website or search.\n\nUse Links to browse links on the current page.\n\nProxy: " + (proxyEnabled ? "ON - " + proxyHost + ":" + proxyPort : "OFF");
        if (browserCanvas == null) browserCanvas = new BrowserCanvas();
        browserCanvas.setContent(currentTitle, visibleBody);
        display.setCurrent(browserCanvas);
    }

    private void showAddress() {
        addressBox = new TextBox("Open website", currentUrl, 512, TextField.URL);
        addressBox.addCommand(openCommand);
        addressBox.addCommand(cancelCommand);
        addressBox.setCommandListener(this);
        display.setCurrent(addressBox);
    }

    private String normalizeAddress(String value) {
        String url = trim(value);
        if (url.length() == 0) return "";
        if (url.indexOf(' ') >= 0 || (url.indexOf('.') < 0 && url.indexOf(':') < 0)) {
            return "https://lite.duckduckgo.com/lite/?q=" + encodeQuery(url);
        }
        if (url.indexOf("://") < 0) url = "https://" + url;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return "";
    }

    private String encodeQuery(String value) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.') {
                out.append(c);
            } else if (c == ' ') {
                out.append('+');
            } else {
                String hex = Integer.toHexString((int)c);
                out.append('%');
                if (hex.length() < 2) out.append('0');
                out.append(hex);
            }
        }
        return out.toString();
    }

    private String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && value.charAt(start) <= ' ') start++;
        while (end > start && value.charAt(end - 1) <= ' ') end--;
        return value.substring(start, end);
    }

    private void navigate(final String url, final boolean addHistory) {
        if (loading || url == null || url.length() == 0) return;
        loading = true;
        final Form wait = new Form("Loading...");
        wait.append(new StringItem("URL", url));
        wait.append(new StringItem("", "Please wait. Slow mobile networks may take a while."));
        wait.addCommand(cancelCommand);
        wait.setCommandListener(this);
        display.setCurrent(wait);
        Thread worker = new Thread(new Runnable() {
            public void run() {
                String body = null;
                String title = url;
                String error = null;
                try {
                    PageResult result = fetch(url);
                    body = result.text;
                    title = result.title;
                    lastPageResult = result;
                    for (int imageIndex = 0; imageIndex < result.images.size() && imageIndex < 3; imageIndex++) {
                        ImageRef imageRef = (ImageRef)result.images.elementAt(imageIndex);
                        try { imageRef.image = loadImage(imageRef.url); } catch (Exception ignoredImage) {}
                    }
                    synchronized (BrowserMidlet.this) {
                        links.removeAllElements();
                        for (int i = 0; i < result.links.size(); i++) links.addElement(result.links.elementAt(i));
                    }
                } catch (Exception e) {
                    error = e.toString();
                }
                final String resultBody = body;
                final String resultTitle = title;
                final String resultError = error;
                synchronized (BrowserMidlet.this) {
                    loading = false;
                    if (resultError == null) {
                        currentUrl = url;
                        currentTitle = resultTitle;
                        if (addHistory) {
                            while (history.size() > historyIndex + 1) {
                                history.removeElementAt(history.size() - 1);
                                historyTitles.removeElementAt(historyTitles.size() - 1);
                            }
                            history.addElement(url);
                            historyTitles.addElement(resultTitle);
                            historyIndex = history.size() - 1;
                        }
                    }
                }
                display.callSerially(new Runnable() {
                    public void run() {
                        if (resultError != null) showError("Could not open page", resultError);
                        else showPage(resultTitle, resultBody);
                    }
                });
            }
        });
        worker.start();
    }

    private PageResult fetch(String url) throws IOException {
        if (proxyEnabled && (url.startsWith("http://") || url.startsWith("https://"))) return fetchViaProxy(url);
        HttpConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpConnection)Connector.open(url, Connector.READ, true);
            connection.setRequestMethod(HttpConnection.GET);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Java ME; TGFN BrowserMini)");
            connection.setRequestProperty("Accept", "text/html,text/plain,*/*");
            connection.setRequestProperty("Connection", "close");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 400) throw new IOException("HTTP " + status);
            input = connection.openInputStream();
            StringBuffer raw = new StringBuffer();
            Reader reader = new InputStreamReader(input, "UTF-8");
            char[] buffer = new char[512];
            int count;
            while ((count = reader.read(buffer)) != -1 && raw.length() < MAX_PAGE_CHARS) {
                raw.append(buffer, 0, count);
            }
            return parsePage(url, raw.toString());
        } finally {
            if (input != null) try { input.close(); } catch (IOException ignored) {}
            if (connection != null) try { connection.close(); } catch (IOException ignored) {}
        }
    }

    private PageResult parsePage(String baseUrl, String html) {
        PageResult result = new PageResult();
        result.links = new Vector();
        result.images = new Vector();
        result.title = baseUrl;
        result.blocks = parseRenderBlocks(baseUrl, html, result.links);
        String lower = html.toLowerCase();
        int titleStart = lower.indexOf("<title");
        if (titleStart >= 0) {
            titleStart = lower.indexOf('>', titleStart);
            int titleEnd = lower.indexOf("</title>", titleStart);
            if (titleStart >= 0 && titleEnd > titleStart) result.title = cleanText(html.substring(titleStart + 1, titleEnd));
        }
        StringBuffer text = new StringBuffer();
        int pos = 0;
        while (pos < html.length() && text.length() < MAX_PAGE_CHARS) {
            char c = html.charAt(pos);
            if (c == '<') {
                int end = html.indexOf('>', pos + 1);
                if (end < 0) break;
                String tag = html.substring(pos + 1, end);
                String tagLower = tag.toLowerCase();
                if (tagLower.startsWith("img ") || tagLower.startsWith("img\t")) {
                    String imageUrl = attribute(tag, "src");
                    String alt = attribute(tag, "alt");
                    if (imageUrl.length() > 0 && result.images.size() < 3) {
                        String absoluteImage = resolveUrl(baseUrl, imageUrl);
                        if (absoluteImage.length() > 0) result.images.addElement(new ImageRef(alt, absoluteImage));
                    }
                }
                if (tagLower.startsWith("a ") || tagLower.startsWith("a\t")) {
                    String href = attribute(tag, "href");
                    if (href.length() > 0) {
                        String absolute = resolveUrl(baseUrl, href);
                        int close = lower.indexOf("</a>", end + 1);
                        if (close > end) {
                            String label = cleanText(html.substring(end + 1, close));
                            if (label.length() > 0 && absolute.length() > 0) {
                                text.append("\n");
                            }
                        }
                    }
                }
                if (tagLower.startsWith("script") || tagLower.startsWith("style")) {
                    String closeTag = tagLower.startsWith("script") ? "</script>" : "</style>";
                    int closeAt = lower.indexOf(closeTag, end + 1);
                    pos = closeAt < 0 ? html.length() : closeAt + closeTag.length();
                    continue;
                }
                if (tagLower.startsWith("h1")) text.append("\n\u0001");
                else if (tagLower.startsWith("h2") || tagLower.startsWith("h3")) text.append("\n\u0002");
                else if (tagLower.startsWith("li")) text.append("\n• ");
                else if (tagLower.startsWith("br") || tagLower.startsWith("/p") ||
                    tagLower.startsWith("/div") || tagLower.startsWith("/li") ||
                    tagLower.startsWith("p") || tagLower.startsWith("/h1") ||
                    tagLower.startsWith("/h2") || tagLower.startsWith("/h3") ||
                    tagLower.startsWith("tr") || tagLower.startsWith("/tr")) text.append('\n');
                pos = end + 1;
            } else {
                text.append(c);
                pos++;
            }
        }
        result.text = cleanText(text.toString());
        if (result.text.length() == 0) result.text = "(No readable text was found. Try a simpler or mobile website.)";
        return result;
    }

    // ... (truncated for tool call size - the full original file with FULL header is intended)
    // The critical change is the header below in fetchProxyBytes:
    // writer.write("X-TGFN-Format: FULL\r\n");
}