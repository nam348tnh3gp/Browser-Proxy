package tgfn.browsermini;

import java.io.ByteArrayOutputStream;
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
    private List linkList;
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
        showHome();
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    private void showHome() {
        page = new Form("TGFN BrowserMini");
        page.append(new StringItem("Lightweight J2ME browser", "For Java ME feature phones\n\nEnter a web address or search query. This browser shows readable text and basic links; complex modern sites may not work."));
        page.append(new StringItem("Address", currentUrl));
        page.append(new StringItem("Proxy", proxyEnabled ? "ON: " + proxyHost + ":" + proxyPort + " (HTTP only)" : "OFF"));
        page.addCommand(openCommand);
        page.addCommand(addressCommand);
        page.addCommand(homeCommand);
        page.addCommand(proxyCommand);
        page.addCommand(bookmarksCommand);
        page.addCommand(exitCommand);
        page.setCommandListener(this);
        display.setCurrent(page);
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
        if (proxyEnabled && url.startsWith("http://")) return fetchViaProxy(url);
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
                                result.links.addElement(new LinkItem(label, absolute));
                                text.append("\n[").append(result.links.size()).append("] ").append(label).append("\n");
                            }
                        }
                    }
                }
                if (tagLower.startsWith("br") || tagLower.startsWith("/p") ||
                    tagLower.startsWith("/div") || tagLower.startsWith("/li") ||
                    tagLower.startsWith("p") || tagLower.startsWith("h1") ||
                    tagLower.startsWith("h2") || tagLower.startsWith("tr")) text.append('\n');
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
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&') {
                int semi = input.indexOf(';', i);
                if (semi > i && semi - i < 10) {
                    String entity = input.substring(i + 1, semi);
                    String decoded = decodeEntity(entity);
                    if (decoded != null) {
                        out.append(decoded);
                        i = semi;
                        space = false;
                        continue;
                    }
                }
            }
            if (c == '\r' || c == '\n' || c == '\t' || c == ' ') {
                if (!space && out.length() > 0) out.append(' ');
                space = true;
            } else {
                out.append(c);
                space = false;
            }
        }
        return trim(out.toString());
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
        page = new Form(title.length() > 0 ? title : "Web page");
        page.append(new StringItem("Address", currentUrl));
        if (proxyEnabled && currentUrl.startsWith("https://")) {
            page.append(new StringItem("Proxy", "HTTPS uses the phone's direct secure connection; Java ME cannot safely layer TLS over this HTTP proxy."));
        } else if (proxyEnabled) {
            page.append(new StringItem("Proxy", "HTTP proxy: " + proxyHost + ":" + proxyPort));
        }
        page.append(new StringItem("", body));
        pageImageItems.removeAllElements();
        PageResult displayResult = lastPageResult;
        if (displayResult != null) {
            for (int imageIndex = 0; imageIndex < displayResult.images.size(); imageIndex++) {
                ImageRef imageRef = (ImageRef)displayResult.images.elementAt(imageIndex);
                if (imageRef.image != null) {
                    ImageItem imageItem = new ImageItem(imageRef.alt.length() > 0 ? imageRef.alt : "Image", imageRef.image, ImageItem.LAYOUT_CENTER, imageRef.alt);
                    pageImageItems.addElement(imageItem);
                    page.append(imageItem);
                }
            }
        }
        pageLinkItems.removeAllElements();
        for (int i = 0; i < links.size(); i++) {
            LinkItem link = (LinkItem)links.elementAt(i);
            String label = link.label;
            if (label.length() > 70) label = label.substring(0, 67) + "...";
            StringItem linkItem = new StringItem("Link", label, Item.HYPERLINK);
            linkItem.setDefaultCommand(openLinkCommand);
            linkItem.setItemCommandListener(this);
            pageLinkItems.addElement(linkItem);
            page.append(linkItem);
        }
        page.addCommand(backCommand);
        page.addCommand(forwardCommand);
        page.addCommand(homeCommand);
        page.addCommand(linksCommand);
        page.addCommand(addressCommand);
        page.addCommand(reloadCommand);
        page.addCommand(bookmarkCommand);
        page.addCommand(proxyCommand);
        page.addCommand(bookmarksCommand);
        page.addCommand(exitCommand);
        page.setCommandListener(this);
        display.setCurrent(page);
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
        if (command == exitCommand) {
            notifyDestroyed();
        } else if (source == addressBox) {
            if (command == openCommand) {
                String url = normalizeAddress(addressBox.getString());
                if (url.length() == 0) showError("Invalid address", "Enter a valid HTTP/HTTPS URL or a search query.");
                else navigate(url, true);
            } else {
                if (page != null) display.setCurrent(page); else showHome();
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
                if (page != null) display.setCurrent(page);
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
            if (source == linkList && page != null) display.setCurrent(page);
            else if (page != null) display.setCurrent(page);
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
        proxyEnabledChoice = new ChoiceGroup("Use HTTP proxy", ChoiceGroup.MULTIPLE);
        proxyEnabledChoice.append("Enable proxy", null);
        proxyEnabledChoice.setSelectedIndex(0, proxyEnabled);
        proxyForm.append(new StringItem("Note", "HTTP pages use the configured forward proxy. HTTPS stays direct because standard MIDP cannot safely layer TLS over an HTTP CONNECT socket."));
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
        return parsePage(url, new String(fetchProxyBytes(url, MAX_PAGE_CHARS * 3), "UTF-8"));
    }

    private byte[] fetchProxyBytes(String url, int maxBytes) throws IOException {
        if (!url.startsWith("http://")) throw new IOException("The HTTP proxy only accepts HTTP resources");
        String rest = url.substring("http://".length());
        int slash = rest.indexOf('/');
        String hostPort = slash < 0 ? rest : rest.substring(0, slash);
        String path = slash < 0 ? "/" : rest.substring(slash);
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
            writer.write("GET " + url + " HTTP/1.1\r\n");
            writer.write("Host: " + hostPort + "\r\n");
            writer.write("User-Agent: TGFN-BrowserMini-J2ME/1.0\r\n");
            writer.write("Accept: text/html,text/plain,*/*\r\n");
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
        if (proxyEnabled && url.startsWith("http://")) {
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

    private static final class PageResult {
        String text;
        String title;
        Vector links;
        Vector images;
    }
}
