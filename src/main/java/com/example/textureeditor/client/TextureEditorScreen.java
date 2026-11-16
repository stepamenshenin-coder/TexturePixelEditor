package com.example.textureeditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class TextureEditorScreen extends Screen {
    private EditBox textureField;
    private EditBox colorField;
    private NativeImage source;
    private int[] pixels;
    private int imageW, imageH;
    private int color = 0xFFFF0000;
    private String status = "Укажи текстуру, например: block/stone";
    private final Deque<int[]> undo = new ArrayDeque<>();
    private int canvasX, canvasY, scale = 8;
    private boolean loaded = false;

    public TextureEditorScreen() { super(Component.literal("Редактор текстур")); }

    @Override
    protected void init() {
        int cx = width / 2;
        textureField = new EditBox(font, cx - 150, 35, 300, 20, Component.literal("Путь текстуры"));
        textureField.setValue("block/stone");
        addRenderableWidget(textureField);

        colorField = new EditBox(font, cx - 150, 62, 110, 20, Component.literal("Цвет"));
        colorField.setValue("#FF0000");
        addRenderableWidget(colorField);

        addRenderableWidget(Button.builder(Component.literal("Загрузить"), b -> loadTexture())
                .bounds(cx - 32, 62, 82, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Применить цвет"), b -> parseColor())
                .bounds(cx + 55, 62, 105, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> undo())
                .bounds(cx - 150, height - 48, 65, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> saveTexture())
                .bounds(cx - 78, height - 48, 75, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Сбросить"), b -> resetTexture())
                .bounds(cx + 5, height - 48, 75, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"), b -> onClose())
                .bounds(cx + 88, height - 48, 62, 20).build());
        if (!loaded) loadTexture();
    }

    private String cleanPath() {
        String p = textureField.getValue().trim();
        if (p.startsWith("minecraft:")) p = p.substring("minecraft:".length());
        if (p.startsWith("/")) p = p.substring(1);
        if (!p.endsWith(".png")) p += ".png";
        if (!p.startsWith("textures/")) p = "textures/" + p;
        return p;
    }

    private void loadTexture() {
        try {
            String path = cleanPath();
            ResourceLocation loc = new ResourceLocation("minecraft", path);
            var resource = Minecraft.getInstance().getResourceManager().getResource(loc)
                    .orElseThrow(() -> new IllegalArgumentException("Текстура не найдена: " + loc));
            try (InputStream in = resource.open()) {
                source = NativeImage.read(in);
            }
            imageW = source.getWidth();
            imageH = source.getHeight();
            pixels = new int[imageW * imageH];
            for (int y = 0; y < imageH; y++) for (int x = 0; x < imageW; x++) {
                int abgr = source.getPixelRGBA(x, y);
                int a = (abgr >>> 24) & 255, b = (abgr >>> 16) & 255;
                int g = (abgr >>> 8) & 255, r = abgr & 255;
                pixels[y * imageW + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            undo.clear();
            loaded = true;
            status = "Загружено: " + loc + " (" + imageW + "x" + imageH + ")";
            calculateCanvas();
        } catch (Exception e) {
            loaded = false;
            status = "Ошибка загрузки: " + e.getMessage();
        }
    }

    private void calculateCanvas() {
        if (!loaded) return;
        int availableW = Math.max(32, width - 80);
        int availableH = Math.max(32, height - 175);
        scale = Math.max(1, Math.min(16, Math.min(availableW / imageW, availableH / imageH)));
        canvasX = (width - imageW * scale) / 2;
        canvasY = 105 + Math.max(0, (availableH - imageH * scale) / 2);
    }

    private void parseColor() {
        try {
            String s = colorField.getValue().trim().replace("#", "");
            if (s.length() == 6) s = "FF" + s;
            color = (int) Long.parseLong(s, 16);
            status = "Текущий цвет: " + colorField.getValue();
        } catch (Exception e) { status = "Неверный цвет. Формат: #RRGGBB"; }
    }

    private void pushUndo() {
        if (pixels == null) return;
        undo.push(Arrays.copyOf(pixels, pixels.length));
        while (undo.size() > 30) undo.removeLast();
    }

    private void undo() {
        if (!undo.isEmpty()) pixels = undo.pop();
        else status = "Нет действий для отмены";
    }

    private void saveTexture() {
        if (!loaded) return;
        try {
            String rel = cleanPath();
            Path pack = Minecraft.getInstance().gameDirectory.toPath().resolve("resourcepacks/TexturePixelEditor");
            Path out = pack.resolve("assets/minecraft").resolve(rel);
            Files.createDirectories(out.getParent());
            try (NativeImage img = new NativeImage(imageW, imageH, true)) {
                for (int y = 0; y < imageH; y++) for (int x = 0; x < imageW; x++) {
                    int argb = pixels[y * imageW + x];
                    int a = (argb >>> 24) & 255, r = (argb >>> 16) & 255;
                    int g = (argb >>> 8) & 255, b = argb & 255;
                    img.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                }
                img.writeToFile(out);
            }
            Path meta = pack.resolve("pack.mcmeta");
            if (!Files.exists(meta)) Files.writeString(meta, "{\n  \"pack\": { \"pack_format\": 15, \"description\": \"Textures edited with Texture Pixel Editor\" }\n}");
            status = "Сохранено в resourcepacks/TexturePixelEditor. Включи пак в настройках ресурсов.";
        } catch (Exception e) { status = "Ошибка сохранения: " + e.getMessage(); }
    }

    private void resetTexture() {
        try {
            Path pack = Minecraft.getInstance().gameDirectory.toPath().resolve("resourcepacks/TexturePixelEditor");
            Path out = pack.resolve("assets/minecraft").resolve(cleanPath());
            Files.deleteIfExists(out);
            loadTexture();
            status = "Сохранённая версия удалена. Оригинал будет восстановлен после обновления ресурсов.";
        } catch (Exception e) { status = "Ошибка сброса: " + e.getMessage(); }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, "Редактор текстур", width / 2, 12, 0xFFFFFF);
        g.drawString(font, "Путь: minecraft:textures/...", 12, 39, 0xAAAAAA);
        if (loaded && pixels != null) {
            calculateCanvas();
            g.fill(canvasX - 2, canvasY - 2, canvasX + imageW * scale + 2, canvasY + imageH * scale + 2, 0xFF777777);
            for (int y = 0; y < imageH; y++) for (int x = 0; x < imageW; x++) {
                int c = pixels[y * imageW + x];
                int a = (c >>> 24) & 255;
                if (a == 0) {
                    g.fill(canvasX + x * scale, canvasY + y * scale, canvasX + (x + 1) * scale,
                            canvasY + (y + 1) * scale, ((x + y) % 2 == 0) ? 0xFFBBBBBB : 0xFFEEEEEE);
                } else {
                    g.fill(canvasX + x * scale, canvasY + y * scale, canvasX + (x + 1) * scale,
                            canvasY + (y + 1) * scale, c);
                }
                if (scale >= 5) g.renderOutline(canvasX + x * scale, canvasY + y * scale, scale, scale, 0x22808080);
            }
        }
        g.drawCenteredString(font, status, width / 2, height - 70, 0xFFFFFF);
        g.drawCenteredString(font, "ЛКМ — покрасить   ПКМ — сделать прозрачным", width / 2, height - 84, 0xDDDDDD);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (loaded && pixels != null && (button == 0 || button == 1)) {
            int px = (int) ((mouseX - canvasX) / scale);
            int py = (int) ((mouseY - canvasY) / scale);
            if (px >= 0 && py >= 0 && px < imageW && py < imageH) {
                pushUndo();
                pixels[py * imageW + px] = button == 0 ? color : 0x00000000;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (loaded && pixels != null && (button == 0 || button == 1)) {
            int px = (int) ((mouseX - canvasX) / scale);
            int py = (int) ((mouseY - canvasY) / scale);
            if (px >= 0 && py >= 0 && px < imageW && py < imageH) {
                pixels[py * imageW + px] = button == 0 ? color : 0x00000000;
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(null); }
}
