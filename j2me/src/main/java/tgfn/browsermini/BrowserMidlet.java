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
    private Form webForm;
    private Vector activeFormFields;
    private Vector activeFormNames;
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
                    for (int imageIndex = 0; imageIndex < result.images.size() && imageIndex < 8; imageIndex++) {
                        ImageRef imageRef = (ImageRef)result.images.elementAt(imageIndex);
                        if (imageRef.image == null && imageRef.url != null && imageRef.url.length() > 0) {
                            try { imageRef.image = loadImage(imageRef.url); } catch (Exception ignoredImage) {}
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
                    if (imageUrl.length() > 0 && result.images.size() < 8) {
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
            if (t.startsWith("h1")) style = 1;
            else if (t.startsWith("h2") || t.startsWith("h3") || t.startsWith("strong") || t.startsWith("b")) style = 2;
            else if (t.startsWith("li")) { blocks.addElement(new RenderBlock("• ", 3, null)); style = 3; }
            else if (t.startsWith("p") || t.startsWith("div") || t.startsWith("br") || t.startsWith("hr") ||
                     t.startsWith("/p") || t.startsWith("/div") || t.startsWith("/li") ||
                     t.startsWith("/h1") || t.startsWith("/h2") || t.startsWith("/h3") ||
                     t.startsWith("/tr") || t.startsWith("tr")) {
                if (blocks.size() > 0) blocks.addElement(new RenderBlock("", 0, null));
                style = 0;
            } else if (t.startsWith("/strong") || t.startsWith("/b")) style = 0;
            else if (t.startsWith("/h1") || t.startsWith("/h2") || t.startsWith("/h3")) style = 0;
            pos = close + 1;
        }
        return blocks;
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

    private void showBrowserMenu() {
        browserMenu = new List("Browser menu", List.IMPLICIT);
        browserMenu.append("Open address / search", null);
        browserMenu.append("Page links", null);
        browserMenu.append("Bookmarks", null);
        browserMenu.append("Add bookmark", null);
        browserMenu.append("Reload page", null);
        browserMenu.append("Home", null);
        browserMenu.append("Forward", null);
        browserMenu.append("Proxy settings", null);
        browserMenu.append("Exit", null);
        browserMenu.addCommand(backCommand);
        browserMenu.setCommandListener(this);
        display.setCurrent(browserMenu);
    }

    private void handleBrowserMenu(Command command) {
        if (command == backCommand) { display.setCurrent(browserCanvas); return; }
        if (command != List.SELECT_COMMAND) return;
        int selected = browserMenu.getSelectedIndex();
        if (selected == 0) showAddress();
        else if (selected == 1) showLinks();
        else if (selected == 2) showBookmarks();
        else if (selected == 3) addBookmark(currentUrl);
        else if (selected == 4) navigate(currentUrl, false);
        else if (selected == 5) navigate("https://lite.duckduckgo.com/lite/", true);
        else if (selected == 6) {
            if (historyIndex >= 0 && historyIndex + 1 < history.size()) {
                historyIndex++; navigate((String)history.elementAt(historyIndex), false);
            } else showError("Forward", "There is no next page in history.");
        } else if (selected == 7) showProxySettings();
        else if (selected == 8) notifyDestroyed();
    }

    private void showLinks() {
        if (links.size() == 0) {
            showError("No links", "This page did not expose any simple HTML links.");
            return;
        }
        showingBookmarks = false;
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
                saveSettings();
                showHome();
            } else {
                showHome();
            }
        } else if (source == linkList) {
            if (command == List.SELECT_COMMAND) {
                int selected = linkList.getSelectedIndex();
                if (selected >= 0 && showingBookmarks && selected < bookmarks.size()) {
                    navigate((String)bookmarks.elementAt(selected), true);
                } else if (selected >= 0 && !showingBookmarks && selected < links.size()) {
                    openTarget(((LinkItem)links.elementAt(selected)).url, true);
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
            navigate("https://lite.duckduckgo.com/lite/", true);
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
        List list = new List("Bookmarks", List.IMPLICIT);
        for (int i = 0; i < bookmarks.size(); i++) list.append((String)bookmarks.elementAt(i), null);
        list.addCommand(backCommand);
        list.addCommand(addressCommand);
        list.setCommandListener(this);
        display.setCurrent(list);
        linkList = list;
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
                        proxyHost = header.substring(first + 1, second);
                        proxyPort = header.substring(second + 1);
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
            saved.append(proxyEnabled ? "1" : "0").append('|').append(proxyHost).append('|').append(proxyPort).append('\n');
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
        if (count > 1100) throw new IOException("Too many compact page blocks");
        StringBuffer text = new StringBuffer();
        boolean joinNextTableCell = false;
        for (int i = 0; i < count; i++) {
            int kind = in.readUnsignedByte();
            String label = readTBOString(in);
            String target = readTBOString(in);
            if (kind == 7) {
                FormRef formRef = parseFormPayload(label);
                if (formRef != null) {
                    int formIndex = result.forms.size();
                    result.forms.addElement(formRef);
                    String formLabel = "[Form: " + formRef.label + "]";
                    String formTarget = "form:" + formIndex;
                    result.links.addElement(new LinkItem(formLabel, formTarget));
                    result.blocks.addElement(new RenderBlock(formLabel, 4, formTarget));
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
                result.images.addElement(new ImageRef(label, target));
                if (label.length() > 0) result.blocks.addElement(new RenderBlock(label, 0, null));
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
            if (kind == 4 && target.length() > 0) {
                result.links.addElement(new LinkItem(label, target));
                result.blocks.addElement(new RenderBlock(label, 4, target));
            } else {
                int style = kind >= 0 && kind <= 3 ? kind : 0;
                result.blocks.addElement(new RenderBlock(label, style, null));
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
            int constraints = TextField.ANY;
            if ("password".equals(field.type)) constraints = TextField.PASSWORD;
            else if ("number".equals(field.type) || "tel".equals(field.type)) constraints = TextField.NUMERIC;
            else if ("url".equals(field.type)) constraints = TextField.URL;
            else if ("email".equals(field.type)) constraints = TextField.EMAILADDR;
            TextField input = new TextField(field.label.length() == 0 ? field.name : field.label,
                field.value, 512, constraints);
            webForm.append(input);
            activeFormFields.addElement(input);
            activeFormNames.addElement(field.name);
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
            TextField field = (TextField)activeFormFields.elementAt(i);
            String name = (String)activeFormNames.elementAt(i);
            try {
                target.append(first ? '?' : '&');
                first = false;
                target.append(encodeURLComponent(name));
                target.append('=');
                target.append(encodeURLComponent(field.getString()));
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
            writer.write("User-Agent: TGFN-BrowserMini-J2ME/2.1\r\n");
            writer.write("Accept: application/x-tgfn-obml,image/jpeg,image/*,text/html,text/plain,*/*\r\n");
            if (!internalResource) writer.write("X-TGFN-Format: MOB1\r\n");
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
            g.setClip(4, contentY + 3, w - 8, Math.max(0, contentH - 6));
            int y = contentY + 4 - scrollY;
            int mobileImageTop = y;
            float mobileScale = (float)(w - 14) / 240.0f;
            totalBodyHeight = 0;
            if (lastPageResult != null && lastPageResult.images != null) {
                for (int imageIndex = 0; imageIndex < lastPageResult.images.size() && imageIndex < 8; imageIndex++) {
                    ImageRef ref = (ImageRef)lastPageResult.images.elementAt(imageIndex);
                    if (ref.image != null) {
                        Image toDraw = ref.image;
                        // Scale down if wider than screen
                        if (toDraw.getWidth() > w - 14) {
                            int newH = toDraw.getHeight() * (w - 14) / toDraw.getWidth();
                            if (newH < 1) newH = 1;
                            toDraw = scaleImage(toDraw, w - 14, newH);
                        }
                        if (y + toDraw.getHeight() > contentY && y < contentY + contentH) {
                            g.drawImage(toDraw, 7, y, Graphics.TOP | Graphics.LEFT);
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
                    int lx = 7 + (int)(link.x * mobileScale);
                    int ly = mobileImageTop + (int)(link.y * mobileScale) - scrollY;
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
            Vector lines = new Vector();
            Vector sourceBlocks = lastPageResult != null && lastPageResult.blocks != null ? lastPageResult.blocks : null;
            if (sourceBlocks == null || sourceBlocks.size() == 0) {
                Vector plain = wrap(screenBody, normal, w - 14);
                for (int i = 0; i < plain.size(); i++) lines.addElement(new RenderLine((String)plain.elementAt(i), 0, null));
            } else {
                for (int b = 0; b < sourceBlocks.size(); b++) {
                    RenderBlock block = (RenderBlock)sourceBlocks.elementAt(b);
                    if (block.text.length() == 0) { lines.addElement(new RenderLine("", 0, null)); continue; }
                    Font bf = block.type == 1 ? Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM) :
                              (block.type == 2 ? bold : normal);
                    Vector wrapped = wrap(block.text, bf, w - 14);
                    for (int j = 0; j < wrapped.size(); j++) lines.addElement(new RenderLine((String)wrapped.elementAt(j), block.type, block.url));
                }
            }
            renderedLinkLines.removeAllElements();
            for (int i = 0; i < lines.size(); i++) {
                RenderLine line = (RenderLine)lines.elementAt(i);
                Font lineFont = line.type == 1 ? Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM) :
                                (line.type == 2 ? bold : normal);
                g.setFont(lineFont);
                int lineHeight = lineFont.getHeight() + (line.type == 1 ? 3 : 1);
                if (line.url != null) renderedLinkLines.addElement(new Integer(i));
                boolean focused = line.url != null && renderedLinkLines.size() - 1 == selectedLink;
                if (focused && y + lineHeight > contentY && y < contentY + contentH) {
                    g.setColor(222, 237, 255); g.fillRect(5, y, w - 10, lineHeight);
                }
                g.setColor(line.url != null ? 0 : (line.type == 1 ? 20 : (line.type == 2 ? 30 : 35)),
                           line.url != null ? 72 : (line.type == 1 ? 65 : (line.type == 2 ? 55 : 43)),
                           line.url != null ? 160 : (line.type == 1 ? 115 : (line.type == 2 ? 90 : 52)));
                g.drawString(line.text, 7, y, Graphics.TOP | Graphics.LEFT);
                if (line.url != null) g.drawLine(7, y + lineFont.getHeight() - 1, 7 + lineFont.stringWidth(line.text), y + lineFont.getHeight() - 1);
                y += lineHeight;
                totalBodyHeight += lineHeight;
            }
            g.setClip(0, 0, w, h);
            g.setColor(0, 55, 108); g.fillRect(0, h - footerH, w, footerH);
            g.setColor(255, 255, 255); g.setFont(bold);
            g.drawString("Menu", 5, h - footerH + 3, Graphics.TOP | Graphics.LEFT);
            g.drawString("Back", w - 5, h - footerH + 3, Graphics.TOP | Graphics.RIGHT);
            if (loading) {
                g.setFont(normal);
                g.drawString("Loading...", w / 2, h - footerH + 3, Graphics.TOP | Graphics.HCENTER);
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
            int action = getGameAction(keyCode);
            if (lastPageResult != null && lastPageResult.mobileTiles &&
                (action == UP || keyCode == KEY_NUM2)) {
                moveMobileLink(-1);
                return;
            }
            if (lastPageResult != null && lastPageResult.mobileTiles &&
                (action == DOWN || keyCode == KEY_NUM8)) {
                moveMobileLink(1);
                return;
            }
            // PicoBrowser-style quick navigation
            if (keyCode == Canvas.KEY_NUM5 || keyCode == Canvas.KEY_NUM8) { // next link
                moveLink(1);
                return;
            }
            if (keyCode == Canvas.KEY_NUM2 || keyCode == Canvas.KEY_NUM4) { // prev link
                moveLink(-1);
                return;
            }
            if (keyCode == Canvas.KEY_NUM0 || keyCode == Canvas.FIRE) { // open selected
                openSelectedLink();
                return;
            }

            if (action == UP || keyCode == KEY_NUM2) {
                scrollY = Math.max(0, scrollY - 30);
                repaint();
            } else if (action == DOWN || keyCode == KEY_NUM8) {
                int visible = Math.max(1, getHeight() - 90);
                scrollY = Math.min(Math.max(0, totalBodyHeight - visible), scrollY + 30);
                repaint();
            } else if (action == FIRE || keyCode == KEY_NUM5) {
                if (selectedLink >= 0 && lastPageResult != null && lastPageResult.blocks != null) {
                    int seen = -1;
                    for (int i = 0; i < lastPageResult.blocks.size(); i++) {
                        RenderBlock b = (RenderBlock)lastPageResult.blocks.elementAt(i);
                        if (b.url != null && ++seen == selectedLink) {
                            openTarget(b.url, true);
                            return;
                        }
                    }
                    showAddress();
                } else if (renderedLinkLines.size() > 0) {
                    selectedLink = 0;
                    int lineIndex = ((Integer)renderedLinkLines.elementAt(0)).intValue();
                    scrollY = Math.max(0, lineIndex * 14);
                    repaint();
                } else {
                    showAddress();
                }
            } else if (keyCode == KEY_NUM4) {
                if (renderedLinkLines.size() > 0) {
                    selectedLink = selectedLink <= 0 ? renderedLinkLines.size() - 1 : selectedLink - 1;
                    int lineIndex = ((Integer)renderedLinkLines.elementAt(selectedLink)).intValue();
                    scrollY = Math.max(0, lineIndex * 14);
                    repaint();
                } else if (historyIndex > 0) {
                    historyIndex--;
                    navigate((String)history.elementAt(historyIndex), false);
                }
            } else if (keyCode == KEY_NUM6) {
                if (renderedLinkLines.size() > 0) {
                    selectedLink = (selectedLink + 1) % renderedLinkLines.size();
                    int lineIndex = ((Integer)renderedLinkLines.elementAt(selectedLink)).intValue();
                    scrollY = Math.max(0, lineIndex * 14);
                    repaint();
                }
            } else if (action == LEFT && historyIndex > 0) {
                historyIndex--;
                navigate((String)history.elementAt(historyIndex), false);
            } else if (action == RIGHT && historyIndex >= 0 && historyIndex + 1 < history.size()) {
                historyIndex++;
                navigate((String)history.elementAt(historyIndex), false);
            }
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
                int top = (int)(link.y * ((float)(getWidth() - 14) / 240.0f));
                int bottom = top + Math.max(1, (int)(link.height * ((float)(getWidth() - 14) / 240.0f)));
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
        if (linkCount > 100) throw new IOException("Too many image links");
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
            if (text.length() > 0 && link.length() > 0) {
                result.links.addElement(new LinkItem(text, link, linkX, linkY, linkW, linkH));
                if (!tiled) result.blocks.addElement(new RenderBlock(text, 4, link));
            }
        }
        return result;
    }

}
