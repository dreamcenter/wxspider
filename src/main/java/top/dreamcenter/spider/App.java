package top.dreamcenter.spider;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


public class App {
    private static final String ROOT = "img";
    private static SimpleDateFormat sdf;
    private static JTextArea textArea;
    private static Pattern filePattern;
    private static String driverPath;
    private static Thread thread;
    private static Queue<String> queue;

    static {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("setting.properties"));

            String driver = properties.getProperty("driverPath");
            String replacePattern = properties.getProperty("replacePattern", "[\\\\/:*?\"<>|｜]");
            String prefixFormat = properties.getProperty("prefixFormat", "yyyy_MM_dd_HHmmss");

            driverPath = driver;
            filePattern = Pattern.compile(replacePattern);
            sdf =  new SimpleDateFormat(prefixFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        System.out.println(driverPath);
        System.setProperty("webdriver.chrome.driver", driverPath);

        JFrame frame = new JFrame("微信公众号图片下载助手");
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.setSize(400, 300);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BorderLayout());

        JTextField input = new JTextField();
        queue = new LinkedList<>();
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    String articleLink = input.getText();
                    input.setText("");
                    joinAndRun(articleLink);
                }
            }
        });

        JButton button = new JButton("智能粘贴");
        button.setBackground(Color.white);
        button.addActionListener(e -> {
            String clipboardData = getClipboardData();
            joinAndRun(clipboardData);
        });

        southPanel.add(input, BorderLayout.CENTER);
        southPanel.add(button, BorderLayout.EAST);


        textArea = new JTextArea();
        textArea.setEditable(false);
        DefaultCaret caret = (DefaultCaret)textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);


        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(southPanel, BorderLayout.SOUTH);

        input.requestFocus();

        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private static void joinAndRun(String url) {
        queue.add(url);
        append("⇨ 新入队列，队列排队中: " + queue.size());

        // 有新加入的时候，如果线程正在跑，则不管，否则新建线程获取数据
        if (thread == null || !thread.isAlive()) {
            task();
        }
    }

    private static String getClipboardData() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        try {
            // 检查剪贴板是否包含字符串类型的数据
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                // 获取剪贴板中的文本数据
                String result = (String) clipboard.getData(DataFlavor.stringFlavor);
                System.out.println("剪贴板中的文本内容是：" + result);
                return result;
            } else {
                System.out.println("剪贴板中没有文本数据。");
                return null;
            }
        } catch (UnsupportedFlavorException | IOException e) {
            System.err.println("无法访问剪贴板中的数据：" + e.getMessage());
            return null;
        }

    }

    static void task() {
        thread = new Thread(() -> {
            while (!queue.isEmpty()) {
                String link = queue.poll();
                if (link != null) {
                    append("▼ 开始任务:" + link);
                    try {
                        startOneDownload(link);
                    } catch (Exception ex) {
                        append("▷ [EXCEPTION] " + ex.getMessage());
                    }
                    append("▲ 队列剩余: " + queue.size() + "\n");
                }
            }
        });
        thread.start();
    }

    static void append(String content) {
        textArea.append(content + "\n");
    }

    private static void startOneDownload(String articleLink) {
        append("▷ 导航至:" + articleLink);

        // 无头模式要设置user-agent，否则会被识别到
        ChromeOptions options=new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--headless");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        driver.navigate().to(articleLink);

        try {
            TimeUnit.SECONDS.sleep(5);

        } catch (InterruptedException e) {
            throw new RuntimeException("FAIL SLEEP");
        }

        // 查找标题
        boolean isImgDoc = false;
        // 类别1，普通文档流

        WebElement titleEl;

        try {
            titleEl = driver.findElement(By.id("activity-name"));
        } catch (NoSuchElementException e) {
            titleEl = driver.findElement(By.className("rich_media_title"));
            isImgDoc = true;
        }

        // 类别2，图片流

//        titleEl = driver.findElement(By.id("activity-name"));
        String title = titleEl.getText();

        System.out.println(title);


        File file = Paths.get(ROOT, sdf.format(Calendar.getInstance().getTime()) + "__" + filePattern.matcher(title).replaceAll("_")).toFile();
        if (!file.exists()) {
            file.mkdirs();
        }

        // 获取图片资源
        int total = 0;
        List<String> links = new ArrayList<>();

        // 类别1，普通文档流
        if (!isImgDoc) {
            List<WebElement> img = driver.findElements(By.tagName("img"));
            for (WebElement item : img) {
                String result = item.getAttribute("data-src");

                if (result != null) {
                    total++;
                    links.add(result);
                }
            }
        }

        // 类别2，图片阅览流
        else {
            List<WebElement> img = driver.findElements(By.cssSelector(".swiper_item_img>img"));
            for (WebElement item : img) {
                String result = item.getAttribute("src");

                if (result != null) {
                    total++;
                    links.add(result);
                }
            }
        }


        append("▷ 找到图片:" + total +" 张");

        driver.quit();

        // 开始下载
        int success = 0;
        for (int i = 0; i < total; i++) {
            String link = links.get(i);
            boolean res = download(file.getPath(),(1000 + i) + ".jpg", link);
            if (res) success++;
        }


        append("▷ 成功:" + success + " / " + total + "");
    }

    private static boolean download(String basePath, String fileName, String link) {
        System.out.println(basePath);
        System.out.println("下载:" + link);
        try (FileOutputStream os = new FileOutputStream(Paths.get(basePath, fileName).toString())){

            URL url = new URI(link).toURL();
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            InputStream is = conn.getInputStream();

            byte[] tmp = new byte[1024];
            int len;

            while ((len = is.read(tmp)) != -1) {
                os.write(tmp, 0, len);
            }

            is.close();
            return true;
        } catch (Exception e) {
            System.out.println("下载失败" + e.getMessage());
            return false;
        }

    }

}
