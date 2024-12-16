package keystrokesmod.script;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

public class ScriptManager {
    private Minecraft mc = Minecraft.getMinecraft();
    public HashMap<Script, Module> scripts = new LinkedHashMap<>();
    public JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    public boolean deleteTempFiles = true;
    public File directory;
    public List<String> imports = Arrays.asList(Color.class.getName(), Collections.class.getName(), List.class.getName(), ArrayList.class.getName(), Arrays.class.getName(), Map.class.getName(), HashMap.class.getName(), HashSet.class.getName(), ConcurrentHashMap.class.getName(), LinkedHashMap.class.getName(), Iterator.class.getName(), Comparator.class.getName(), AtomicInteger.class.getName(), AtomicLong.class.getName(), AtomicBoolean.class.getName(), Random.class.getName(), Matcher.class.getName());
    public String tempDir = System.getProperty("java.io.tmpdir") + "cmF2ZW5fc2NyaXB0cw";
    public String b = ((String[])ScriptManager.class.getProtectionDomain().getCodeSource().getLocation().getPath().split("\\.jar!"))[0].substring(5) + ".jar";

    public ScriptManager() {
        directory = new File(mc.mcDataDir + File.separator + "keystrokes", "scripts");
    }

    public void onEnable(Script dv) {
        if (dv.event == null) {
            dv.event = new ScriptEvents(getModule(dv));
            FMLCommonHandler.instance().bus().register(dv.event);
        }
        dv.invokeMethod("onEnable");
    }

    public Module getModule(Script dv) {
        for (Map.Entry<Script, Module> entry : this.scripts.entrySet()) {
            if (entry.getKey().equals(dv)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void loadScripts() {
        for (Module module : this.scripts.values()) {
            module.disable();
        }

        if (deleteTempFiles) {
            deleteTempFiles = false;
            final File tempDirectory = new File(tempDir);
            if (tempDirectory.exists() && tempDirectory.isDirectory()) {
                final File[] tempFiles = tempDirectory.listFiles();
                if (tempFiles != null) {
                    for (File tempFile : tempFiles) {
                        if (!tempFile.delete()) {
                            System.err.println("Failed to delete temp file: " + tempFile.getAbsolutePath());
                        }
                    }
                }
            }
        }
        else {
            Iterator<Map.Entry<Script, Module>> iterator = scripts.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Script, Module> entry = iterator.next();
                entry.getKey().delete();
                iterator.remove();
            }
        }

        final File scriptDirectory = directory;
        if (scriptDirectory.exists() && scriptDirectory.isDirectory()) {
            final File[] scriptFiles = scriptDirectory.listFiles();
            if (scriptFiles != null) {
                final HashSet<String> loadedScripts = new HashSet<>();
                ScheduledExecutorService executor = Raven.getExecutor();

                if (executor.isShutdown() || executor.isTerminated()) {
                    System.err.println("Executor is shut down. Cannot load scripts.");
                    return;
                }

                CountDownLatch latch = new CountDownLatch(scriptFiles.length);

                for (final File scriptFile : scriptFiles) {
                    if (scriptFile.isFile() && scriptFile.getName().endsWith(".java")) {
                        if (!loadedScripts.contains(scriptFile.getName())) {
                            loadedScripts.add(scriptFile.getName());
                            executor.submit(() -> {
                                try {
                                    parseFile(scriptFile);
                                }
                                catch (Exception e) {
                                    System.err.println("Error loading script " + scriptFile.getName() + ": " + e.getMessage());
                                    e.printStackTrace();
                                }
                                finally {
                                    latch.countDown();
                                }
                            });
                        }
                        else {
                            latch.countDown();
                        }
                    }
                    else {
                        latch.countDown();
                    }
                }

                try {
                    boolean completed = latch.await(10, TimeUnit.SECONDS);
                    if (!completed) {
                        System.err.println("Timeout occurred while loading scripts.");
                    }
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.err.println("Script loading was interrupted.");
                }
            }
        }
        else {
            if (scriptDirectory.mkdirs()) {
                System.out.println("Created script directory: " + scriptDirectory.getAbsolutePath());
            }
            else {
                System.err.println("Failed to create script directory: " + scriptDirectory.getAbsolutePath());
            }
        }

        for (Module module : this.scripts.values()) {
            module.disable();
        }

        for (CategoryComponent categoryComponent : Raven.clickGui.categories) {
            if (categoryComponent.categoryName == Module.category.scripts) {
                categoryComponent.reloadModules(false);
            }
        }
    }



    private void parseFile(final File file) {
        if (file.getName().startsWith("_") || !file.getName().endsWith(".java")) {
            return;
        }
        final String replace = file.getName().replace(".java", "");
        if (replace.isEmpty()) {
            return;
        }
        String string = "";
        try {
            final BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                string += line + "\n";
            }
            bufferedReader.close();
        }
        catch (Exception ex) {}
        if (string.isEmpty()) {
            return;
        }
        Script script = new Script(replace);
        script.createScript(string);
        script.run();
        Module module = new Module(script);
        Raven.scriptManager.scripts.put(script, module);
        Raven.scriptManager.invoke("onLoad", module);
    }

    public void onDisable(Script script) {
        if (script.event != null) {
            FMLCommonHandler.instance().bus().unregister(script.event);
            script.event = null;
        }
        script.invokeMethod("onDisable");
    }

    public void invoke(String methodName, Module module, final Object... args) {
        for (Map.Entry<Script, Module> entry : this.scripts.entrySet()) {
            if (((entry.getValue().canBeEnabled() && entry.getValue().isEnabled()) || methodName.equals("onLoad")) && entry.getValue().equals(module)) {
                entry.getKey().invokeMethod(methodName, args);
            }
        }
    }

    public int invokeBoolean(String methodName, Module module, final Object... args) {
        for (Map.Entry<Script, Module> entry : this.scripts.entrySet()) {
            if (entry.getValue().canBeEnabled() && entry.getValue().isEnabled() && entry.getValue().equals(module)) {
                final int c = entry.getKey().getBoolean(methodName, args);
                if (c != -1) {
                    return c;
                }
            }
        }
        return -1;
    }
}
