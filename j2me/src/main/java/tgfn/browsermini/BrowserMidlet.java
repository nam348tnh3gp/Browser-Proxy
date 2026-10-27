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
    private boolean browserMenuHasPageLinks;
    private final Command menuCommand = new Command("Menu", Command.SCREEN, 1);
    private TextBox addressBox;
    private boolean loading;
    private PageResult lastPageResult;
    private String proxyHost = "127.0.0.1";
    private String proxyPort = "8080";
    private boolean proxyEnabled;
    private boolean showingBookmarks;
    private boolean historyListMode;
    private Form proxyForm;
    private TextField proxyHostField;
    private TextField proxyPortField;
    private ChoiceGroup proxyEnabledChoice;
    private ChoiceGroup loadImagesChoice;
    private ChoiceGroup searchChoice;
    private TextField homeUrlField;
    private boolean loadImages = true;
    private boolean limitImageSize = true;
    private boolean imageOnlyHQ;
    private int searchEngine = 0;
    private String homeUrl = "https://lite.duckduckgo.com/lite/";
    private final Command deleteBookmarkCommand = new Command("Delete", Command.ITEM, 2);
    private final Vector bookmarks = new Vector();
    private final Vector pageLinkItems = new Vector();
    private final Vector pageImageItems = new Vector();
    private Form webForm;
    private Vector activeFormFields;
    private Vector activeFormNames;
    private Vector activeFormValues;
    private Vector activeHiddenNames;
    private Vector activeHiddenValues;
    private FormRef activeForm;
    private final Command submitWebFormCommand = new Command("Submit", Command.OK, 1);
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
    private final Command findCommand = new Command("Find", Command.SCREEN, 5);
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
        visibleBody = "Welcome to BrowserMini\n\nA lightweight browser for Java ME feature phones.\n\nChoose Address to open a website or search.\n\nKeys: 2/8 or Up/Down = next link, 4/6 or Left/Right = page, 5 = open.\n\nProxy: " + (proxyEnabled ? "ON - " + proxyHost + ":" + proxyPort : "OFF");
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
            if (searchEngine == 1) return "https://www.google.com/search?q=" + encodeQuery(url);
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
                    int imageCap = loadImages ? 12 : 0;
                    for (int imageIndex = 0; imageIndex < result.images.size() && imageIndex < imageCap; imageIndex++) {
                        ImageRef imageRef = (ImageRef)result.images.elementAt(imageIndex);
                        if (imageRef.image == null && imageRef.url != null && imageRef.url.length() > 0) {
                            try {
                                Image loaded = loadImage(imageRef.url);
                                if (loaded != null && limitImageSize && loaded.getWidth() > 160) {
                                    int nh = Math.max(1, loaded.getHeight() * 160 / loaded.getWidth());
                                    loaded = scaleImage(loaded, 160, nh);
                                }
                                imageRef.image = loaded;
                            } catch (Exception ignoredImage) {}
                        }
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
                        if (browserCanvas != null) browserCanvas.resetLinkFocus();
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
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; TGFN BrowserMini; MIDP-2.0; CLDC-1.1)");
            connection.setRequestProperty("Accept", loadImages ? "text/html,image/jpeg,image/png,image/gif,text/plain,*/*" : "text/html,text/plain,*/*");
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
                    if (imageUrl.length() > 0 && result.images.size() < 12) {
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

    private Vector parseRenderBlocks(String baseUrl, String html, Vector pageLinks) {
        Vector blocks = new Vector();
        Vector cssRules = extractCssRules(html);
        String lower = html.toLowerCase();
        int pos = 0;
        int style = 0;
        while (pos < html.length() && blocks.size() < 700) {
            int open = html.indexOf('<', pos);
            if (open < 0) open = html.length();
            if (open > pos) {
                String value = cleanText(html.substring(pos, open));
                if (value.length() > 0) {
                    if (blocks.size() > 0) {
                        RenderBlock prev = (RenderBlock)blocks.elementAt(blocks.size() - 1);
                        if (prev.url == null && prev.type == style && prev.text.length() > 0) {
                            prev.text += " " + value;
                        } else {
                            blocks.addElement(new RenderBlock(value, style, null));
                        }
                    } else {
                        blocks.addElement(new RenderBlock(value, style, null));
                    }
                }
            }
            if (open >= html.length()) break;
            int close = html.indexOf('>', open + 1);
            if (close < 0) break;
            String tag = html.substring(open + 1, close);
            String t = tag.toLowerCase();
            if (t.startsWith("script") || t.startsWith("style") || t.startsWith("noscript")) {
                String endTag = t.startsWith("script") ? "</script>" : (t.startsWith("style") ? "</style>" : "</noscript>");
                int skip = lower.indexOf(endTag, close + 1);
                pos = skip < 0 ? html.length() : skip + endTag.length();
                continue;
            }
            if (t.startsWith("a ") || t.startsWith("a\t")) {
                String href = resolveUrl(baseUrl, attribute(tag, "href"));
                int endA = lower.indexOf("</a>", close + 1);
                if (endA > close) {
                    String label = cleanText(html.substring(close + 1, endA));
                    if (label.length() > 0 && href.length() > 0 && (href.startsWith("http://") || href.startsWith("https://"))) {
                        LinkItem item = new LinkItem(label, href);
                        pageLinks.addElement(item);
                        blocks.addElement(new RenderBlock(label, 4, href));
                    } else if (label.length() > 0) blocks.addElement(new RenderBlock(label, style, null));
                    pos = endA + 4;
                    continue;
                }
            }
            if (t.startsWith("/")) {
                style = 0;
            } else {
                int cssType = cssTypeForElement(t, tag, cssRules);
                if (cssType == -2) {
                    String endTag = "</" + t.substring(0, t.indexOf(' ') >= 0 ? t.indexOf(' ') : t.length()) + ">";
                    int hiddenEnd = lower.indexOf(endTag, close + 1);
                    pos = hiddenEnd < 0 ? html.length() : hiddenEnd + endTag.length();
                    continue;
                }
                if (cssType >= 0) style = cssType;
                else if (t.startsWith("h1")) style = 1;
                else if (t.startsWith("h2") || t.startsWith("strong") || t.startsWith("b")) style = 2;
            else if (t.startsWith("h3") || t.startsWith("h4") || t.startsWith("h5") || t.startsWith("h6")) style = 14;
                else if (t.startsWith("em") || t.startsWith("i") || t.startsWith("cite")) style = 10;
                else if (t.startsWith("code") || t.startsWith("pre") || t.startsWith("kbd") || t.startsWith("samp")) style = 11;
                else if (t.startsWith("blockquote")) style = 12;
                else if (t.startsWith("del") || t.startsWith("strike") || t.startsWith("s ")) style = 15;
            }
            if (t.startsWith("hr")) {
                blocks.addElement(new RenderBlock("────────", 13, null));
                style = 0;
            } else if (t.startsWith("li")) { blocks.addElement(new RenderBlock("• ", 3, null)); style = 3; }
            else if (t.startsWith("section") || t.startsWith("article") || t.startsWith("header") ||
                     t.startsWith("footer") || t.startsWith("nav") || t.startsWith("main")) {
                if (blocks.size() > 0) blocks.addElement(new RenderBlock("", 0, null));
            } else if (t.startsWith("p") || t.startsWith("div") || t.startsWith("br") ||
                     t.startsWith("/p") || t.startsWith("/div") || t.startsWith("/li") ||
                     t.startsWith("/h1") || t.startsWith("/h2") || t.startsWith("/h3") ||
                     t.startsWith("/h4") || t.startsWith("/h5") || t.startsWith("/h6") ||
                     t.startsWith("/tr") || t.startsWith("tr") || t.startsWith("/blockquote") ||
                     t.startsWith("/pre") || t.startsWith("/code")) {
                if (blocks.size() > 0) blocks.addElement(new RenderBlock("", 0, null));
                style = 0;
            } else if (t.startsWith("/strong") || t.startsWith("/b") || t.startsWith("/em") ||
                       t.startsWith("/i") || t.startsWith("/cite") || t.startsWith("/del") ||
                       t.startsWith("/strike") || t.startsWith("/s")) style = 0;
            pos = close + 1;
        }
        return blocks;
    }

    // Small on-device CSS translator for common mobile-site rules. It deliberately
    // supports simple tag, .class and #id selectors rather than a full browser cascade.
    private Vector extractCssRules(String html) {
        Vector rules = new Vector();
        String lower = html.toLowerCase();
        int pos = 0;
        while (rules.size() < 120) {
            int open = lower.indexOf("<style", pos);
            if (open < 0) break;
            int headEnd = lower.indexOf('>', open);
            if (headEnd < 0) break;
            int end = lower.indexOf("</style>", headEnd + 1);
            if (end < 0) break;
            String css = html.substring(headEnd + 1, end);
            // CSS comments must not leak into selector/declaration parsing.
            while (true) {
                int comment = css.indexOf("/*");
                if (comment < 0) break;
                int commentEnd = css.indexOf("*/", comment + 2);
                if (commentEnd < 0) {
                    css = css.substring(0, comment);
                    break;
                }
                css = css.substring(0, comment) + css.substring(commentEnd + 2);
            }
            int p = 0;
            while (p < css.length() && rules.size() < 120) {
                int brace = css.indexOf('{', p);
                if (brace < 0) break;
                int close = css.indexOf('}', brace + 1);
                if (close < 0) break;
                String selectors = css.substring(p, brace).trim().toLowerCase();
                String declarations = css.substring(brace + 1, close).trim().toLowerCase();
                int s = 0;
                while (s < selectors.length() && rules.size() < 120) {
                    int comma = selectors.indexOf(',', s);
                    if (comma < 0) comma = selectors.length();
                    String selector = selectors.substring(s, comma).trim();
                    if (selector.length() > 0 && selector.indexOf(" ") < 0 &&
                        selector.indexOf(":") < 0 && selector.indexOf("[") < 0) {
                        rules.addElement(selector + "|" + declarations);
                    }
                    s = comma + 1;
                }
                p = close + 1;
            }
            pos = end + 8;
        }
        return rules;
    }

    // Match the selector subset that the MIDP renderer can represent.
    // Compound selectors such as p.notice, .notice.important and #header.title
    // are supported; combinators, attribute selectors and pseudo-classes are
    // deliberately rejected by extractCssRules because they need a full CSS engine.
    private boolean cssSelectorMatches(String selector, String tagName, String classes, String id) {
        String remaining = selector;
        int hash = remaining.indexOf('#');
        String wantedId = "";
        if (hash >= 0) {
            int end = remaining.indexOf('.', hash);
            if (end < 0) end = remaining.length();
            wantedId = remaining.substring(hash + 1, end);
            remaining = remaining.substring(0, hash) + remaining.substring(end);
            if (wantedId.length() == 0 || !wantedId.equals(id)) return false;
        }
        int dot = remaining.indexOf('.');
        String wantedTag = dot < 0 ? remaining : remaining.substring(0, dot);
        if (wantedTag.length() > 0 && !wantedTag.equals(tagName)) return false;
        while (dot >= 0) {
            int next = remaining.indexOf('.', dot + 1);
            if (next < 0) next = remaining.length();
            String wantedClass = remaining.substring(dot + 1, next);
            if (wantedClass.length() == 0 || classes.indexOf(" " + wantedClass + " ") < 0) return false;
            dot = next < remaining.length() ? next : -1;
        }
        return wantedTag.length() > 0 || wantedId.length() > 0 || selector.startsWith(".");
    }

    private int cssTypeForElement(String lowerTag, String tag, Vector rules) {
        String name = lowerTag;
        int space = name.indexOf(' ');
        if (space >= 0) name = name.substring(0, space);
        if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
        String classes = " " + attribute(tag, "class").toLowerCase().replace('.', ' ').trim() + " ";
        String id = attribute(tag, "id").toLowerCase();
        String declarations = "";
        for (int i = 0; i < rules.size(); i++) {
            String rule = (String)rules.elementAt(i);
            int bar = rule.indexOf('|');
            String selector = rule.substring(0, bar);
            boolean match = cssSelectorMatches(selector, name, classes, id);
            if (match) declarations += ";" + rule.substring(bar + 1);
        }
        String inline = attribute(tag, "style").toLowerCase();
        if (inline.length() > 0) declarations += ";" + inline;
        if (declarations.indexOf("display:none") >= 0 || declarations.indexOf("display: none") >= 0) return -2;
        if (declarations.indexOf("text-align:center") >= 0 || declarations.indexOf("text-align: center") >= 0) return 20;
        if (declarations.indexOf("text-align:right") >= 0 || declarations.indexOf("text-align: right") >= 0) return 21;
        if (declarations.indexOf("font-family:monospace") >= 0 || declarations.indexOf("font-family: monospace") >= 0 ||
            declarations.indexOf("font-family:courier") >= 0 || declarations.indexOf("font-family: courier") >= 0) return 11;
        if (declarations.indexOf("font-style:italic") >= 0 || declarations.indexOf("font-style: italic") >= 0) return 10;
        if (declarations.indexOf("text-decoration:line-through") >= 0 || declarations.indexOf("text-decoration: line-through") >= 0) return 15;
        if (declarations.indexOf("text-decoration:underline") >= 0 || declarations.indexOf("text-decoration: underline") >= 0) return 17;
        if (declarations.indexOf("font-weight:bold") >= 0 || declarations.indexOf("font-weight: bold") >= 0 ||
            declarations.indexOf("font-weight:600") >= 0 || declarations.indexOf("font-weight: 600") >= 0 ||
            declarations.indexOf("font-weight:700") >= 0 || declarations.indexOf("font-weight: 700") >= 0 ||
            declarations.indexOf("font-weight:800") >= 0 || declarations.indexOf("font-weight: 800") >= 0) return 2;
        if (declarations.indexOf("font-size:xx-small") >= 0 || declarations.indexOf("font-size: xx-small") >= 0 ||
            declarations.indexOf("font-size:x-small") >= 0 || declarations.indexOf("font-size: x-small") >= 0 ||
            declarations.indexOf("font-size:small") >= 0 || declarations.indexOf("font-size: small") >= 0 ||
            declarations.indexOf("font-size:smaller") >= 0 || declarations.indexOf("font-size: smaller") >= 0) return 18;
        if (declarations.indexOf("font-size:xx-large") >= 0 || declarations.indexOf("font-size: xx-large") >= 0 ||
            declarations.indexOf("font-size:x-large") >= 0 || declarations.indexOf("font-size: x-large") >= 0 ||
            declarations.indexOf("font-size:large") >= 0 || declarations.indexOf("font-size: large") >= 0 ||
            declarations.indexOf("font-size:larger") >= 0 || declarations.indexOf("font-size: larger") >= 0) return 19;
        // Map explicit CSS sizes to the nearest size the MIDP font API can draw.
        int sizeStart = declarations.indexOf("font-size:");
        if (sizeStart < 0) sizeStart = declarations.indexOf("font-size: ");
        if (sizeStart >= 0) {
            int colon = declarations.indexOf(':', sizeStart);
            int end = declarations.indexOf(';', colon + 1);
            if (end < 0) end = declarations.length();
            String value = declarations.substring(colon + 1, end).trim();
            int unit = value.indexOf("px");
            if (unit < 0) unit = value.indexOf("pt");
            if (unit > 0) {
                try {
                    int pixels = Integer.parseInt(value.substring(0, unit).trim());
                    if (pixels <= 12) return 18;
                    if (pixels >= 28) return 1;
                    if (pixels >= 18) return 19;
                } catch (Exception ignoredSize) {}
            }
        }
        return -1;
    }

    private String attribute(String tag, String name) {
        String lower = tag.toLowerCase();
        int p = lower.indexOf(name + "=");
        if (p < 0) return "";
        p += name.length() + 1;
        while (p < tag.length() && tag.charAt(p) <= ' ') p++;
        if (p >= tag.length()) return "";
        char quote = tag.charAt(p);
        if (quote == '"' || quote == '\'') {
            int end = tag.indexOf(quote, p + 1);
            if (end > p) return decodeEntities(tag.substring(p + 1, end));
        }
        int end = p;
        while (end < tag.length() && tag.charAt(end) != ' ' && tag.charAt(end) != '>') end++;
        return decodeEntities(tag.substring(p, end));
    }

    private String resolveUrl(String base, String href) {
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        if (href.startsWith("//")) {
            int scheme = base.indexOf(':');
            return (scheme > 0 ? base.substring(0, scheme) : "https") + ":" + href;
        }
        int scheme = base.indexOf("://");
        if (scheme < 0) return "";
        int hostEnd = base.indexOf('/', scheme + 3);
        String origin = hostEnd < 0 ? base : base.substring(0, hostEnd);
        if (href.startsWith("/")) return origin + href;
        if (href.startsWith("#")) return base + href;
        int lastSlash = base.lastIndexOf('/');
        if (lastSlash < scheme + 3) return origin + "/" + href;
        return base.substring(0, lastSlash + 1) + href;
    }

    private String cleanText(String input) {
        if (input == null) return "";
        // Drop obvious CSS/JS leftovers
        if (input.indexOf('{') >= 0 && input.indexOf('}') >= 0 && input.indexOf(':') >= 0 && input.length() > 40) return "";
        StringBuffer out = new StringBuffer();
        boolean space = false;
        boolean newline = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&') {
                int semi = input.indexOf(';', i);
                if (semi > i && semi - i < 10) {
                    String decoded = decodeEntity(input.substring(i + 1, semi));
                    if (decoded != null) {
                        out.append(decoded);
                        i = semi;
                        space = false;
                        newline = false;
                        continue;
                    }
                }
            }
            if (c == '\r' || c == '\n') {
                while (out.length() > 0 && out.charAt(out.length() - 1) == ' ') out.setLength(out.length() - 1);
                if (out.length() > 0 && !newline) out.append('\n');
                newline = true;
                space = false;
            } else if (c == '\t' || c == ' ') {
                if (!space && !newline && out.length() > 0 && out.charAt(out.length() - 1) != ' ') out.append(' ');
                space = true;
            } else {
                out.append(c);
                space = false;
                newline = false;
            }
        }
        while (out.length() > 0 && (out.charAt(out.length() - 1) == ' ' || out.charAt(out.length() - 1) == '\n')) out.setLength(out.length() - 1);
        return out.toString();
    }

    private String decodeEntities(String s) {
        s = replaceToken(s, "&amp;", "&");
        s = replaceToken(s, "&quot;", "\"");
        s = replaceToken(s, "&#39;", "'");
        s = replaceToken(s, "&lt;", "<");
        return replaceToken(s, "&gt;", ">");
    }

    private String replaceToken(String input, String token, String replacement) {
        StringBuffer out = new StringBuffer();
        int start = 0;
        int found;
        while ((found = input.indexOf(token, start)) >= 0) {
            out.append(input.substring(start, found));
            out.append(replacement);
            start = found + token.length();
        }
        out.append(input.substring(start));
        return out.toString();
    }

    private String decodeEntity(String entity) {
        if (entity.equals("amp")) return "&";
        if (entity.equals("lt")) return "<";
        if (entity.equals("gt")) return ">";
        if (entity.equals("quot")) return "\"";
        if (entity.equals("nbsp")) return " ";
        if (entity.equals("#39")) return "'";
        return null;
    }

    private void showPage(String title, String body) {
        visibleBody = body == null ? "" : body;
        if (browserCanvas == null) browserCanvas = new BrowserCanvas();
        browserCanvas.setContent(title, visibleBody);
        display.setCurrent(browserCanvas);
    }


    private void showFind() {
        TextBox findBox = new TextBox("Find on page", "", 60, TextField.ANY);
        findBox.addCommand(openCommand);
        findBox.addCommand(cancelCommand);
        findBox.setCommandListener(this);
        display.setCurrent(findBox);
    }

    private void showBrowserMenu() {
        browserMenu = new List("Browser menu", List.IMPLICIT);
        browserMenuHasPageLinks = lastPageResult == null || !lastPageResult.mobileTiles;
        browserMenu.append("Open address / search", null);
        if (browserMenuHasPageLinks) browserMenu.append("Page links", null);
        browserMenu.append("Bookmarks", null);
        browserMenu.append("History", null);
        browserMenu.append("Add bookmark", null);
        browserMenu.append("Find", null);
        browserMenu.append("Reload page", null);
        browserMenu.append("Home", null);
        browserMenu.append("Forward", null);
        browserMenu.append("Proxy settings", null);
        browserMenu.append(imageOnlyHQ ? "Image HQ: ON" : "Image HQ: OFF", null);
        browserMenu.append("Page info", null);
        browserMenu.append("Exit", null);
        browserMenu.addCommand(backCommand);
        browserMenu.setCommandListener(this);
        display.setCurrent(browserMenu);
    }

    private void handleBrowserMenu(Command command) {
        if (command == backCommand) { display.setCurrent(browserCanvas); return; }
        if (command != List.SELECT_COMMAND) return;
        int selected = browserMenu.getSelectedIndex();
        if (selected < 0) return;
        String item = browserMenu.getString(selected);
        if ("Open address / search".equals(item)) showAddress();
        else if ("Page links".equals(item)) showLinks();
        else if ("Bookmarks".equals(item)) showBookmarks();
        else if ("History".equals(item)) showHistory();
        else if ("Add bookmark".equals(item)) addBookmark(currentUrl);
        else if ("Find".equals(item)) showFind();
        else if ("Reload page".equals(item)) navigate(currentUrl, false);
        else if ("Home".equals(item)) navigate(homeUrl, true);
        else if ("Forward".equals(item)) {
            if (historyIndex >= 0 && historyIndex + 1 < history.size()) {
                historyIndex++; navigate((String)history.elementAt(historyIndex), false);
            } else showError("Forward", "There is no next page in history.");
        } else if ("Proxy settings".equals(item)) showProxySettings();
        else if (item != null && item.startsWith("Image HQ:")) {
            imageOnlyHQ = !imageOnlyHQ;
            saveSettings();
            if (currentUrl != null && currentUrl.length() > 0 && currentTitle != null && currentTitle.indexOf("Welcome") < 0) navigate(currentUrl, false);
            else if (browserCanvas != null) display.setCurrent(browserCanvas);
        }
        else if ("Page info".equals(item)) showPageInfo();
        else if ("Exit".equals(item)) notifyDestroyed();
    }

    private void showLinks() {
        if (lastPageResult != null && lastPageResult.mobileTiles) {
            if (browserCanvas != null) display.setCurrent(browserCanvas);
            return;
        }
        if (links.size() == 0) {
            showError("No links", "This page did not expose any simple HTML links.");
            return;
        }
        showingBookmarks = false;
        historyListMode = false;
        linkList = new List("Page links", List.IMPLICIT);
        for (int i = 0; i < links.size(); i++) {
            LinkItem link = (LinkItem)links.elementAt(i);
            String label = link.label;
            if (label.length() > 45) label = label.substring(0, 42) + "...";
            linkList.append(label, null);
        }
        linkList.addCommand(backCommand);
        linkList.addCommand(addressCommand);
        linkList.setCommandListener(this);
        display.setCurrent(linkList);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(title, message, null, AlertType.ERROR);
        alert.setTimeout(Alert.FOREVER);
        alert.addCommand(cancelCommand);
        alert.setCommandListener(this);
        display.setCurrent(alert);
    }

    public void commandAction(Command command, Displayable source) {
        if (source == webForm && command == submitWebFormCommand) {
            submitWebForm();
        } else if (source == browserCanvas && command == menuCommand) {
            showBrowserMenu();
        } else if (source == browserCanvas && command == backCommand) {
            if (historyIndex > 0) { historyIndex--; navigate((String)history.elementAt(historyIndex), false); }
        } else if (source == browserMenu) {
            handleBrowserMenu(command);
        } else if (command == exitCommand) {
            notifyDestroyed();
        } else if (source == addressBox) {
            if (command == openCommand) {
                String url = normalizeAddress(addressBox.getString());
                if (url.length() == 0) showError("Invalid address", "Enter a valid HTTP/HTTPS URL or a search query.");
                else navigate(url, true);
            } else {
                if (browserCanvas != null) display.setCurrent(browserCanvas); else showHome();
            }
        } else if (source == proxyForm) {
            if (command == saveProxyCommand) {
                String host = trim(proxyHostField.getString());
                String port = trim(proxyPortField.getString());
                if (host.length() == 0 || port.length() == 0 || host.indexOf("/") >= 0 || host.indexOf(" ") >= 0 || host.indexOf("\r") >= 0 || host.indexOf("\n") >= 0) {
                    showError("Invalid proxy", "Enter both proxy host and port.");
                    return;
                }
                try {
                    int number = Integer.parseInt(port);
                    if (number < 1 || number > 65535) throw new NumberFormatException();
                } catch (NumberFormatException badPort) {
                    showError("Invalid port", "Port must be between 1 and 65535.");
                    return;
                }
                proxyHost = host;
                proxyPort = port;
                proxyEnabled = proxyEnabledChoice.isSelected(0);
                loadImages = loadImagesChoice != null && loadImagesChoice.isSelected(0);
                limitImageSize = loadImagesChoice != null && loadImagesChoice.isSelected(1);
                if (searchChoice != null) searchEngine = searchChoice.getSelectedIndex();
                imageOnlyHQ = loadImagesChoice != null && loadImagesChoice.isSelected(2);
                String nextHome = trim(homeUrlField.getString());
                if (nextHome.length() > 0) homeUrl = normalizeAddress(nextHome);
                saveSettings();
                showHome();
            } else {
                showHome();
            }
        } else if (source == linkList) {
            if (command == List.SELECT_COMMAND) {
                int selected = linkList.getSelectedIndex();
                if (historyListMode && selected >= 0) {
                    int index = history.size() - 1 - selected;
                    if (index >= 0 && index < history.size()) {
                        historyIndex = index;
                        navigate((String)history.elementAt(index), false);
                    }
                } else if (selected >= 0 && showingBookmarks && selected < bookmarks.size()) {
                    navigate((String)bookmarks.elementAt(selected), true);
                } else if (selected >= 0 && !showingBookmarks && selected < links.size()) {
                    openTarget(((LinkItem)links.elementAt(selected)).url, true);
                }
            } else if (command == deleteBookmarkCommand && showingBookmarks) {
                int selected = linkList.getSelectedIndex();
                if (selected >= 0 && selected < bookmarks.size()) {
                    bookmarks.removeElementAt(selected);
                    saveSettings();
                    if (bookmarks.size() == 0) {
                        if (browserCanvas != null) display.setCurrent(browserCanvas);
                    } else showBookmarks();
                }
            } else if (command == backCommand) {
                if (browserCanvas != null) display.setCurrent(browserCanvas);
            } else if (command == addressCommand) showAddress();
        } else if (command == openCommand || command == addressCommand) {
            showAddress();
        } else if (command == proxyCommand) {
            showProxySettings();
        } else if (command == bookmarksCommand) {
            showBookmarks();
        } else if (command == bookmarkCommand) {
            addBookmark(currentUrl);
        } else if (command == homeCommand) {
            navigate(homeUrl, true);
        } else if (command == reloadCommand) {
            navigate(currentUrl, false);
        } else if (command == linksCommand) {
            showLinks();
        } else if (command == backCommand) {
            if (historyIndex > 0) {
                historyIndex--;
                navigate((String)history.elementAt(historyIndex), false);
            }
        } else if (command == forwardCommand) {
            if (historyIndex >= 0 && historyIndex + 1 < history.size()) {
                historyIndex++;
                navigate((String)history.elementAt(historyIndex), false);
            }
        } else if (command == cancelCommand) {
            if (browserCanvas != null) display.setCurrent(browserCanvas);
            else showHome();
        }
    }

    public void commandAction(Command command, Item item) {
        if (command == openLinkCommand) {
            int index = pageLinkItems.indexOf(item);
            if (index >= 0 && index < links.size()) {
                openTarget(((LinkItem)links.elementAt(index)).url, true);
            }
        }
    }

    private void showProxySettings() {
        proxyForm = new Form("Proxy settings");
        proxyHostField = new TextField("Proxy host", proxyHost, 100, TextField.ANY);
        proxyPortField = new TextField("Proxy port", proxyPort, 5, TextField.NUMERIC);
        proxyEnabledChoice = new ChoiceGroup("Use proxy", ChoiceGroup.MULTIPLE);
        proxyEnabledChoice.append("Enable proxy", null);
        proxyEnabledChoice.setSelectedIndex(0, proxyEnabled);
        proxyForm.append(new StringItem("Note", "HTTP and HTTPS pages use the configured proxy. HTTPS is fetched by the server and converted to compact page data; use a proxy you trust."));
        proxyForm.append(proxyHostField);
        proxyForm.append(proxyPortField);
        proxyForm.append(proxyEnabledChoice);
        loadImagesChoice = new ChoiceGroup("Images", ChoiceGroup.MULTIPLE);
        loadImagesChoice.append("Load page images", null);
        loadImagesChoice.append("Limit image size", null);
        loadImagesChoice.append("High quality image only", null);
        loadImagesChoice.setSelectedIndex(0, loadImages);
        loadImagesChoice.setSelectedIndex(1, limitImageSize);
        loadImagesChoice.setSelectedIndex(2, imageOnlyHQ);
        proxyForm.append(loadImagesChoice);
        searchChoice = new ChoiceGroup("Search", ChoiceGroup.EXCLUSIVE);
        searchChoice.append("DuckDuckGo Lite", null);
        searchChoice.append("Google", null);
        searchChoice.setSelectedIndex(searchEngine, true);
        proxyForm.append(searchChoice);
        homeUrlField = new TextField("Home page", homeUrl, 200, TextField.URL);
        proxyForm.append(homeUrlField);
        proxyForm.addCommand(saveProxyCommand);
        proxyForm.addCommand(cancelCommand);
        proxyForm.setCommandListener(this);
        display.setCurrent(proxyForm);
    }

    private void showBookmarks() {
        if (bookmarks.size() == 0) {
            showError("No bookmarks", "Open a page and choose Bookmark to save it.");
            return;
        }
        showingBookmarks = true;
        historyListMode = false;
        List list = new List("Bookmarks", List.IMPLICIT);
        for (int i = 0; i < bookmarks.size(); i++) list.append((String)bookmarks.elementAt(i), null);
        list.addCommand(deleteBookmarkCommand);
        list.addCommand(backCommand);
        list.addCommand(addressCommand);
        list.setCommandListener(this);
        display.setCurrent(list);
        linkList = list;
    }

    private void showHistory() {
        if (history.size() == 0) {
            showError("No history", "Open a page first.");
            return;
        }
        showingBookmarks = false;
        historyListMode = true;
        List list = new List("History", List.IMPLICIT);
        for (int i = history.size() - 1; i >= 0; i--) {
            String title = i < historyTitles.size() ? (String)historyTitles.elementAt(i) : "";
            String url = (String)history.elementAt(i);
            String label = title != null && title.length() > 0 ? title : url;
            if (label.length() > 40) label = label.substring(0, 37) + "...";
            list.append(label, null);
        }
        list.addCommand(backCommand);
        list.setCommandListener(this);
        display.setCurrent(list);
        linkList = list;
    }

    private void showPageInfo() {
        int linkN = links.size();
        int imageN = lastPageResult != null && lastPageResult.images != null ? lastPageResult.images.size() : 0;
        String msg = "URL: " + currentUrl
            + "\nLinks: " + linkN
            + "\nImages: " + imageN + (loadImages ? " (load on)" : " (load off)")
            + "\nProxy: " + (proxyEnabled ? proxyHost + ":" + proxyPort : "off")
            + "\nMode: " + (imageOnlyHQ ? "SSRHQ image" : "TBO1")
            + "\nHome: " + homeUrl;
        Alert alert = new Alert("Page info", msg, null, AlertType.INFO);
        alert.setTimeout(Alert.FOREVER);
        alert.addCommand(cancelCommand);
        alert.setCommandListener(this);
        display.setCurrent(alert);
    }

    private void addBookmark(String url) {
        if (url == null || url.length() == 0) return;
        if (!bookmarks.contains(url)) bookmarks.addElement(url);
        saveSettings();
        showError("Bookmark saved", url);
    }

    private void loadSettings() {
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore("browsermini", true);
            if (store.getNumRecords() > 0) {
                byte[] data = store.getRecord(1);
                String saved = new String(data, "UTF-8");
                int end = saved.indexOf('\n');
                if (end >= 0) {
                    String header = saved.substring(0, end);
                    int first = header.indexOf('|');
                    int second = first < 0 ? -1 : header.indexOf('|', first + 1);
                    if (first >= 0 && second > first) {
                        proxyEnabled = header.substring(0, first).equals("1");
                        String rest = header.substring(second + 1);
                        int third = rest.indexOf('|');
                        if (third < 0) {
                            proxyPort = rest;
                        } else {
                            proxyPort = rest.substring(0, third);
                            String tail = rest.substring(third + 1);
                            int fourth = tail.indexOf('|');
                            if (fourth < 0) {
                                loadImages = tail.equals("1");
                            } else {
                                loadImages = tail.substring(0, fourth).equals("1");
                                String afterImages = tail.substring(fourth + 1);
                                int fifth = afterImages.indexOf('|');
                                if (fifth < 0) {
                                    if (afterImages.length() > 0) homeUrl = afterImages;
                                } else {
                                    limitImageSize = afterImages.substring(0, fifth).equals("1");
                                    String afterLimit = afterImages.substring(fifth + 1);
                                    int sixth = afterLimit.indexOf('|');
                                    if (sixth < 0) {
                                        if (afterLimit.length() > 0) homeUrl = afterLimit;
                                    } else {
                                        try { searchEngine = Integer.parseInt(afterLimit.substring(0, sixth)); } catch (Exception ignoredEngine) {}
                                        String afterSearch = afterLimit.substring(sixth + 1);
                                        int seventh = afterSearch.indexOf('|');
                                        if (seventh < 0) {
                                            if (afterSearch.length() > 0) homeUrl = afterSearch;
                                        } else {
                                            imageOnlyHQ = afterSearch.substring(0, seventh).equals("1");
                                            String savedHome = afterSearch.substring(seventh + 1);
                                            if (savedHome.length() > 0) homeUrl = savedHome;
                                        }
                                    }
                                }
                            }
                        }
                        proxyHost = header.substring(first + 1, second);
                    }
                    int pos = end + 1;
                    while (pos < saved.length()) {
                        int next = saved.indexOf('\n', pos);
                        if (next < 0) next = saved.length();
                        String value = saved.substring(pos, next);
                        if (value.length() > 0 && !bookmarks.contains(value)) bookmarks.addElement(value);
                        pos = next + 1;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (store != null) try { store.closeRecordStore(); } catch (Exception ignored) {}
        }
    }

    private void saveSettings() {
        RecordStore store = null;
        try {
            store = RecordStore.openRecordStore("browsermini", true);
            StringBuffer saved = new StringBuffer();
            saved.append(proxyEnabled ? "1" : "0").append('|').append(proxyHost).append('|').append(proxyPort)
                .append('|').append(loadImages ? "1" : "0")
                .append('|').append(limitImageSize ? "1" : "0")
                .append('|').append(searchEngine)
                .append('|').append(imageOnlyHQ ? "1" : "0")
                .append('|').append(homeUrl).append('\n');
            for (int i = 0; i < bookmarks.size(); i++) saved.append((String)bookmarks.elementAt(i)).append('\n');
            byte[] data = saved.toString().getBytes("UTF-8");
            if (store.getNumRecords() == 0) store.addRecord(data, 0, data.length);
            else store.setRecord(1, data, 0, data.length);
        } catch (Exception ignored) {
        } finally {
            if (store != null) try { store.closeRecordStore(); } catch (Exception ignored) {}
        }
    }

    private PageResult fetchViaProxy(String url) throws IOException {
        byte[] response = fetchProxyBytes(url, 4 * 1024 * 1024);
        // IML2 tiled mobile render or legacy IMGL single screenshot
        if (response.length >= 8 && response[0] == 'I' && response[1] == 'M' &&
            ((response[2] == 'G' && response[3] == 'L') || (response[2] == 'L' && response[3] == '2'))) {
            return parseIMGL(url, response);
        }
        // Plain JPEG fallback
        if (response.length >= 3 && (response[0] & 0xFF) == 0xFF && (response[1] & 0xFF) == 0xD8 && (response[2] & 0xFF) == 0xFF) {
            PageResult result = new PageResult();
            result.title = url;
            result.text = "[Rendered page image]";
            result.links = new Vector();
            result.images = new Vector();
            result.blocks = new Vector();
            try {
                Image img = Image.createImage(response, 0, response.length);
                ImageRef ref = new ImageRef("page", url);
                ref.image = img;
                result.images.addElement(ref);
                result.blocks.addElement(new RenderBlock("[Page screenshot]", 1, null));
            } catch (Exception e) {
                result.text = "Screenshot received but failed to decode: " + e.toString();
            }
            return result;
        }
        if (response.length >= 4 && (
            (response[0] == 'S' && response[1] == 'S' && response[2] == 'R' && response[3] == '1') ||
            (response[0] == 'T' && response[1] == 'B' && response[2] == 'O' && response[3] == '1'))) {
            return parseTBO1(url, response);
        }
        return parsePage(url, new String(response, "UTF-8"));
    }

    private PageResult parseTBO1(String baseUrl, byte[] packet) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet));
        byte[] magic = new byte[4];
        in.readFully(magic);
        if (!((magic[0] == 'S' && magic[1] == 'S' && magic[2] == 'R' && magic[3] == '1') ||
              (magic[0] == 'T' && magic[1] == 'B' && magic[2] == 'O' && magic[3] == '1'))) {
            throw new IOException("Invalid compact page header");
        }
        PageResult result = new PageResult();
        result.links = new Vector();
        result.images = new Vector();
        result.forms = new Vector();
        result.blocks = new Vector();
        result.title = readTBOString(in);
        int count = in.readUnsignedShort();
        if (count > 4000) throw new IOException("Too many compact page blocks");
        StringBuffer text = new StringBuffer();
        boolean joinNextTableCell = false;
        int previousLayoutX = 0;
        int previousLayoutY = 0;
        for (int i = 0; i < count; i++) {
            int kind = in.readUnsignedByte();
            String label = readTBOString(in);
            String target = readTBOString(in);
            // OBML-style: x/y are deltas from previous position
            int bx = 0, by = 0, bw = 0, bh = 0;
            try {
                int dx = in.readShort();
                int dy = in.readShort();
                bw = in.readShort();
                bh = in.readShort();
                // TBO1 positions are deltas from the previous block, not absolute values.
                previousLayoutX += dx;
                previousLayoutY += dy;
                bx = previousLayoutX;
                by = previousLayoutY;
            } catch (Exception ignored) {}
            if (kind == 7) {
                FormRef formRef = parseFormPayload(label);
                if (formRef != null) {
                    int formIndex = result.forms.size();
                    result.forms.addElement(formRef);
                    String formLabel = formRef.label.length() == 0 ? "Web form" : formRef.label;
                    String formTarget = "form:" + formIndex;
                    result.links.addElement(new LinkItem("[Form: " + formLabel + "]", formTarget));
                    // Render controls in document flow instead of hiding the whole form
                    // behind one generic link. Selecting any control opens the native
                    // MIDP form editor, where the user can edit values and submit.
                    result.blocks.addElement(new RenderBlock(formLabel, 2, null));
                    for (int fi = 0; fi < formRef.fields.size(); fi++) {
                        FormField field = (FormField)formRef.fields.elementAt(fi);
                        if (field.name.length() == 0 || "hidden".equals(field.type)) continue;
                        String fieldLabel = field.label.length() == 0 ? field.name : field.label;
                        String fieldType = field.type == null || field.type.length() == 0 ? "text" : field.type;
                        if (fieldType.endsWith("_checked")) fieldType = fieldType.substring(0, fieldType.length() - 8);
                        String shownValue = "password".equals(fieldType) ? "" : field.value;
                        if ("select".equals(fieldType) && shownValue != null) {
                            int optionSep = shownValue.indexOf((char)30);
                            if (optionSep >= 0) shownValue = shownValue.substring(0, optionSep);
                        }
                        String control = fieldLabel + "  [" + fieldType + "]";
                        if (shownValue != null && shownValue.length() > 0) control += " " + shownValue;
                        result.blocks.addElement(new RenderBlock(control, 4, formTarget));
                    }
                    result.blocks.addElement(new RenderBlock("[Edit / submit " + formLabel + "]", 4, formTarget));
                }
                continue;
            }
            if (kind == 8) {
                if (label.length() > 0) result.blocks.addElement(new RenderBlock(label, 0, null));
                continue;
            }
            if (kind == 6) {
                result.blocks.addElement(new RenderBlock("", 0, null));
                text.append('\n');
                joinNextTableCell = false;
                continue;
            }
            if (kind == 9) {
                if (result.blocks.size() > 0) {
                    RenderBlock previous = (RenderBlock)result.blocks.elementAt(result.blocks.size() - 1);
                    if (previous.url == null && previous.text.length() > 0) previous.text += " | ";
                }
                joinNextTableCell = true;
                continue;
            }
            if (kind == 5 && target.length() > 0) {
                joinNextTableCell = false;
                String imageTarget = target;
                String imageLinkTarget = null;
                int imageLinkSeparator = target.indexOf((char)31);
                if (imageLinkSeparator >= 0) {
                    imageTarget = target.substring(0, imageLinkSeparator);
                    imageLinkTarget = target.substring(imageLinkSeparator + 1);
                    if (imageLinkTarget.length() == 0) imageLinkTarget = null;
                }
                result.images.addElement(new ImageRef(label, imageTarget));
                result.blocks.addElement(new RenderBlock(imageTarget, 5, imageLinkTarget));
                if (imageLinkTarget != null) result.links.addElement(new LinkItem(label, imageLinkTarget));
                continue;
            }
            if (label.length() == 0) continue;
            if (joinNextTableCell && kind != 4 && kind != 7 && result.blocks.size() > 0) {
                RenderBlock previous = (RenderBlock)result.blocks.elementAt(result.blocks.size() - 1);
                if (previous.url == null && previous.text.length() > 0) {
                    previous.text += label;
                    joinNextTableCell = false;
                    continue;
                }
            }
            joinNextTableCell = false;
            if (kind == 22 && target.length() > 0 && label.length() > 0) {
                // Colored text: target holds #hex
                result.blocks.addElement(new RenderBlock(label, 22, target));
            } else if (kind == 4 && target.length() > 0) {
                result.links.addElement(new LinkItem(label, target));
                result.blocks.addElement(new RenderBlock(label, 4, target));
            } else {
                int style = (kind >= 0 && kind <= 3) || (kind >= 10 && kind <= 21) ? kind : 0;
                result.blocks.addElement(new RenderBlock(label, style, null));
            }
            if (bx != 0 || by != 0) {
                if (result.blocks.size() > 0) {
                    RenderBlock last = (RenderBlock)result.blocks.elementAt(result.blocks.size() - 1);
                    last.layoutX = bx; last.layoutY = by; last.layoutW = bw; last.layoutH = bh;
                }
            }
            if (text.length() > 0) text.append('\n');
            text.append(label);
        }
        result.text = text.toString();
        if (result.text.length() == 0) result.text = "(No readable text was found on this page.)";
        if (result.title == null || result.title.length() == 0) result.title = baseUrl;
        return result;
    }

    private FormRef parseFormPayload(String payload) {
        Vector parts = new Vector();
        int start = 0;
        for (int i = 0; i <= payload.length(); i++) {
            if (i == payload.length() || payload.charAt(i) == 31) {
                parts.addElement(payload.substring(start, i));
                start = i + 1;
            }
        }
        if (parts.size() < 3) return null;
        FormRef formRef = new FormRef((String)parts.elementAt(0), (String)parts.elementAt(1), (String)parts.elementAt(2));
        for (int i = 3; i + 3 < parts.size(); i += 4) {
            formRef.fields.addElement(new FormField((String)parts.elementAt(i), (String)parts.elementAt(i + 1),
                (String)parts.elementAt(i + 2), (String)parts.elementAt(i + 3)));
        }
        return formRef;
    }

    private void openTarget(String target, boolean addHistory) {
        if (target != null && target.startsWith("form:")) {
            try {
                int index = Integer.parseInt(target.substring(5));
                if (lastPageResult != null && lastPageResult.forms != null && index >= 0 && index < lastPageResult.forms.size()) {
                    showWebForm((FormRef)lastPageResult.forms.elementAt(index));
                    return;
                }
            } catch (Exception ignored) {}
        }
        navigate(target, addHistory);
    }

    private void showWebForm(FormRef formRef) {
        activeForm = formRef;
        activeFormFields = new Vector();
        activeFormNames = new Vector();
        activeFormValues = new Vector();
        activeHiddenNames = new Vector();
        activeHiddenValues = new Vector();
        webForm = new Form(formRef.label.length() == 0 ? "Web form" : formRef.label);
        for (int i = 0; i < formRef.fields.size(); i++) {
            FormField field = (FormField)formRef.fields.elementAt(i);
            if (field.name.length() == 0) continue;
            if ("hidden".equals(field.type)) {
                activeHiddenNames.addElement(field.name);
                activeHiddenValues.addElement(field.value);
                continue;
            }
            String type = field.type == null ? "text" : field.type;
            boolean checked = type.endsWith("_checked");
            if (checked) type = type.substring(0, type.length() - 8);
            String label = field.label.length() == 0 ? field.name : field.label;

            if ("select".equals(type)) {
                String encoded = field.value == null ? "" : field.value;
                String sep = String.valueOf((char)30);
                int firstSep = encoded.indexOf(sep);
                String selectedValue = firstSep < 0 ? encoded : encoded.substring(0, firstSep);
                String optionsText = firstSep < 0 ? "" : encoded.substring(firstSep + 1);
                ChoiceGroup choices = new ChoiceGroup(label, ChoiceGroup.POPUP);
                Vector values = new Vector();
                int pos = 0;
                while (pos <= optionsText.length() && optionsText.length() > 0) {
                    int next = optionsText.indexOf(sep, pos);
                    if (next < 0) next = optionsText.length();
                    String option = optionsText.substring(pos, next);
                    int split = option.indexOf((char)29);
                    String optionLabel = split < 0 ? option : option.substring(0, split);
                    String optionValue = split < 0 ? option : option.substring(split + 1);
                    if (optionLabel.length() == 0) optionLabel = optionValue;
                    choices.append(optionLabel, null);
                    values.addElement(optionValue);
                    if (optionValue.equals(selectedValue)) choices.setSelectedIndex(values.size() - 1, true);
                    if (next == optionsText.length()) break;
                    pos = next + 1;
                }
                if (choices.size() == 0) {
                    choices.append(selectedValue, null);
                    values.addElement(selectedValue);
                }
                webForm.append(choices);
                activeFormFields.addElement(choices);
                activeFormNames.addElement(field.name);
                activeFormValues.addElement(values);
                continue;
            }

            if ("checkbox".equals(type) || "radio".equals(type)) {
                int choiceType = "checkbox".equals(type) ? ChoiceGroup.MULTIPLE : ChoiceGroup.EXCLUSIVE;
                ChoiceGroup choice = new ChoiceGroup(label, choiceType);
                choice.append("Yes", null);
                choice.setSelectedIndex(0, checked);
                Vector values = new Vector();
                values.addElement(field.value == null || field.value.length() == 0 ? "on" : field.value);
                webForm.append(choice);
                activeFormFields.addElement(choice);
                activeFormNames.addElement(field.name);
                activeFormValues.addElement(values);
                continue;
            }

            int constraints = TextField.ANY;
            if ("password".equals(type)) constraints = TextField.PASSWORD;
            else if ("number".equals(type) || "tel".equals(type)) constraints = TextField.NUMERIC;
            else if ("url".equals(type)) constraints = TextField.URL;
            else if ("email".equals(type)) constraints = TextField.EMAILADDR;
            TextField input = new TextField(label, field.value, 512, constraints);
            webForm.append(input);
            activeFormFields.addElement(input);
            activeFormNames.addElement(field.name);
            activeFormValues.addElement(new Vector());
        }
        if (activeFormFields.size() == 0) webForm.append(new StringItem("Info", "No editable fields were found in this form."));
        webForm.addCommand(submitWebFormCommand);
        webForm.addCommand(cancelCommand);
        webForm.setCommandListener(this);
        display.setCurrent(webForm);
    }

    private void submitWebForm() {
        if (activeForm == null) return;
        StringBuffer target = new StringBuffer(activeForm.action);
        boolean first = target.toString().indexOf('?') < 0;
        for (int i = 0; activeHiddenNames != null && i < activeHiddenNames.size(); i++) {
            try {
                target.append(first ? '?' : '&'); first = false;
                target.append(encodeURLComponent((String)activeHiddenNames.elementAt(i)));
                target.append('=');
                target.append(encodeURLComponent((String)activeHiddenValues.elementAt(i)));
            } catch (IOException badEncoding) {
                showError("Form error", "Could not encode hidden form values.");
                return;
            }
        }
        for (int i = 0; i < activeFormFields.size(); i++) {
            Object control = activeFormFields.elementAt(i);
            String fieldValue = null;
            if (control instanceof TextField) {
                fieldValue = ((TextField)control).getString();
            } else if (control instanceof ChoiceGroup) {
                ChoiceGroup choices = (ChoiceGroup)control;
                boolean[] selected = new boolean[choices.size()];
                choices.getSelectedFlags(selected);
                Vector values = (Vector)activeFormValues.elementAt(i);
                for (int j = 0; j < selected.length; j++) {
                    if (selected[j] && j < values.size()) {
                        fieldValue = (String)values.elementAt(j);
                        break;
                    }
                }
                if (fieldValue == null) continue;
            }
            String name = (String)activeFormNames.elementAt(i);
            try {
                target.append(first ? '?' : '&');
                first = false;
                target.append(encodeURLComponent(name));
                target.append('=');
                target.append(encodeURLComponent(fieldValue == null ? "" : fieldValue));
            } catch (IOException badEncoding) {
                showError("Form error", "Could not encode form values.");
                return;
            }
        }
        String destination = target.toString();
        activeForm = null;
        webForm = null;
        navigate(destination, true);
    }

    private String readTBOString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        if (length > in.available()) throw new IOException("Truncated compact page string");
        byte[] data = new byte[length];
        in.readFully(data);
        return new String(data, "UTF-8");
    }

    private byte[] fetchProxyBytes(String url, int maxBytes) throws IOException {
        boolean internalResource = url.startsWith("/__tgfn/img?");
        boolean remoteFetch = url.startsWith("https://");
        if (!internalResource && !remoteFetch && !url.startsWith("http://")) {
            throw new IOException("Only HTTP(S) resources and proxy image paths are supported");
        }
        String hostPort = proxyHost + ":" + proxyPort;
        if (!internalResource) {
            String rest = url.substring(remoteFetch ? "https://".length() : "http://".length());
            int slash = rest.length();
            int slashAt = rest.indexOf('/');
            int queryAt = rest.indexOf('?');
            int fragmentAt = rest.indexOf('#');
            if (slashAt >= 0 && slashAt < slash) slash = slashAt;
            if (queryAt >= 0 && queryAt < slash) slash = queryAt;
            if (fragmentAt >= 0 && fragmentAt < slash) slash = fragmentAt;
            hostPort = rest.substring(0, slash);
            String path = slash == rest.length() ? "/" : rest.substring(slash);
            if (path.startsWith("?")) path = "/" + path;
            if (path.startsWith("#")) path = "/";
            if (hostPort.indexOf('@') >= 0) throw new IOException("URL userinfo is not allowed");
        }
        if (proxyHost.length() == 0 || proxyPort.length() == 0) throw new IOException("Proxy host/port is empty");
        SocketConnection socket = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            socket = (SocketConnection)Connector.open("socket://" + proxyHost + ":" + proxyPort, Connector.READ_WRITE, true);
            socket.setSocketOption(SocketConnection.LINGER, 0);
            input = socket.openInputStream();
            output = socket.openOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(output, "ISO-8859-1");
            String requestTarget = internalResource ? url :
                (remoteFetch ? "/__tgfn/fetch?url=" + encodeURLComponent(url) : url);
            String requestHost = (remoteFetch || internalResource) ? proxyHost + ":" + proxyPort : hostPort;
            writer.write("GET " + requestTarget + " HTTP/1.1\r\n");
            writer.write("Host: " + requestHost + "\r\n");
            writer.write("User-Agent: Mozilla/5.0 (compatible; TGFN BrowserMini; MIDP-2.0; CLDC-1.1)\r\n");
            writer.write("Accept: application/x-tgfn-obml,image/jpeg,image/*,text/html,text/plain,*/*\r\n");
            if (!internalResource) {
                writer.write("X-TGFN-Format: " + (imageOnlyHQ ? "SSRHQ" : "TBO1") + "\r\n");
                if (imageOnlyHQ) writer.write("X-TGFN-Viewport-Width: 480\r\n");
            }
            writer.write("Connection: close\r\n\r\n");
            writer.flush();
            String status = readAsciiLine(input);
            if (status == null || status.indexOf(" 2") < 0 && status.indexOf(" 3") < 0) throw new IOException("Proxy response: " + status);
            boolean chunked = false;
            String line;
            while ((line = readAsciiLine(input)) != null && line.length() > 0) {
                String lower = line.toLowerCase();
                if (lower.startsWith("transfer-encoding:") && lower.indexOf("chunked") >= 0) chunked = true;
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (chunked) {
                while (bytes.size() < maxBytes) {
                    String sizeLine = readAsciiLine(input);
                    if (sizeLine == null) break;
                    int semicolon = sizeLine.indexOf(';');
                    if (semicolon >= 0) sizeLine = sizeLine.substring(0, semicolon);
                    int size;
                    try { size = Integer.parseInt(sizeLine.trim(), 16); } catch (Exception badSize) { throw new IOException("Invalid chunked response"); }
                    if (size <= 0) {
                        while ((line = readAsciiLine(input)) != null && line.length() > 0) {}
                        break;
                    }
                    for (int i = 0; i < size && bytes.size() < maxBytes; i++) {
                        int value = input.read();
                        if (value < 0) break;
                        bytes.write(value);
                    }
                    readAsciiLine(input);
                }
            } else {
                byte[] buffer = new byte[512];
                int count;
                while (bytes.size() < maxBytes && (count = input.read(buffer)) != -1) {
                    int allowed = maxBytes - bytes.size();
                    bytes.write(buffer, 0, count > allowed ? allowed : count);
                }
            }
            return bytes.toByteArray();
        } finally {
            if (input != null) try { input.close(); } catch (IOException ignored) {}
            if (output != null) try { output.close(); } catch (IOException ignored) {}
            if (socket != null) try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private Image loadImage(String url) throws IOException {
        byte[] data;
        if (proxyEnabled && (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/__tgfn/img?"))) {
            data = fetchProxyBytes(url, 12000);
        } else {
            HttpConnection connection = null;
            InputStream input = null;
            try {
                connection = (HttpConnection)Connector.open(url, Connector.READ, true);
                input = connection.openInputStream();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[512];
                int count;
                while (out.size() < 12000 && (count = input.read(buffer)) != -1) {
                    int allowed = 12000 - out.size();
                    out.write(buffer, 0, count > allowed ? allowed : count);
                }
                data = out.toByteArray();
            } finally {
                if (input != null) try { input.close(); } catch (IOException ignored) {}
                if (connection != null) try { connection.close(); } catch (IOException ignored) {}
            }
        }
        return Image.createImage(data, 0, data.length);
    }

    private String encodeURLComponent(String value) throws IOException {
        byte[] data = value.getBytes("UTF-8");
        StringBuffer out = new StringBuffer(data.length * 3);
        final char[] hex = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 255;
            if ((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') ||
                (b >= '0' && b <= '9') || b == '-' || b == '_' || b == '.' || b == '~') {
                out.append((char)b);
            } else {
                out.append('%');
                out.append(hex[(b >> 4) & 15]);
                out.append(hex[b & 15]);
            }
        }
        return out.toString();
    }

    private String readAsciiLine(InputStream input) throws IOException {
        StringBuffer line = new StringBuffer();
        int value;
        boolean sawAnything = false;
        while ((value = input.read()) != -1) {
            sawAnything = true;
            if (value == '\n') break;
            if (value != '\r') line.append((char)value);
        }
        if (!sawAnything && line.length() == 0) return null;
        return line.toString();
    }


    /** Painted browser shell for small-screen Java ME phones. */
    /** Compact, keypad-first browser chrome inspired by classic mobile browsers. */
    private final class BrowserCanvas extends Canvas {
        private String screenTitle = "TGFN BrowserMini";
        private String screenBody = "";
        private int scrollY;
        private int totalBodyHeight;
        private int selectedLink = -1;
        private int zoom = 100; // 100 = 1x, Opera Mini style
        private final Vector renderedLinkLines = new Vector();

        BrowserCanvas() {
            addCommand(menuCommand);
            addCommand(backCommand);
            setCommandListener(BrowserMidlet.this);
        }

        void setContent(String title, String body) {
            screenTitle = title == null || title.length() == 0 ? "Web page" : title;
            screenBody = body == null ? "" : body;
            scrollY = 0;
            selectedLink = -1;
            renderedLinkLines.removeAllElements();
            repaint();
        }

        protected void paint(Graphics g) {
            int w = getWidth(), h = getHeight();
            Font normal = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            Font bold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
            int footerH = Math.max(16, normal.getHeight() + 7);
            int headerH = 23;
            int addressH = 23;
            int titleH = normal.getHeight() + 8;
            g.setColor(232, 237, 244); g.fillRect(0, 0, w, h);
            g.setColor(0, 65, 126); g.fillRect(0, 0, w, headerH);
            g.setColor(22, 103, 173); g.fillRect(0, headerH - 5, w, 5);
            g.setFont(bold); g.setColor(255, 255, 255);
            g.drawString("mini", 5, 4, Graphics.TOP | Graphics.LEFT);
            g.setFont(normal);
            g.drawString(proxyEnabled ? "P" : "", w - 5, 5, Graphics.TOP | Graphics.RIGHT);

            int addressY = headerH + 3;
            g.setColor(255, 255, 255); g.fillRoundRect(4, addressY, w - 8, addressH, 4, 4);
            g.setColor(126, 151, 177); g.drawRoundRect(4, addressY, w - 8, addressH, 4, 4);
            g.setColor(31, 51, 72);
            g.drawString(shorten(currentUrl, w - 18, normal), 9, addressY + 5, Graphics.TOP | Graphics.LEFT);

            int titleY = addressY + addressH + 2;
            g.setColor(214, 226, 238); g.fillRect(0, titleY, w, titleH);
            g.setColor(40, 70, 101); g.setFont(bold);
            g.drawString(shorten(screenTitle, w - 12, normal), 6, titleY + 3, Graphics.TOP | Graphics.LEFT);

            int contentY = titleY + titleH;
            int contentH = h - contentY - footerH;
            g.setColor(255, 255, 255); g.fillRect(0, contentY, w, contentH);
            boolean edgeToEdgeMobile = lastPageResult != null && lastPageResult.mobileTiles;
            g.setClip(edgeToEdgeMobile ? 0 : 4, contentY + 3, edgeToEdgeMobile ? w : w - 8, Math.max(0, contentH - 6));
            int y = contentY + 4 - scrollY;
            int zoomPct = zoom < 50 ? 100 : zoom;
            int mobileImageTop = y;
            float mobileScale = (float)w / Math.max(1, lastPageResult == null ? 240 : lastPageResult.mobileViewportWidth);
            totalBodyHeight = 0;
            if (lastPageResult != null && lastPageResult.images != null && lastPageResult.mobileTiles) {
                for (int imageIndex = 0; imageIndex < lastPageResult.images.size() && (lastPageResult.mobileTiles || imageIndex < 8); imageIndex++) {
                    ImageRef ref = (ImageRef)lastPageResult.images.elementAt(imageIndex);
                    if (ref.image != null) {
                        Image toDraw = ref.image;
                        // Mobile page tiles fill the complete display width; ordinary images stay inset.
                        int targetImageWidth = lastPageResult.mobileTiles ? w : w - 14;
                        if (lastPageResult.mobileTiles) {
                            // Cache scaled mobile tiles: paint() runs on every key/repaint and
                            // per-pixel MIDP scaling on every frame causes severe keypad lag.
                            if (ref.scaledImage == null || ref.scaledWidth != targetImageWidth) {
                                int newH = ref.image.getHeight() * targetImageWidth / ref.image.getWidth();
                                if (newH < 1) newH = 1;
                                ref.scaledImage = targetImageWidth == ref.image.getWidth()
                                    ? ref.image : scaleImage(ref.image, targetImageWidth, newH);
                                ref.scaledWidth = targetImageWidth;
                            }
                            toDraw = ref.scaledImage;
                        } else if (toDraw.getWidth() > targetImageWidth) {
                            int newH = toDraw.getHeight() * targetImageWidth / toDraw.getWidth();
                            if (newH < 1) newH = 1;
                            toDraw = scaleImage(toDraw, targetImageWidth, newH);
                        }
                        if (y + toDraw.getHeight() > contentY && y < contentY + contentH) {
                            g.drawImage(toDraw, lastPageResult.mobileTiles ? 0 : 7, y, Graphics.TOP | Graphics.LEFT);
                        }
                        int imageAdvance = toDraw.getHeight() + (lastPageResult.mobileTiles ? 0 : 4);
                        y += imageAdvance;
                        totalBodyHeight += imageAdvance;
                    }
                }
            }
            // IML2 keeps link hitboxes aligned with the captured page; underline links in blue
            // and outline the focused target directly over the screenshot (no duplicate link list).
            if (lastPageResult != null && lastPageResult.mobileTiles && lastPageResult.links != null) {
                for (int li = 0; li < lastPageResult.links.size(); li++) {
                    LinkItem link = (LinkItem)lastPageResult.links.elementAt(li);
                    if (link.x < 0 || link.y < 0 || link.width <= 0 || link.height <= 0) continue;
                    int lx = (int)(link.x * mobileScale);
                    int ly = mobileLinkY(link.y, mobileImageTop, mobileScale);
                    int lw = Math.max(2, (int)(link.width * mobileScale));
                    int lh = Math.max(2, (int)(link.height * mobileScale));
                    if (ly + lh < contentY || ly > contentY + contentH) continue;
                    g.setColor(0, 82, 190);
                    g.drawLine(lx, ly + lh - 1, lx + lw, ly + lh - 1);
                    if (li == selectedLink) {
                        g.setColor(0, 90, 220);
                        g.drawRect(lx, ly, lw, lh);
                    }
                }
            }
            // OBML-style absolute layout when positions are available
            boolean useAbsolute = false;
            int absCount = 0;
            Vector sourceBlocks = lastPageResult != null && lastPageResult.blocks != null ? lastPageResult.blocks : null;
            if (sourceBlocks != null) {
                for (int b = 0; b < sourceBlocks.size(); b++) {
                    RenderBlock blk = (RenderBlock)sourceBlocks.elementAt(b);
                    if (blk.layoutY > 0 || blk.layoutX > 0) absCount++;
                }
                useAbsolute = absCount > sourceBlocks.size() / 4;
            }
            if (useAbsolute && sourceBlocks != null) {
                int maxY = 0;
                float scale = w / 240.0f;
                if (scale < 0.5f) scale = 0.5f;
                if (scale > 1.5f) scale = 1.5f;
                for (int b = 0; b < sourceBlocks.size(); b++) {
                    RenderBlock blk = (RenderBlock)sourceBlocks.elementAt(b);
                    if (blk.text == null || blk.text.length() == 0) continue;
                    int px = (int)(blk.layoutX * scale);
                    int py = contentY + (int)(blk.layoutY * scale) - scrollY;
                    if (py + 20 < contentY || py > contentY + contentH) continue;
                    Font f = fontForType(blk.type);
                    g.setFont(f);
                    if (blk.type == 22 && blk.url != null && blk.url.length() >= 4 && blk.url.charAt(0) == '#') {
                        try {
                            int rgb = parseCssColor(blk.url, 0x1E2832);
                            g.setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                        } catch (Exception e) { g.setColor(0); }
                    } else if (blk.url != null) {
                        g.setColor(0, 72, 160);
                    } else {
                        g.setColor(30, 40, 50);
                    }
                    String absText = shorten(blk.text, Math.max(1, w - px - 4), f);
                    int absTextWidth = f.stringWidth(absText);
                    int absX = px;
                    int absAnchor = Graphics.LEFT;
                    if (blk.type == 20) {
                        absX = px + Math.max(0, (blk.layoutW > 0 ? (int)(blk.layoutW * scale) : w - px) / 2);
                        absAnchor = Graphics.HCENTER;
                    } else if (blk.type == 21) {
                        absX = px + (blk.layoutW > 0 ? (int)(blk.layoutW * scale) : w - px);
                        absAnchor = Graphics.RIGHT;
                    }
                    int absLeft = absAnchor == Graphics.HCENTER ? absX - absTextWidth / 2 :
                                  (absAnchor == Graphics.RIGHT ? absX - absTextWidth : absX);
                    int absBoxWidth = blk.layoutW > 0 ? (int)(blk.layoutW * scale) : absTextWidth;
                    int absBoxHeight = blk.layoutH > 0 ? (int)(blk.layoutH * scale) : f.getHeight();
                    if (blk.type == 16) {
                        g.setColor(255, 244, 170);
                        g.fillRect(Math.max(0, absLeft), py, Math.min(w - Math.max(0, absLeft), Math.max(absBoxWidth, absTextWidth)), absBoxHeight);
                    }
                    g.drawString(absText, absX, py, Graphics.TOP | absAnchor);
                    if (blk.type == 17 || blk.type == 4) {
                        g.setColor(blk.type == 4 ? 0 : 65, blk.type == 4 ? 72 : 95, blk.type == 4 ? 160 : 125);
                        g.drawLine(absLeft, py + f.getHeight() - 1, absLeft + absTextWidth, py + f.getHeight() - 1);
                    }
                    if (blk.type == 15) {
                        g.setColor(120, 120, 120);
                        g.drawLine(absLeft, py + f.getHeight() / 2, absLeft + absTextWidth, py + f.getHeight() / 2);
                    }
                    // OBML-style: link is a separate hit rectangle
                    if (blk.type == 4 && blk.layoutW > 0 && blk.layoutH > 0) {
                        int lw = (int)(blk.layoutW * scale);
                        int lh = (int)(blk.layoutH * scale);
                        g.setColor(0, 82, 190);
                        g.drawRect(px, py, Math.max(2, lw), Math.max(2, lh));
                    }
                    int bottom = blk.layoutY + (blk.layoutH > 0 ? blk.layoutH : 16);
                    if (bottom > maxY) maxY = bottom;
                }
                totalBodyHeight = (int)(maxY * scale) + 40;
                // Skip normal flow drawing
                g.setColor(255, 255, 255);
                g.fillRect(0, h - footerH, w, footerH);
                g.setColor(20, 40, 70);
                g.setFont(normal);
                g.drawString("Menu", 5, h - footerH + 3, Graphics.TOP | Graphics.LEFT);
                g.drawString("Back", w - 5, h - footerH + 3, Graphics.TOP | Graphics.RIGHT);
                return;
            }
            Vector lines = new Vector();

            if (lastPageResult != null && lastPageResult.mobileTiles) {
                // The screenshot already contains the full page; do not append a fake text/link section.
            } else if (sourceBlocks == null || sourceBlocks.size() == 0) {
                Vector plain = wrap(screenBody, normal, w - 14);
                for (int i = 0; i < plain.size(); i++) lines.addElement(new RenderLine((String)plain.elementAt(i), 0, null));
            } else {
                for (int b = 0; b < sourceBlocks.size(); b++) {
                    RenderBlock block = (RenderBlock)sourceBlocks.elementAt(b);
                    if (block.type == 5) {
                        lines.addElement(new RenderLine(block.text, 5, block.url));
                        continue;
                    }
                    if (block.text.length() == 0) { lines.addElement(new RenderLine("", 0, null)); continue; }
                    Font bf = fontForType(block.type);
                    Vector wrapped = wrap(block.text, bf, w - 14);
                    for (int j = 0; j < wrapped.size(); j++) lines.addElement(new RenderLine((String)wrapped.elementAt(j), block.type, block.url));
                }
            }
            renderedLinkLines.removeAllElements();
            for (int i = 0; i < lines.size(); i++) {
                RenderLine line = (RenderLine)lines.elementAt(i);
                if (line.type == 5) {
                    ImageRef imageRef = findImage(line.text);
                    if (imageRef != null && imageRef.image != null) {
                        Image toDraw = imageRef.image;
                        int maxImageWidth = Math.max(1, w - 14);
                        if (toDraw.getWidth() > maxImageWidth) {
                            int newHeight = Math.max(1, toDraw.getHeight() * maxImageWidth / toDraw.getWidth());
                            if (imageRef.scaledImage == null || imageRef.scaledWidth != maxImageWidth) {
                                imageRef.scaledImage = scaleImage(toDraw, maxImageWidth, newHeight);
                                imageRef.scaledWidth = maxImageWidth;
                            }
                            toDraw = imageRef.scaledImage;
                        }
                        int imageHeight = Math.max(1, toDraw.getHeight());
                        boolean imageFocused = false;
                        if (line.url != null) {
                            renderedLinkLines.addElement(new Integer(i));
                            imageFocused = renderedLinkLines.size() - 1 == selectedLink;
                        }
                        if (y + imageHeight > contentY && y < contentY + contentH) {
                            if (imageFocused) {
                                g.setColor(222, 237, 255);
                                g.fillRect(5, y, w - 10, imageHeight);
                            }
                            g.drawImage(toDraw, 7, y, Graphics.TOP | Graphics.LEFT);
                            if (imageFocused) {
                                g.setColor(0, 90, 220);
                                g.drawRect(6, y, Math.min(w - 12, toDraw.getWidth() + 2), imageHeight);
                            }
                        }
                        y += imageHeight + 4;
                        totalBodyHeight += imageHeight + 4;
                    } else {
                        Font imageFont = normal;
                        g.setFont(imageFont);
                        g.setColor(80, 100, 120);
                        String altText = imageRef == null || imageRef.alt == null || imageRef.alt.length() == 0 ? "[image unavailable]" : imageRef.alt;
                        g.drawString(shorten(altText, w - 14, imageFont), 7, y, Graphics.TOP | Graphics.LEFT);
                        y += imageFont.getHeight() + 3;
                        totalBodyHeight += imageFont.getHeight() + 3;
                    }
                    continue;
                }
                if (line.type == 13) {
                    int ruleY = y + Math.max(2, normal.getHeight() / 2);
                    g.setColor(150, 165, 180);
                    g.drawLine(7, ruleY, w - 7, ruleY);
                    y += normal.getHeight() + 5;
                    totalBodyHeight += normal.getHeight() + 5;
                    continue;
                }
                Font lineFont = fontForType(line.type);
                g.setFont(lineFont);
                int lineHeight = lineFont.getHeight() + (line.type == 1 ? 3 : 1);
                if (line.url != null) renderedLinkLines.addElement(new Integer(i));
                boolean focused = line.url != null && renderedLinkLines.size() - 1 == selectedLink;
                if (focused && y + lineHeight > contentY && y < contentY + contentH) {
                    g.setColor(222, 237, 255); g.fillRect(5, y, w - 10, lineHeight);
                }
                if (line.type == 11 && y + lineHeight > contentY && y < contentY + contentH) {
                    g.setColor(244, 246, 248); g.fillRect(5, y, w - 10, lineHeight);
                }
                if (line.type == 12) {
                    g.setColor(115, 135, 155);
                    g.fillRect(7, y + 1, 2, Math.max(2, lineHeight - 2));
                }
                int tx = line.type == 12 ? 15 : (line.type == 11 ? 10 : 7);
                if (line.type == 16 && y + lineHeight > contentY && y < contentY + contentH) {
                    g.setColor(255, 244, 170); g.fillRect(5, y, w - 10, lineHeight);
                }
                if (line.type == 22 && line.url != null && line.url.length() >= 4 && line.url.charAt(0) == '#') {
                    int rgb = parseCssColor(line.url, 0x1E2832);
                    g.setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
                } else {
                g.setColor(line.url != null ? 0 : (line.type == 1 ? 20 : (line.type == 2 || line.type == 14 ? 30 : (line.type == 12 ? 65 : 35))),
                           line.url != null ? 72 : (line.type == 1 ? 65 : (line.type == 2 || line.type == 14 ? 55 : 43)),
                           line.url != null ? 160 : (line.type == 1 ? 115 : (line.type == 2 || line.type == 14 ? 90 : 52)));
                }
                int textX = tx;
                int textAnchor = Graphics.LEFT;
                if (line.type == 20) {
                    textX = w / 2;
                    textAnchor = Graphics.HCENTER;
                } else if (line.type == 21) {
                    textX = w - 7;
                    textAnchor = Graphics.RIGHT;
                }
                g.drawString(line.text, textX, y, Graphics.TOP | textAnchor);
                int textWidth = lineFont.stringWidth(line.text);
                if (line.url != null || line.type == 17) {
                    g.setColor(line.url != null ? 0 : 65, line.url != null ? 72 : 95, line.url != null ? 160 : 125);
                    int underlineX = line.type == 20 ? textX - textWidth / 2 : (line.type == 21 ? textX - textWidth : tx);
                    g.drawLine(underlineX, y + lineFont.getHeight() - 1, underlineX + textWidth, y + lineFont.getHeight() - 1);
                }
                if (line.type == 15) {
                    int strikeX = line.type == 20 ? textX - textWidth / 2 : (line.type == 21 ? textX - textWidth : tx);
                    g.drawLine(strikeX, y + lineFont.getHeight() / 2, strikeX + textWidth, y + lineFont.getHeight() / 2);
                }
                y += lineHeight;
                totalBodyHeight += lineHeight;
            }
            g.setClip(0, 0, w, h);
            g.setColor(0, 55, 108); g.fillRect(0, h - footerH, w, footerH);
            g.setColor(255, 255, 255); g.setFont(bold);
            g.drawString("Menu", 5, h - footerH + 3, Graphics.TOP | Graphics.LEFT);
            g.drawString("Back", w - 5, h - footerH + 3, Graphics.TOP | Graphics.RIGHT);
            g.setFont(normal);
            if (loading) {
                g.drawString("Loading...", w / 2, h - footerH + 3, Graphics.TOP | Graphics.HCENTER);
            } else if (selectedLink >= 0) {
                int total = linkCount();
                g.drawString((selectedLink + 1) + "/" + total, w / 2, h - footerH + 3, Graphics.TOP | Graphics.HCENTER);
            }
        }

        private ImageRef findImage(String imageUrl) {
            if (lastPageResult == null || lastPageResult.images == null || imageUrl == null) return null;
            for (int i = 0; i < lastPageResult.images.size(); i++) {
                ImageRef ref = (ImageRef)lastPageResult.images.elementAt(i);
                if (imageUrl.equals(ref.url)) return ref;
            }
            return null;
        }

        private Font fontForType(int type) {
            if (type == 1 || type == 2) return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            if (type == 10) return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
            if (type == 11) return Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            if (type == 14) return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
            if (type == 17) return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_UNDERLINED, Font.SIZE_MEDIUM);
            if (type == 18) return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            if (type == 19) return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
            return Font.getDefaultFont();
        }

        /** Parse CSS #RGB or #RRGGBB into a MIDP-compatible RGB integer. */
        private int parseCssColor(String value, int fallback) {
            if (value == null) return fallback;
            String hex = value.trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0)
                          + hex.charAt(1) + hex.charAt(1)
                          + hex.charAt(2) + hex.charAt(2);
            }
            if (hex.length() != 6) return fallback;
            try {
                return Integer.parseInt(hex, 16);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        private Vector wrap(String value, Font font, int maxWidth) {
            Vector out = new Vector();
            int pos = 0;
            while (pos < value.length()) {
                int end = value.indexOf('\n', pos);
                if (end < 0) end = value.length();
                if (end == pos) { out.addElement(""); pos++; continue; }
                char marker = value.charAt(pos) == '\u0001' || value.charAt(pos) == '\u0002' ? value.charAt(pos++) : 0;
                boolean first = true;
                while (pos < end) {
                    int fit = pos, lastSpace = -1;
                    while (fit < end && font.stringWidth(value.substring(pos, fit + 1)) <= maxWidth) {
                        if (value.charAt(fit) == ' ') lastSpace = fit;
                        fit++;
                    }
                    if (fit == pos) fit++;
                    if (fit < end && lastSpace >= pos) fit = lastSpace;
                    String part = value.substring(pos, fit);
                    out.addElement(first && marker != 0 ? String.valueOf(marker) + part : part);
                    first = false;
                    pos = fit;
                    while (pos < end && value.charAt(pos) == ' ') pos++;
                }
                pos = end + 1;
            }
            return out;
        }

        private String shorten(String value, int width, Font font) {
            if (font.stringWidth(value) <= width) return value;
            int end = value.length();
            while (end > 0 && font.stringWidth(value.substring(0, end) + "...") > width) end--;
            return value.substring(0, end) + "...";
        }

        protected void keyPressed(int keyCode) {
            // Opera Mini style zoom
            if (keyCode == KEY_STAR) { zoom = Math.max(50, zoom - 25); repaint(); return; }
            if (keyCode == KEY_POUND) { zoom = Math.min(200, zoom + 25); repaint(); return; }
            int action = getGameAction(keyCode);
            boolean tiles = lastPageResult != null && lastPageResult.mobileTiles;

            // PicoBrowser / picomidp: up/down = previous/next link, left/right = page.
            if (action == UP || keyCode == KEY_NUM2) {
                if (tiles) moveMobileLink(-1); else moveLink(-1);
                return;
            }
            if (action == DOWN || keyCode == KEY_NUM8) {
                if (tiles) moveMobileLink(1); else moveLink(1);
                return;
            }
            if (action == LEFT || keyCode == KEY_NUM4) {
                pageUp();
                return;
            }
            if (action == RIGHT || keyCode == KEY_NUM6) {
                pageDown();
                return;
            }
            if (action == FIRE || keyCode == KEY_NUM5 || keyCode == KEY_NUM0) {
                openSelectedLink();
                return;
            }
            // Fine scroll, picomidp KEY_NUM1 / KEY_NUM3
            if (keyCode == KEY_NUM1) {
                scrollBy(-24);
                return;
            }
            if (keyCode == KEY_NUM3) {
                scrollBy(24);
                return;
            }
        }

        private int mobileLinkY(int imageY, int imageTop, float scale) {
            if (lastPageResult == null || lastPageResult.images == null) return imageTop + (int)(imageY * scale);
            int origin = 0;
            int screen = imageTop;
            for (int i = 0; i < lastPageResult.images.size(); i++) {
                ImageRef ref = (ImageRef)lastPageResult.images.elementAt(i);
                if (ref == null || ref.image == null) continue;
                int srcH = ref.image.getHeight();
                int drawH = ref.scaledImage != null ? ref.scaledImage.getHeight() : Math.max(1, (int)(srcH * scale));
                if (imageY < origin + srcH || i == lastPageResult.images.size() - 1) {
                    int local = imageY - origin;
                    if (local < 0) local = 0;
                    if (srcH < 1) srcH = 1;
                    return screen + local * drawH / srcH;
                }
                origin += srcH;
                screen += drawH;
            }
            return imageTop + (int)(imageY * scale);
        }

        private int linkCount() {
            if (lastPageResult != null && lastPageResult.mobileTiles && lastPageResult.links != null) {
                return lastPageResult.links.size();
            }
            return renderedLinkLines.size();
        }

        private void pageUp() {
            int visible = Math.max(1, getHeight() - 90);
            scrollY = scrollY < visible ? 0 : scrollY - visible;
            repaint();
        }

        private void pageDown() {
            int visible = Math.max(1, getHeight() - 90);
            int maxScroll = Math.max(0, totalBodyHeight - visible);
            scrollY = Math.min(maxScroll, scrollY + visible);
            repaint();
        }

        private void scrollBy(int delta) {
            int visible = Math.max(1, getHeight() - 90);
            int maxScroll = Math.max(0, totalBodyHeight - visible);
            scrollY = Math.max(0, Math.min(maxScroll, scrollY + delta));
            repaint();
        }

        private void resetLinkFocus() {
            selectedLink = -1;
            scrollY = 0;
            repaint();
        }

        private void moveMobileLink(int dir) {
            if (lastPageResult == null || lastPageResult.links == null || lastPageResult.links.size() == 0) {
                scrollY = Math.max(0, Math.min(Math.max(0, totalBodyHeight - (getHeight() - 90)), scrollY + dir * 30));
                repaint();
                return;
            }
            if (selectedLink < 0) selectedLink = dir > 0 ? 0 : lastPageResult.links.size() - 1;
            else {
                selectedLink += dir;
                if (selectedLink < 0) selectedLink = lastPageResult.links.size() - 1;
                if (selectedLink >= lastPageResult.links.size()) selectedLink = 0;
            }
            LinkItem link = (LinkItem)lastPageResult.links.elementAt(selectedLink);
            if (link.y >= 0) {
                float pageScale = (float)getWidth() / Math.max(1, lastPageResult.mobileViewportWidth);
                int top = (int)(link.y * pageScale);
                int bottom = top + Math.max(1, (int)(link.height * pageScale));
                int visible = Math.max(1, getHeight() - 90);
                if (top < scrollY) scrollY = top;
                else if (bottom > scrollY + visible) scrollY = Math.max(0, bottom - visible);
                int maxScroll = Math.max(0, totalBodyHeight - visible);
                if (scrollY > maxScroll) scrollY = maxScroll;
            }
            repaint();
        }

        private void moveLink(int dir) {
            if (renderedLinkLines.size() == 0) return;
            if (selectedLink < 0) selectedLink = 0;
            else {
                selectedLink += dir;
                if (selectedLink < 0) selectedLink = renderedLinkLines.size() - 1;
                if (selectedLink >= renderedLinkLines.size()) selectedLink = 0;
            }
            repaint();
        }

        private void openSelectedLink() {
            if (lastPageResult != null && lastPageResult.mobileTiles && lastPageResult.links != null) {
                if (selectedLink >= 0 && selectedLink < lastPageResult.links.size()) {
                    openTarget(((LinkItem)lastPageResult.links.elementAt(selectedLink)).url, true);
                }
                return;
            }
            if (selectedLink < 0 || lastPageResult == null || lastPageResult.blocks == null) return;
            int seen = -1;
            for (int i = 0; i < lastPageResult.blocks.size(); i++) {
                RenderBlock b = (RenderBlock)lastPageResult.blocks.elementAt(i);
                if (b.url != null && ++seen == selectedLink) {
                    openTarget(b.url, true);
                    return;
                }
            }
        }
    }

    private static final class LinkItem {
        String label;
        String url;
        int x, y, width, height;
        LinkItem(String label, String url) { this(label, url, -1, -1, 0, 0); }
        LinkItem(String label, String url, int x, int y, int width, int height) {
            this.label = label; this.url = url; this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    private static final class ImageRef {
        String alt;
        String url;
        Image image;
        Image scaledImage;
        int scaledWidth = -1;
        ImageRef(String alt, String url) {
            this.alt = alt == null ? "" : alt;
            this.url = url;
        }
    }

    private static final class FormRef {
        String label; String action; String method; Vector fields = new Vector();
        FormRef(String label, String action, String method) { this.label = label; this.action = action; this.method = method; }
    }

    private static final class FormField {
        String label; String type; String name; String value;
        FormField(String label, String type, String name, String value) {
            this.label = label; this.type = type; this.name = name; this.value = value;
        }
    }

    private static final class RenderBlock {
        String text;
        int type;
        String url;
        int layoutX, layoutY, layoutW, layoutH;
        RenderBlock(String text, int type, String url) { this.text = text; this.type = type; this.url = url; }
    }


    private static final class RenderLine {
        String text;
        int type;
        String url;
        RenderLine(String text, int type, String url) { this.text = text; this.type = type; this.url = url; }
    }

    private static final class PageResult {
        String text;
        String title;
        Vector links;
        Vector images;
        Vector blocks;
        Vector forms;
        boolean mobileTiles;
        int mobileViewportWidth = 240;
    }

    private Image scaleImage(Image src, int newW, int newH) {
        if (src == null || newW < 1 || newH < 1) return src;
        // Simple nearest-neighbor scale safe for MIDP
        Image dst = Image.createImage(newW, newH);
        Graphics g = dst.getGraphics();
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        for (int y = 0; y < newH; y++) {
            int sy = (y * srcH) / newH;
            for (int x = 0; x < newW; x++) {
                int sx = (x * srcW) / newW;
                g.setClip(x, y, 1, 1);
                g.drawImage(src, x - sx, y - sy, Graphics.TOP | Graphics.LEFT);
            }
        }
        return dst;
    }


    private PageResult parseIMGL(String url, byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        byte[] magic = new byte[4];
        in.readFully(magic);
        boolean tiled = magic[0] == 'I' && magic[1] == 'M' && magic[2] == 'L' && magic[3] == '2';
        PageResult result = new PageResult();
        result.title = url;
        result.text = "[Mobile-rendered page]";
        result.links = new Vector();
        result.images = new Vector();
        result.blocks = new Vector();
        result.mobileTiles = tiled;
        if (tiled) {
            int tileCount = in.readUnsignedShort();
            if (tileCount < 1 || tileCount > 32) throw new IOException("Bad mobile tile count");
            for (int i = 0; i < tileCount; i++) {
                int jpegLen = in.readInt();
                if (jpegLen < 10 || jpegLen > in.available()) throw new IOException("Bad mobile tile length");
                byte[] jpeg = new byte[jpegLen];
                in.readFully(jpeg);
                Image img = Image.createImage(jpeg, 0, jpeg.length);
                if (i == 0) result.mobileViewportWidth = img.getWidth();
                ImageRef ref = new ImageRef("page", "");
                ref.image = img;
                result.images.addElement(ref);
            }
        } else {
            int jpegLen = in.readInt();
            if (jpegLen < 10 || jpegLen > in.available() - 2) throw new IOException("Bad IMGL");
            byte[] jpeg = new byte[jpegLen];
            in.readFully(jpeg);
            try {
                Image img = Image.createImage(jpeg, 0, jpeg.length);
                ImageRef ref = new ImageRef("page", "");
                ref.image = img;
                result.images.addElement(ref);
            } catch (Exception ignored) {}
        }
        int linkCount = in.readUnsignedShort();
        int keepLinks = linkCount > 200 ? 200 : linkCount;
        for (int i = 0; i < linkCount; i++) {
            int tLen = in.readUnsignedShort();
            byte[] tb = new byte[tLen];
            in.readFully(tb);
            String text = new String(tb, "UTF-8");
            int uLen = in.readUnsignedShort();
            byte[] ub = new byte[uLen];
            in.readFully(ub);
            String link = new String(ub, "UTF-8");
            int linkX = tiled ? in.readUnsignedShort() : -1;
            int linkY = tiled ? in.readUnsignedShort() : -1;
            int linkW = tiled ? in.readUnsignedShort() : 0;
            int linkH = tiled ? in.readUnsignedShort() : 0;
            if (i < keepLinks && text.length() > 0 && link.length() > 0) {
                result.links.addElement(new LinkItem(text, link, linkX, linkY, linkW, linkH));
                if (!tiled) result.blocks.addElement(new RenderBlock(text, 4, link));
            }
        }
        return result;
    }

}
