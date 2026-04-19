package zov.polyak.ui;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import zov.polyak.module.ModuleCategory;
import zov.polyak.ui.component.SearchField;
import zov.polyak.util.IMinecraft;
import zov.polyak.util.cursor.CursorManager;
import zov.polyak.util.render.helper.HoverUtil;
import zov.polyak.util.render.math.Easing;
import zov.polyak.util.render.msdf.Fonts;
import zov.polyak.util.render.providers.ColorProvider;
import zov.polyak.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ClickGuiFrame extends Screen implements IMinecraft {

    private final List<Panel> panels = new ArrayList<>();
    private final SearchField searchField;

    private final ThemeManagerWindow themeManager;

    public ClickGuiFrame() {
        super(Text.of("Avalora Frame"));
        searchField = new SearchField("Search...");
        for (ModuleCategory category : ModuleCategory.values()) {
            panels.add(new Panel(category, this));
        }
        themeManager = new ThemeManagerWindow(this);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        CursorManager.reset();
        CursorManager.resetIBeam();
        CursorManager.resetClick();

        int windowWidth = mc.getWindow().getScaledWidth();
        int windowHeight = mc.getWindow().getScaledHeight();

        float panelWidth = 105f;
        float spacing = 4f;
        float panelHeight = 240f;
        float panelTotalWidth = panels.size() * (panelWidth + spacing) - spacing;

        float startX = (windowWidth - panelTotalWidth) / 2f;
        float panelY = (windowHeight - panelHeight) / 2f - 20;

        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            panel.getAnimationAlpha().setDuration(650);
            panel.getAnimationAlpha().run(1);
            panel.getAnimationAlpha().setEasing(Easing.QUINTIC_OUT);

            panel.setX(startX + i * (panelWidth + spacing));
            panel.setY(panelY);
            panel.setWidth(panelWidth);
            panel.setHeight(panelHeight);

            panel.render(context.getMatrices(), mouseX, mouseY, delta);
        }

        float searchW = 90;
        float searchH = 18;
        float searchX = windowWidth / 2f - searchW / 2f;
        float searchY = panelY + panelHeight + 15;

        searchField.setBounds(searchX, searchY, searchW, searchH);
        searchField.render(context, mouseX, mouseY, delta);

        themeManager.setX(20);
        themeManager.setY(panelY);
        themeManager.render(context.getMatrices(), mouseX, mouseY, delta);

        for (Panel panel : panels) {
            boolean isMouseInPanel = HoverUtil.isHovered(mouseX, mouseY, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight());

            for (ModuleComponent component : panel.getModuleComponents()) {
                if (component.isHovered() && isMouseInPanel && searchField.isEmpty()) {
                    String desc = component.getModule().getDesc();
                    if (desc != null && !desc.isEmpty()) {
                        float textWidth = Fonts.SFREGULAR.get().getWidth(desc, 8f) + 2;
                        DrawUtil.drawRound(windowWidth / 2f - textWidth / 2f - 2, windowHeight / 2f - 180, textWidth + 8, 14, 0, ColorProvider.rgba(0, 0, 0, 111));
                        DrawUtil.drawText(Fonts.SFREGULAR.get(), desc, windowWidth / 2f - textWidth / 2f, windowHeight / 2f - 176, ColorProvider.rgba(255, 255, 255, 255), 8f);
                    }
                }
            }
        }

        long window = mc.getWindow().getHandle();
        if (CursorManager.shouldBeHand()) GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
        else if (CursorManager.shouldIBeam()) GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR));
        else if (CursorManager.shouldClick()) GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR));
        else GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
    }

    public boolean searchCheck(String text) {
        return !searchField.isEmpty() && !text.replaceAll(" ", "").toLowerCase().contains(searchField.text.replaceAll(" ", "").toLowerCase());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        themeManager.mouseClicked(mouseX, mouseY, button);
        searchField.mouseClicked(mouseX, mouseY, button);

        if (searchField.isEmpty()) {
            for (Panel panel : panels) {
                if (HoverUtil.isHovered(mouseX, mouseY, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight())) {
                    panel.mouseClicked(mouseX, mouseY, button);
                }
            }
        } else {
            for (Panel panel : panels) {
                panel.mouseClicked(mouseX, mouseY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        themeManager.mouseReleased(mouseX, mouseY, button);
        for (Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        themeManager.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        for (Panel panel : panels) {
            panel.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        themeManager.keyPressed(keyCode);
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            long window = mc.getWindow().getHandle();
            GLFW.glfwSetCursor(window, GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
        }
        searchField.keyPressed(keyCode, scanCode, modifiers);
        for (Panel panel : panels) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                panel.getAnimationAlpha().setValue(0);
                panel.getAnimationAlpha().reset();
            }
            panel.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        themeManager.charTyped(chr);
        searchField.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}