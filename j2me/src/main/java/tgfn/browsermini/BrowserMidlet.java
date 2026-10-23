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
                    for (int imageIndex = 0; imageIndex < result.images.size() && imageIndex < 8; imageIndex++) {
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
        if (source == browserCanvas && command == menuCommand) {
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
                    navigate(((LinkItem)links.elementAt(selected)).url, true);
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
                navigate(((LinkItem)links.elementAt(index)).url, true);
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
        byte[] response = fetchProxyBytes(url, MAX_PAGE_CHARS * 3);
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
        result.blocks = new Vector();
        result.title = readTBOString(in);
        int count = in.readUnsignedShort();
        if (count > 1200) throw new IOException("Too many compact page blocks");
        StringBuffer text = new StringBuffer();
        for (int i = 0; i < count; i++) {
            int kind = in.readUnsignedByte();
            String label = readTBOString(in);
            String target = readTBOString(in);
            if (kind == 6) {
                result.blocks.addElement(new RenderBlock("", 0, null));
                text.append('\n');
                continue;
            }
            if (kind == 5 && target.length() > 0) {
                result.images.addElement(new ImageRef(label, target));
                if (label.length() > 0) result.blocks.addElement(new RenderBlock(label, 0, null));
                continue;
            }
            if (label.length() == 0) continue;
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

    private String readTBOString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();
        if (length > in.available()) throw new IOException("Truncated compact page string");
        byte[] data = new byte[length];
        in.readFully(data);
        return new String(data, "UTF-8");
    }

    private byte[] fetchProxyBytes(String url, int maxBytes) throws IOException {
        boolean remoteFetch = url.startsWith("https://");
        if (!remoteFetch && !url.startsWith("http://")) throw new IOException("Only HTTP(S) resources are supported");
        String rest = url.substring(remoteFetch ? "https://".length() : "http://".length());
        int slash = rest.length();
        int slashAt = rest.indexOf('/');
        int queryAt = rest.indexOf('?');
        int fragmentAt = rest.indexOf('#');
        if (slashAt >= 0 && slashAt < slash) slash = slashAt;
        if (queryAt >= 0 && queryAt < slash) slash = queryAt;
        if (fragmentAt >= 0 && fragmentAt < slash) slash = fragmentAt;
        String hostPort = rest.substring(0, slash);
        String path = slash == rest.length() ? "/" : rest.substring(slash);
        if (path.startsWith("?")) path = "/" + path;
        if (path.startsWith("#")) path = "/";
        if (hostPort.indexOf('@') >= 0) throw new IOException("URL userinfo is not allowed");
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
            String requestTarget = remoteFetch
                ? "/__tgfn/fetch?url=" + encodeURLComponent(url)
                : url;
            String requestHost = remoteFetch ? proxyHost + ":" + proxyPort : hostPort;
            writer.write("GET " + requestTarget + " HTTP/1.1\r\n");
            writer.write("Host: " + requestHost + "\r\n");
            writer.write("User-Agent: TGFN-BrowserMini-J2ME/2.0\r\n");
            writer.write("Accept: application/x-tgfn-obml,text/html,text/plain,image/*,*/*\r\n");
            writer.write("X-TGFN-Format: SSR\r\n");
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
                        int b = input.read();
                        if (b < 0) break;
                        bytes.write(b);
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
        if (proxyEnabled && (url.startsWith("http://") || url.startsWith("https://"))) {
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
            totalBodyHeight = 0;
            if (lastPageResult != null && lastPageResult.images != null) {
                for (int imageIndex = 0; imageIndex < lastPageResult.images.size() && imageIndex < 8; imageIndex++) {
                    ImageRef ref = (ImageRef)lastPageResult.images.elementAt(imageIndex);
                    if (ref.image != null && ref.image.getWidth() <= w - 14 && ref.image.getHeight() <= 64) {
                        if (y + ref.image.getHeight() > contentY && y < contentY + contentH) {
                            g.drawImage(ref.image, 7, y, Graphics.TOP | Graphics.LEFT);
                        }
                        y += ref.image.getHeight() + 4;
                        totalBodyHeight += ref.image.getHeight() + 4;
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
                            navigate(b.url, true);
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
    }

    private static final class LinkItem {
        String label;
        String url;
        LinkItem(String label, String url) { this.label = label; this.url = url; }
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
    }
}
