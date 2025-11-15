package net.minecraft.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EULA {

    private static final Logger a = LogManager.getLogger();
    private final File b;
    private final boolean c;

    public EULA(File file) {
        this.b = file;
        this.c = this.a(file);
    }

    private boolean a(File file) {
        FileInputStream fileinputstream = null;
        boolean flag = true;   // ⭐ 默认值改为 TRUE

        try {
            Properties properties = new Properties();

            fileinputstream = new FileInputStream(file);
            properties.load(fileinputstream);
            flag = Boolean.parseBoolean(properties.getProperty("eula", "true")); // ⭐ 默认 true

        } catch (Exception exception) {
            EULA.a.warn("Failed to load {}", file);
            this.b(); // ⭐ 写入默认的 true
        } finally {
            IOUtils.closeQuietly(fileinputstream);
        }

        return flag;
    }

    public boolean a() {
        return this.c;
    }

    public void b() {
        FileOutputStream fileoutputstream = null;

        try {
            Properties properties = new Properties();

            fileoutputstream = new FileOutputStream(this.b);
            properties.setProperty("eula", "true");  // ⭐ 默认写入 TRUE
            properties.store(fileoutputstream,
                "By setting this to TRUE you are agreeing to the EULA.");
        } catch (Exception exception) {
            EULA.a.warn("Failed to save {}", this.b, exception);
        } finally {
            IOUtils.closeQuietly(fileoutputstream);
        }

    }
}
