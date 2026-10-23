package tgfn.browsermini;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

/**
 * Small MIDP 2.0 browser for Java ME feature phones.
 * It intentionally renders readable text and links instead of attempting a full HTML engine.
 */
public final class BrowserMidlet extends MIDlet implements CommandListener {
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
        page.addCommand(openCommand);
        page.addCommand(addressCommand);
        page.addCommand(homeCommand);
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
        return s.replace('&amp;', '&').replace('&quot;', '"').replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">");
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
        page.append(new StringItem("URL", currentUrl));
        page.append(new StringItem("", body));
        page.addCommand(backCommand);
        page.addCommand(forwardCommand);
        page.addCommand(homeCommand);
        page.addCommand(linksCommand);
        page.addCommand(addressCommand);
        page.addCommand(reloadCommand);
        page.addCommand(exitCommand);
        page.setCommandListener(this);
        display.setCurrent(page);
    }

    private void showLinks() {
        if (links.size() == 0) {
            showError("No links", "This page did not expose any simple HTML links.");
            return;
        }
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
        } else if (source == linkList) {
            if (command == List.SELECT_COMMAND) {
                int selected = linkList.getSelectedIndex();
                if (selected >= 0 && selected < links.size()) navigate(((LinkItem)links.elementAt(selected)).url, true);
            } else if (command == backCommand) {
                if (page != null) display.setCurrent(page);
            } else if (command == addressCommand) showAddress();
        } else if (command == openCommand || command == addressCommand) {
            showAddress();
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

    private static final class LinkItem {
        String label;
        String url;
        LinkItem(String label, String url) { this.label = label; this.url = url; }
    }

    private static final class PageResult {
        String text;
        String title;
        Vector links;
    }
}
