package zov.polyak.ui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import zov.polyak.module.Module;
import zov.polyak.module.settings.*;
import zov.polyak.ui.component.Component;
import zov.polyak.ui.component.impl.*;
import zov.polyak.util.cursor.CursorManager;
import zov.polyak.util.render.helper.HoverUtil;
import zov.polyak.util.render.math.Animation;
import zov.polyak.util.render.math.Easing;
import zov.polyak.util.render.msdf.Fonts;
import zov.polyak.util.render.providers.ColorProvider;
import zov.polyak.util.render.renderers.DrawUtil;

@Getter
public class ModuleComponent extends Component {
    private final Module module;
    private final Panel panel;

    private final Animation animation = new Animation(Easing.QUINTIC_OUT, 320);
    private final Animation hoverAnim = new Animation(Easing.QUINTIC_OUT, 300);
    private final Animation enabledAnim = new Animation(Easing.QUINTIC_OUT, 400);

    public boolean open;
    private boolean isHovered;
    private boolean binding;

    private final ObjectArrayList<Component> components = new ObjectArrayList<>();

    public ModuleComponent(Module module, Panel panel) {
        this.module = module;
        this.panel = panel;
        for (Setting setting : module.getSettings()) {
            switch (setting) {
                case BooleanSetting option -> components.add(new BooleanComponent(option));
                case ModeSetting option -> components.add(new ModeComponent(option));
                case ModeListSetting option -> components.add(new ModeListComponent(option));
                case SliderSetting option -> components.add(new SliderComponent(option));
                case BindSetting option -> components.add(new BindComponent(option));
                case ThemeSetting option -> components.add(new ThemeComponent(option));
                case ColorSetting option -> components.add(new ColorPickerComponent(option));
                default -> {}
            }
        }

        if (module.getName().equals("Interface")) {
            components.add(new ThemeActionComponent(this));
        }
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        isHovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, 19);

        hoverAnim.run(isHovered);
        animation.run(open);
        enabledAnim.run(module.isEnabled());

        if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, 19)) CursorManager.requestHand();

        float alpha = Math.max(Math.min(panel.getAnimationAlpha().getValue(), 1), 0);

        int textColor = ColorProvider.interpolateColor(
                ColorProvider.setAlpha(ColorProvider.getColorInactiveText(), (int)(255 * alpha)),
                ColorProvider.setAlpha(ColorProvider.getColorText(), (int)(255 * alpha)),
                enabledAnim.getValue()
        );

        float highlightProgress = Math.max(hoverAnim.getValue(), enabledAnim.getValue());
        int outlineAlpha = (int) ((25 + (40 * highlightProgress)) * alpha);

        int outlineColor = ColorProvider.rgba(255, 255, 255, outlineAlpha);
        int innerColor = ColorProvider.interpolateColor(
                ColorProvider.setAlpha(ColorProvider.getColorMain(), (int)(70 * alpha)),
                ColorProvider.setAlpha(ColorProvider.getColorVisualModules(), (int)(50 * alpha)),
                enabledAnim.getValue()
        );

        float currentHeight = 19f + ((height - 19f) * animation.getValue());

        DrawUtil.drawRound(x, y, width, currentHeight - 0.5f, 3f, innerColor);

        if (binding) {
            DrawUtil.drawText(Fonts.SFREGULAR.get(), "Нажмите клавишу...", x + width / 2f - Fonts.SFREGULAR.get().getWidth("Нажмите клавишу...", 7.5f) / 2f, y + 5.75f, ColorProvider.rgba(255, 255, 255, (int)(255 * alpha)), 7.5f);
        } else {
            DrawUtil.drawText(Fonts.SFREGULAR.get(), module.getName(), x + 4.5f, y + 5.25f, textColor, 7.5f);

            if (!components.isEmpty()) {
                DrawUtil.drawText(Fonts.DIVINE.get(), "h", x + width - 12, y + 5.75f, textColor, 7.5f);
            }
        }

        if (animation.getValue() > 0.01f) {
            float compY = y + 17.5f;
            float panelTop = panel.getY() + 20;
            float panelBottom = panel.getY() + panel.getHeight() - 4;
            float settingsY = y + 19;
            float settingsBottom = y + currentHeight;

            float intersectY = Math.max(settingsY, panelTop);
            float intersectBottom = Math.min(settingsBottom, panelBottom);
            float intersectHeight = Math.max(0, intersectBottom - intersectY);

            float darkHeight = currentHeight - 19f;
            if (darkHeight > 0) {
                DrawUtil.drawRound(x + 1f, y + 19, width - 2f, darkHeight, 0f, ColorProvider.rgba(0, 0, 0, (int)(30 * alpha * animation.getValue())));
            }

            for (Component component : components) {
                component.getAlphaAnim().setValue(Math.min(panel.getAnimationAlpha().getValue(), 1));
                component.getAlphaAnimSetting().run(component.isVisible());

                float visibleProgress = MathHelper.clamp(component.getAlphaAnimSetting().getValue(), 0f, 1f);
                if (component.isVisible() || visibleProgress > 0) {
                    component.setX(x);
                    component.setY(compY);
                    component.setWidth(width - 4);

                    zov.polyak.util.render.math.Scissor.push();
                    zov.polyak.util.render.math.Scissor.setFromComponentCoordinates(x, intersectY, width, intersectHeight);

                    component.render(matrixStack, mouseX, mouseY, partialTicks);

                    zov.polyak.util.render.math.Scissor.unset();
                    zov.polyak.util.render.math.Scissor.pop();

                    compY += component.getHeight() * visibleProgress;
                }
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 19)) {
            if (button == 0) module.setEnabled(!module.isEnabled());
            if (button == 1 && !components.isEmpty()) open = !open;
            if (button == 2) binding = !binding;
        }

        if (open) {
            for (Component component : components) {
                if (component.isVisible() && component.getAlphaAnimSetting().getValue() > 0.5f) {
                    component.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (open) {
            for (Component component : components) {
                component.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                module.setKey(-1);
            } else {
                module.setKey(keyCode);
            }
            binding = false;
            return;
        }

        if (open) {
            for (Component component : components) {
                component.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    private boolean isHovered(double mouseX, double mouseY, float heightCheck) {
        return HoverUtil.isHovered(mouseX, mouseY, x, y, width, heightCheck);
    }

    public static class ThemeActionComponent extends Component {
        private final ModuleComponent parent;
        private final Animation hoverAnim = new Animation(Easing.QUINTIC_OUT, 300);

        public ThemeActionComponent(ModuleComponent parent) {
            this.parent = parent;
        }

        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            float btnX = x + 2f;
            float btnY = y + 1f;
            float btnW = width - 4f;
            float btnH = 14f;

            boolean isHovered = HoverUtil.isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH);
            hoverAnim.run(isHovered);
            if (isHovered) CursorManager.requestHand();

            float alpha = (float) getAlphaAnim().getValue();

            int outlineAlpha = (int) ((25 + (40 * hoverAnim.getValue())) * alpha);
            int outlineColor = ColorProvider.rgba(255, 255, 255, outlineAlpha);
            int innerColor = ColorProvider.rgba(44, 44, 44, (int)(140 * alpha));

            DrawUtil.drawRound(btnX - 0.5f, btnY - 0.5f, btnW + 1f, btnH + 0.5f, 3.5f, outlineColor);
            DrawUtil.drawRoundBlur(btnX, btnY, btnW, btnH - 0.5f, 3f, innerColor, 20f);

            int textColor = ColorProvider.rgba(255, 255, 255, (int)(255 * alpha));
            DrawUtil.drawText(Fonts.SFREGULAR.get(), "Открыть менеджер тем", btnX + 3.5f, btnY + 3.25f, textColor, 7.35f);
        }

        @Override
        public void mouseClicked(double mouseX, double mouseY, int button) {
            float btnX = x + 2f;
            float btnY = y + 1f;
            float btnW = width - 4f;
            float btnH = 14f;

            if (HoverUtil.isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH) && button == 0) {
                ThemeManagerWindow tm = parent.getPanel().getParent().getThemeManager();
                tm.setOpen(!tm.isOpen());
            }
        }

        @Override
        public void mouseReleased(double mouseX, double mouseY, int button) {}

        @Override
        public void keyPressed(int keyCode, int scanCode, int modifiers) {}

        @Override
        public float getHeight() {
            return 16f;
        }

        @Override
        public boolean isVisible() {
            return true;
        }
    }
}