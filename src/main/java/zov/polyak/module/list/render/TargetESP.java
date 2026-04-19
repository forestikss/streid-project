package zov.polyak.module.list.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import zov.polyak.Polyak;
import zov.polyak.module.Module;
import zov.polyak.module.ModuleCategory;
import zov.polyak.module.ModuleInformation;
import zov.polyak.module.list.combat.KillAura;
import zov.polyak.module.settings.BooleanSetting;
import zov.polyak.module.settings.ModeSetting;
import zov.polyak.module.settings.SliderSetting;
import zov.polyak.util.render.math.Animation;
import zov.polyak.util.render.math.Easing;
import zov.polyak.util.render.math.MathUtil;
import zov.polyak.util.render.providers.ColorProvider;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInformation(moduleName = "Target ESP", moduleDesc = "Визуальный эффект на текущей цели", moduleCategory = ModuleCategory.RENDER)
public class TargetESP extends Module {

    private static final Identifier GLOW_TEXTURE = Identifier.of("mre", "images/bloom.png");

    private final ModeSetting mode = new ModeSetting("Режим", "Ghost", "Ghost", "Crystals", "Crystals2", "Cubes", "Prizraki", "Kolco", "GhostRider");

    private final SliderSetting ghostSpeed = new SliderSetting("Скорость призраков", 1.0f, 0.5f, 2.0f, 0.1f).setVisible(() -> mode.is("Ghost") || mode.is("Prizraki"));
    private final SliderSetting prizrakiSize = new SliderSetting("Размер призраков", 0.4f, 0.1f, 1.0f, 0.05f).setVisible(() -> mode.is("Prizraki"));

    private final SliderSetting kolcoSpeed = new SliderSetting("Скорость кольца", 0.01f, 0.05f, 0.3f, 0.01f).setVisible(() -> mode.is("Kolco"));
    private final BooleanSetting kolcoRainbow = new BooleanSetting("Радуга кольца", true).setVisible(() -> mode.is("Kolco"));
    private final BooleanSetting kolcoDamageRed = new BooleanSetting("Красный при уроне", true).setVisible(() -> mode.is("Kolco"));
    private final BooleanSetting kolcoThroughWalls = new BooleanSetting("Через стены", false).setVisible(() -> mode.is("Kolco"));

    private final Animation animation = new Animation(Easing.EXPO_OUT, 500);

    private Entity lastTarget = null;
    private boolean registered = false;

    // Ghost
    private float ghostAnimTime = 0f;
    private long lastGhostTimestamp = 0L;

    // Prizraki
    private static long lastPrizrakTime = System.currentTimeMillis();

    // Kolco
    private long lastKolcoUpdateTime = 0L;
    private double kolcoStep = 0;
    private float kolcoAnimProgress = 0f;
    private long lastDamageTime = 0;
    private float damageFlashIntensity = 0f;
    private static final long DAMAGE_FLASH_DURATION = 500;

    // Cubes
    private final ArrayList<Particle> particles = new ArrayList<>();
    private static final int PARTICLES_PER_SPAWN = 1;
    private static final float SPAWN_INTERVAL = 0.017f;
    private float spawnAccumulator = 0f;
    private long lastCubeTime = 0L;

    // Crystals anim
    private final Animation crystalsAnim = new Animation(Easing.EXPO_OUT, 350);
    private Entity crystalsLastTarget = null;

    private final WorldRenderEvents.Last listener = context -> {
        onRenderWorldLast(context.matrixStack(), context.camera(), context.tickCounter().getTickDelta(true));
    };

    @Override
    public void onEnable() {
        if (!registered) {
            WorldRenderEvents.LAST.register(listener);
            registered = true;
        }
        super.onEnable();
    }

    private void onRenderWorldLast(MatrixStack matrices, Camera camera, float tickDelta) {
        if (!isEnabled()) return;

        Entity target = Polyak.getInstance().getModuleStorage().get(KillAura.class).getTarget();

        if (target != null && target != mc.player && !(target instanceof ArmorStandEntity)) {
            lastTarget = target;
            animation.run(1);
        } else {
            animation.run(0);
            if (animation.getValue() == 0) {
                lastTarget = null;
            }
        }

        float partialTicks = tickDelta;

        if (target != null && lastTarget != null) {
            switch (mode.getValue()) {
                case "Ghost" -> renderGhost(matrices, camera, (LivingEntity) lastTarget, partialTicks);
                case "Crystals" -> {
                    crystalsLastTarget = lastTarget;
                    crystalsAnim.run(1);
                    float anim = crystalsAnim.getValue();
                    if (anim > 0.001f) renderCrystals(matrices, camera, (LivingEntity) crystalsLastTarget, false, anim, partialTicks);
                }
                case "Crystals2" -> {
                    crystalsLastTarget = lastTarget;
                    crystalsAnim.run(1);
                    float anim = crystalsAnim.getValue();
                    if (anim > 0.001f) renderCrystals(matrices, camera, (LivingEntity) crystalsLastTarget, true, anim, partialTicks);
                }
                case "Cubes" -> renderCubes(matrices, camera, (LivingEntity) lastTarget, partialTicks);
                case "Prizraki" -> renderPrizraki(matrices, camera, (LivingEntity) lastTarget, partialTicks);
                case "Kolco" -> {
                    kolcoAnimProgress = Math.min(1f, kolcoAnimProgress + 0.1f);
                    if (lastTarget instanceof LivingEntity living && living.hurtTime > 0) {
                        lastDamageTime = System.currentTimeMillis();
                    }
                    long now = System.currentTimeMillis();
                    if (lastKolcoUpdateTime == 0L) lastKolcoUpdateTime = now;
                    float dt = Math.min((now - lastKolcoUpdateTime) / 1000.0f, 0.1f);
                    lastKolcoUpdateTime = now;
                    kolcoStep += kolcoSpeed.getFloatValue() * dt * 60.0;
                    renderKolco(matrices, camera, (LivingEntity) lastTarget, partialTicks, kolcoAnimProgress);
                }
                case "GhostRider" -> renderGhostRider(matrices, camera, (LivingEntity) lastTarget, partialTicks);
            }
        } else {
            lastGhostTimestamp = 0L;
            ghostAnimTime = 0f;
            lastCubeTime = 0L;
            spawnAccumulator = 0f;
            riderParticles.clear();

            if (mode.is("Crystals") || mode.is("Crystals2")) {
                crystalsAnim.run(0);
                float anim = crystalsAnim.getValue();
                if (anim > 0.001f && crystalsLastTarget != null) {
                    renderCrystals(matrices, camera, (LivingEntity) crystalsLastTarget, mode.is("Crystals2"), anim, partialTicks);
                } else if (anim <= 0.001f) {
                    crystalsLastTarget = null;
                }
            }

            if (mode.is("Kolco")) {
                kolcoAnimProgress = Math.max(0f, kolcoAnimProgress - 0.05f);
                if (kolcoAnimProgress > 0 && lastTarget != null) {
                    long now = System.currentTimeMillis();
                    if (lastKolcoUpdateTime == 0L) lastKolcoUpdateTime = now;
                    float dt = Math.min((now - lastKolcoUpdateTime) / 1000.0f, 0.1f);
                    lastKolcoUpdateTime = now;
                    kolcoStep += kolcoSpeed.getFloatValue() * dt * 60.0;
                    renderKolco(matrices, camera, (LivingEntity) lastTarget, partialTicks, kolcoAnimProgress);
                }
            } else {
                lastKolcoUpdateTime = 0L;
            }

            if (mode.is("Cubes") && !particles.isEmpty()) {
                renderCubes(matrices, camera, null, partialTicks);
            }
        }
    }

    // ─── Ghost ───────────────────────────────────────────────────────────────

    private void renderGhost(MatrixStack matrices, Camera camera, LivingEntity target, float partialTicks) {
        Vec3d camPos = camera.getPos();
        float camYaw = camera.getYaw();
        float camPitch = camera.getPitch();

        double tx = MathHelper.lerp(partialTicks, target.lastRenderX, target.getX());
        double ty = MathHelper.lerp(partialTicks, target.lastRenderY, target.getY());
        double tz = MathHelper.lerp(partialTicks, target.lastRenderZ, target.getZ());

        double x = tx - camPos.x;
        double y = ty - camPos.y;
        double z = tz - camPos.z;

        long now = System.currentTimeMillis();
        if (lastGhostTimestamp == 0) lastGhostTimestamp = now;
        ghostAnimTime += (4f * (now - lastGhostTimestamp) / 600f) * ghostSpeed.getFloatValue();
        lastGhostTimestamp = now;

        int colorInt = ColorProvider.getColorClient();
        float r = ColorProvider.red(colorInt) / 255f;
        float g = ColorProvider.green(colorInt) / 255f;
        float b = ColorProvider.blue(colorInt) / 255f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean hasGhost = false;

        float radiusMod = 1.3f;
        float spriteSize = 0.5f;
        int totalSprites = 25;

        for (int ring = 0; ring < 9; ring += 3) {
            for (int si = 0; si < totalSprites; si++) {
                float ao = si * 0.1f;
                float ro = (float) Math.pow(ring, 2.0);

                double sx = x + radiusMod * 0.6f * MathHelper.sin(ghostAnimTime + ao + ro);
                double sy = y + 0.4f + radiusMod * 0.3f * MathHelper.sin(ghostAnimTime + si * 0.2f) + 0.2f * ring;
                double sz = z + radiusMod * 0.6f * MathHelper.cos(ghostAnimTime + ao - ro);

                float progress = (float) si / (totalSprites - 1);
                float scale = spriteSize * (0.2f + progress * 0.8f);
                float alpha = 1.0f - progress * 0.9f;
                if (scale < 0.01f || alpha < 0.01f) continue;

                float cr = Math.min(1f, r * 1.5f), cg = Math.min(1f, g * 1.5f), cb = Math.min(1f, b * 1.5f);
                putBloomQuad(builder, matrices, sx, sy, sz, scale * 4.0f, r, g, b, alpha * 0.15f, camYaw, camPitch);
                putBloomQuad(builder, matrices, sx, sy, sz, scale * 2.0f, r, g, b, alpha * 0.35f, camYaw, camPitch);
                putBloomQuad(builder, matrices, sx, sy, sz, scale * 0.8f, cr, cg, cb, alpha, camYaw, camPitch);
                hasGhost = true;
            }
        }

        if (hasGhost) BufferRenderer.drawWithGlobalProgram(builder.end());
        restoreRenderState();
    }

    // ─── Prizraki ─────────────────────────────────────────────────────────────

    private void renderPrizraki(MatrixStack matrices, Camera camera, LivingEntity target, float partialTicks) {
        Vec3d camPos = camera.getPos();
        float camYaw = camera.getYaw();
        float camPitch = camera.getPitch();

        double tx = MathHelper.lerp(partialTicks, target.lastRenderX, target.getX());
        double ty = MathHelper.lerp(partialTicks, target.lastRenderY, target.getY()) + 0.38 + target.getHeight() / 2.0;
        double tz = MathHelper.lerp(partialTicks, target.lastRenderZ, target.getZ());

        double rx = tx - camPos.x + 0.2;
        double ry = ty - camPos.y;
        double rz = tz - camPos.z;

        double radius = 0.4 + target.getWidth() / 2.0;
        float speed = 30f / ghostSpeed.getFloatValue();
        float size = prizrakiSize.getFloatValue();
        double distance = 6;
        int length = 34;
        long now = System.currentTimeMillis();

        int colorInt = ColorProvider.getColorClient();
        float r = ColorProvider.red(colorInt) / 255f;
        float g = ColorProvider.green(colorInt) / 255f;
        float b = ColorProvider.blue(colorInt) / 255f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        // 3 spiral trails
        for (int trail = 0; trail < 3; trail++) {
            for (int i = 0; i < length; i++) {
                double angle = 0.05f * (now - lastPrizrakTime - (i * distance)) / speed;
                double s = Math.sin(angle * Math.PI) * radius;
                double c = Math.cos(angle * Math.PI) * radius;
                double o = (trail == 0) ? Math.cos(angle * Math.PI) * radius : Math.sin(angle * Math.PI) * radius;

                float t = i / (float) (length - 1);
                float curSize = size * (1.0f - t * 0.5f);
                float alpha = 1.0f - t * 0.9f;

                double px = rx + (trail == 1 ? -s : s);
                double py = ry + o;
                double pz = rz + (trail == 2 ? c : -c);

                float cr = Math.min(1f, r * 1.5f), cg = Math.min(1f, g * 1.5f), cb = Math.min(1f, b * 1.5f);
                putBloomQuad(buffer, matrices, px, py, pz, curSize * 0.6f, r, g, b, alpha * 0.15f, camYaw, camPitch);
                putBloomQuad(buffer, matrices, px, py, pz, curSize * 0.35f, r, g, b, alpha * 0.35f, camYaw, camPitch);
                putBloomQuad(buffer, matrices, px, py, pz, curSize * 0.15f, cr, cg, cb, alpha, camYaw, camPitch);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        restoreRenderState();
    }

    // ─── Kolco ────────────────────────────────────────────────────────────────

    private void renderKolco(MatrixStack matrices, Camera camera, LivingEntity target, float partialTicks, float animProgress) {
        if (animProgress <= 0f || target == null) return;

        Vec3d camPos = camera.getPos();
        float camYaw = camera.getYaw();
        float camPitch = camera.getPitch();

        double tx = MathHelper.lerp(partialTicks, target.lastRenderX, target.getX());
        double ty = MathHelper.lerp(partialTicks, target.lastRenderY, target.getY());
        double tz = MathHelper.lerp(partialTicks, target.lastRenderZ, target.getZ());

        double rx = tx - camPos.x;
        double ry = ty - camPos.y;
        double rz = tz - camPos.z;

        float entityWidth = target.getWidth() * 0.9f;
        float entityHeight = target.getHeight();
        float eased = easeOutCubic(animProgress);

        boolean canSee = mc.player != null && mc.player.canSee(target);
        if (!kolcoThroughWalls.getValue() && !canSee) return;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean hasKolco = false;

        double golovkaY = absSin(kolcoStep) * entityHeight;
        float spriteSize = 0.12f;
        int totalPoints = 72;
        long now = System.currentTimeMillis();

        for (int i = 0; i < totalPoints; i++) {
            double angle = 2 * Math.PI * i / totalPoints;
            float xOff = (float) (Math.cos(angle) * entityWidth);
            float zOff = (float) (Math.sin(angle) * entityWidth);

            int baseColor = getKolcoColor(i * (360 / totalPoints), now);
            float cr = ColorProvider.red(baseColor) / 255f;
            float cg = ColorProvider.green(baseColor) / 255f;
            float cb = ColorProvider.blue(baseColor) / 255f;

            float ss = eased * spriteSize;
            if (ss < 0.01f) continue;

            putBloomQuad(builder, matrices, rx + xOff, ry + golovkaY, rz + zOff, ss * 4.0f, cr, cg, cb, eased * 0.15f, camYaw, camPitch);
            putBloomQuad(builder, matrices, rx + xOff, ry + golovkaY, rz + zOff, ss * 2.0f, cr, cg, cb, eased * 0.35f, camYaw, camPitch);
            putBloomQuad(builder, matrices, rx + xOff, ry + golovkaY, rz + zOff, ss * 0.8f,
                    Math.min(1f, cr * 1.5f), Math.min(1f, cg * 1.5f), Math.min(1f, cb * 1.5f), eased, camYaw, camPitch);
            hasKolco = true;
        }

        if (hasKolco) BufferRenderer.drawWithGlobalProgram(builder.end());
        restoreRenderState();
    }

    private double absSin(double input) {
        return Math.abs(1 + Math.sin(input)) / 2.0;
    }

    private int getKolcoColor(int offsetAngle, long now) {
        int color;
        if (kolcoRainbow.getValue()) {
            float timeFactor = (now % 3000) / 3000.0f;
            int hue = (int) (timeFactor * 360) + offsetAngle;
            color = Color.HSBtoRGB((hue % 360) / 360f, 0.7f, 1.0f);
            color = (255 << 24) | (color & 0x00FFFFFF);
        } else {
            color = ColorProvider.getColorClient();
            if (ColorProvider.alpha(color) == 0) color = (255 << 24) | (color & 0x00FFFFFF);
        }
        return applyDamageFlash(color);
    }

    private int applyDamageFlash(int color) {
        if (!kolcoDamageRed.getValue()) return color;
        long timeSince = System.currentTimeMillis() - lastDamageTime;
        float target = 0f;
        if (timeSince < DAMAGE_FLASH_DURATION) {
            target = 1.0f - easeOutCubic((float) timeSince / DAMAGE_FLASH_DURATION);
        }
        damageFlashIntensity = MathHelper.lerp(0.1f, damageFlashIntensity, target);
        if (damageFlashIntensity < 0.05f) return color;

        int a = ColorProvider.alpha(color);
        int r = ColorProvider.red(color);
        int g = ColorProvider.green(color);
        int b = ColorProvider.blue(color);
        return (a << 24)
                | ((int) MathHelper.lerp(damageFlashIntensity, r, 255) << 16)
                | ((int) MathHelper.lerp(damageFlashIntensity, g, 50) << 8)
                | (int) MathHelper.lerp(damageFlashIntensity, b, 50);
    }

    // ─── Crystals ─────────────────────────────────────────────────────────────

    private void renderCrystals(MatrixStack matrices, Camera camera, LivingEntity target, boolean sharp, float anim, float partialTicks) {
        if (target == null || mc.player == null) return;

        float eased = easeOutCubic(anim);
        float time = (mc.player.age + partialTicks) * 6.0f;

        Vec3d camPos = camera.getPos();
        double tx = MathHelper.lerp(partialTicks, target.lastRenderX, target.getX());
        double ty = MathHelper.lerp(partialTicks, target.lastRenderY, target.getY());
        double tz = MathHelper.lerp(partialTicks, target.lastRenderZ, target.getZ());

        float camYaw = camera.getYaw();
        float camPitch = camera.getPitch();

        float entityHeight = target.getHeight();
        float halfWidth = target.getWidth() * 0.5f;

        int baseColor = ColorProvider.getColorClient();
        float r = Math.min(1f, ColorProvider.red(baseColor) / 255f * 1.3f);
        float g = Math.min(1f, ColorProvider.green(baseColor) / 255f * 1.3f);
        float b = Math.min(1f, ColorProvider.blue(baseColor) / 255f * 1.3f);

        matrices.push();
        matrices.translate(tx - camPos.x, ty - camPos.y, tz - camPos.z);

        int crystalCount = 18;
        float pelvisY = entityHeight * 0.35f;
        float torsoY = entityHeight * 0.55f;
        float neckY = entityHeight * 0.74f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        boolean hasCrystals = false;

        for (int i = 0; i < crystalCount; i++) {
            float s1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
            float s2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
            float s3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;

            float angle = time + i * (360f / crystalCount) + s1 * 12f;
            float radius = halfWidth + 0.25f + s3 * 0.15f;
            float cx = radius * (float) Math.cos(Math.toRadians(angle));
            float cz = radius * (float) Math.sin(Math.toRadians(angle));
            float cy = s2 * entityHeight;

            float scale = 0.18f * eased;
            if (scale < 0.001f) continue;
            float lookY = getCrystalLookY(cy, entityHeight, pelvisY, torsoY, neckY);
            float dx = -cx, dy = lookY - cy, dz = -cz;
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx));
            float pitch = (float) Math.toDegrees(Math.atan2(dy, (float) Math.sqrt(dx * dx + dz * dz)));

            drawCrystalShape(buf, matrices, cx, cy, cz, scale, yaw, pitch,
                    (int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(200 * anim), sharp);
            hasCrystals = true;
        }

        if (hasCrystals) BufferRenderer.drawWithGlobalProgram(buf.end());

        // Bloom pass
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

        BufferBuilder glowBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        boolean hasGlow = false;

        for (int i = 0; i < crystalCount; i++) {
            float s1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
            float s2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
            float s3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;

            float angle = time + i * (360f / crystalCount) + s1 * 12f;
            float radius = halfWidth + 0.25f + s3 * 0.15f;
            float cx = radius * (float) Math.cos(Math.toRadians(angle));
            float cz = radius * (float) Math.sin(Math.toRadians(angle));
            float cy = s2 * entityHeight;

            float scale = 0.18f * eased;
            if (anim * 0.15f > 0.001f && scale > 0.0001f) {
                putBloomQuad(glowBuf, matrices, cx, cy, cz, scale * 5.5f, r, g, b, anim * 0.15f, camYaw, camPitch);
                putBloomQuad(glowBuf, matrices, cx, cy, cz, scale * 3.5f, r, g, b, anim * 0.25f, camYaw, camPitch);
                hasGlow = true;
            }
        }

        if (hasGlow) BufferRenderer.drawWithGlobalProgram(glowBuf.end());
        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private float getCrystalLookY(float cy, float h, float pelvis, float torso, float neck) {
        float n = cy / h;
        if (n < 0.33f) return pelvis;
        else if (n < 0.6f) return torso;
        else return neck;
    }

    private void drawCrystalShape(BufferBuilder buf, MatrixStack ms, float x, float y, float z,
                                   float scale, float yaw, float pitch, int r, int g, int b, int a, boolean sharp) {
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pitch));
        ms.scale(scale, scale, scale);

        Matrix4f m = ms.peek().getPositionMatrix();
        float w = sharp ? 0.35f : 0.7f;
        float h = sharp ? 1.2f : 1.0f;

        tri(buf, m, h, 0, 0, 0, w, 0, 0, 0, w, r, g, b, a);
        tri(buf, m, h, 0, 0, 0, 0, w, 0, -w, 0, r, g, b, a);
        tri(buf, m, h, 0, 0, 0, -w, 0, 0, 0, -w, r, g, b, a);
        tri(buf, m, h, 0, 0, 0, 0, -w, 0, w, 0, r, g, b, a);
        tri(buf, m, -h, 0, 0, 0, w, 0, 0, 0, w, r, g, b, a);
        tri(buf, m, -h, 0, 0, 0, 0, w, 0, -w, 0, r, g, b, a);
        tri(buf, m, -h, 0, 0, 0, -w, 0, 0, 0, -w, r, g, b, a);
        tri(buf, m, -h, 0, 0, 0, 0, -w, 0, w, 0, r, g, b, a);

        ms.pop();
    }

    private void tri(BufferBuilder buf, Matrix4f m,
                     float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float x3, float y3, float z3,
                     int r, int g, int b, int a) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, a);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a);
        buf.vertex(m, x3, y3, z3).color(r, g, b, a);
    }

    // ─── Cubes ────────────────────────────────────────────────────────────────

    private void renderCubes(MatrixStack matrices, Camera camera, LivingEntity target, float partialTicks) {
        long now = System.currentTimeMillis();
        if (lastCubeTime == 0L) lastCubeTime = now;
        float dt = Math.min((now - lastCubeTime) / 1000.0f, 0.1f);
        lastCubeTime = now;

        if (target != null) {
            spawnAccumulator += dt;
            while (spawnAccumulator >= SPAWN_INTERVAL) {
                spawnAccumulator -= SPAWN_INTERVAL;
                for (int i = 0; i < PARTICLES_PER_SPAWN; i++) {
                    double rand = MathUtil.random(0, 360);
                    double px = Math.cos(Math.toRadians(rand)) * 0.7;
                    double py = MathUtil.random(0.04, 0.2);
                    double pz = Math.sin(Math.toRadians(rand)) * 0.7;
                    particles.add(new Particle(target, px, py, pz));
                }
            }
        }

        float camYaw = camera.getYaw();
        float camPitch = camera.getPitch();

        Iterator<Particle> it = particles.iterator();
        ArrayList<Particle> toRender = new ArrayList<>();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(dt);
            if (now - p.time > 1000L) it.remove();
            else toRender.add(p);
        }

        int color = ColorProvider.getColorClient();
        for (Particle p : toRender) p.renderCube(matrices, camera, color, partialTicks);

        if (!toRender.isEmpty()) {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);

            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            boolean hasBloom = false;
            for (Particle p : toRender) {
                if (p.renderBloom(builder, matrices, camera, color, camYaw, camPitch, partialTicks)) hasBloom = true;
            }
            if (hasBloom) BufferRenderer.drawWithGlobalProgram(builder.end());

            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }
    }

    // ─── GhostRider ───────────────────────────────────────────────────────────

    private static final int RIDER_MAX_PARTICLES = 3;
    private static final float RIDER_BASE_MUL = 0.05f;
    private static final float RIDER_ALPHA_STEP = 0.005f;

    private final List<RiderParticle> riderParticles = new ArrayList<>();
    private final Animation riderAnim = new Animation(Easing.EXPO_OUT, 300);
    private float riderMovingAngle = 0f;

    private void renderGhostRider(MatrixStack matrices, Camera camera, LivingEntity target, float partialTicks) {
        // spawn up to RIDER_MAX_PARTICLES
        while (riderParticles.size() < RIDER_MAX_PARTICLES) {
            riderParticles.add(new RiderParticle(new Vec3d(target.getX(), target.getY() + target.getHeight() / 2.0, target.getZ())));
        }

        riderMovingAngle = (riderMovingAngle + 20f) % (360f * 100);
        riderAnim.run(target.hurtTime > 7 ? 1 : 0);

        Vec3d camPos = camera.getPos();
        float camYaw = camera.getYaw();
        float camPitch = camera.getPitch();

        double tx = MathHelper.lerp(partialTicks, target.lastRenderX, target.getX());
        double ty = MathHelper.lerp(partialTicks, target.lastRenderY, target.getY());
        double tz = MathHelper.lerp(partialTicks, target.lastRenderZ, target.getZ());

        int colorInt = ColorProvider.getColorClient();
        float r = ColorProvider.red(colorInt) / 255f;
        float g = ColorProvider.green(colorInt) / 255f;
        float b = ColorProvider.blue(colorInt) / 255f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        List<RiderParticle> toRemove = new ArrayList<>();

        for (int i = 0; i < riderParticles.size(); i++) {
            RiderParticle p = riderParticles.get(i);

            float angleOffset = i * (360f / RIDER_MAX_PARTICLES);
            float currentAngle = riderMovingAngle + angleOffset;
            double rad = Math.toRadians(currentAngle);

            float collapseAnim = riderAnim.getValue();
            float orbitRadius = 0.6f * (1f - collapseAnim);

            double targetX = tx + Math.sin(rad) * orbitRadius;
            double targetY = ty + 0.2 + target.getHeight() / 2.0 * Math.sin(Math.toRadians(riderMovingAngle / (i + 1f)));
            double targetZ = tz + Math.cos(rad) * orbitRadius;

            // spring motion toward target
            double mx = (targetX - p.pos.x) * RIDER_BASE_MUL;
            double my = (targetY - p.pos.y) * RIDER_BASE_MUL;
            double mz = (targetZ - p.pos.z) * RIDER_BASE_MUL;
            p.pos = p.pos.add(mx, my, mz);

            // fade in
            if (p.alpha < 1f) p.alpha = Math.min(1f, p.alpha + 0.02f);

            double px = p.pos.x - camPos.x;
            double py = p.pos.y - camPos.y;
            double pz = p.pos.z - camPos.z;

            float cr = Math.min(1f, r * 1.5f), cg = Math.min(1f, g * 1.5f), cb = Math.min(1f, b * 1.5f);
            putBloomQuad(builder, matrices, px, py, pz, 0.6f, r, g, b, p.alpha * 0.15f, camYaw, camPitch);
            putBloomQuad(builder, matrices, px, py, pz, 0.35f, r, g, b, p.alpha * 0.35f, camYaw, camPitch);
            putBloomQuad(builder, matrices, px, py, pz, 0.15f, cr, cg, cb, p.alpha, camYaw, camPitch);
        }

        if (!riderParticles.isEmpty()) BufferRenderer.drawWithGlobalProgram(builder.end());
        restoreRenderState();
    }

    private static class RiderParticle {
        Vec3d pos;
        float alpha;
        RiderParticle(Vec3d pos) { this.pos = pos; this.alpha = 0f; }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void putBloomQuad(BufferBuilder builder, MatrixStack ms,
                               double x, double y, double z,
                               float scale, float r, float g, float b, float a,
                               float camYaw, float camPitch) {
        if (a <= 0.001f || scale <= 0.0001f) return;
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));
        ms.scale(scale, scale, scale);

        Matrix4f m = ms.peek().getPositionMatrix();
        int ri = (int)(r * 255), gi = (int)(g * 255), bi = (int)(b * 255), ai = (int)(a * 255);

        builder.vertex(m, -0.5f,  0.5f, 0).texture(0f, 1f).color(ri, gi, bi, ai);
        builder.vertex(m,  0.5f,  0.5f, 0).texture(1f, 1f).color(ri, gi, bi, ai);
        builder.vertex(m,  0.5f, -0.5f, 0).texture(1f, 0f).color(ri, gi, bi, ai);
        builder.vertex(m, -0.5f, -0.5f, 0).texture(0f, 0f).color(ri, gi, bi, ai);

        ms.pop();
    }

    private static float easeOutCubic(float t) {
        float u = 1.0f - t;
        return 1.0f - u * u * u;
    }

    private void restoreRenderState() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    // ─── Particle (Cubes) ─────────────────────────────────────────────────────

    public static class Particle {
        double x, y, z;
        long time;
        LivingEntity entity;

        public Particle(LivingEntity entity, double x, double y, double z) {
            this.entity = entity;
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = System.currentTimeMillis();
        }

        public void update(float dt) {
            this.y += MathUtil.random(0.01, 0.04) * (dt * 60);
        }

        public void renderCube(MatrixStack ms, Camera camera, int colorInt, float partialTicks) {
            if (entity == null) return;
            double alive = System.currentTimeMillis() - time;
            float life = Math.min(1f, (float) alive / 1000f);
            float alpha = life > 0.8f ? 1f - (life - 0.8f) * 5f : (alive < 200 ? (float) alive / 200f : 1f);
            if (alpha <= 0.001f) return;

            Vec3d cam = camera.getPos();
            double ex = MathHelper.lerp(partialTicks, entity.lastRenderX, entity.getX());
            double ey = MathHelper.lerp(partialTicks, entity.lastRenderY, entity.getY());
            double ez = MathHelper.lerp(partialTicks, entity.lastRenderZ, entity.getZ());

            float scale = 0.12f;
            int color = ColorProvider.setAlpha(colorInt, (int)(alpha * 255));

            ms.push();
            ms.translate(ex - cam.x + x, ey - cam.y + y, ez - cam.z + z);
            double rot = alive / 10.0;
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rot));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rot));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rot));
            ms.scale(scale, scale, scale);

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

            drawBox(ms, color);

            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            ms.pop();
        }

        public boolean renderBloom(BufferBuilder builder, MatrixStack ms, Camera camera, int colorInt,
                                 float camYaw, float camPitch, float partialTicks) {
            if (entity == null) return false;
            double alive = System.currentTimeMillis() - time;
            float life = Math.min(1f, (float) alive / 1000f);
            float alpha = life > 0.8f ? 1f - (life - 0.8f) * 5f : (alive < 200 ? (float) alive / 200f : 1f);
            if (alpha <= 0.001f) return false;

            float r = ColorProvider.red(colorInt) / 255f;
            float g = ColorProvider.green(colorInt) / 255f;
            float b = ColorProvider.blue(colorInt) / 255f;

            Vec3d cam = camera.getPos();
            double ex = MathHelper.lerp(partialTicks, entity.lastRenderX, entity.getX());
            double ey = MathHelper.lerp(partialTicks, entity.lastRenderY, entity.getY());
            double ez = MathHelper.lerp(partialTicks, entity.lastRenderZ, entity.getZ());

            ms.push();
            ms.translate(ex - cam.x + x, ey - cam.y + y, ez - cam.z + z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camYaw));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camPitch));
            ms.scale(0.9f, 0.9f, 0.9f);

            Matrix4f m = ms.peek().getPositionMatrix();
            int ai = (int)(alpha * 0.12f * 255);
            int ri = (int)(r * 255), gi = (int)(g * 255), bi = (int)(b * 255);

            builder.vertex(m, -0.5f,  0.5f, 0).texture(0f, 1f).color(ri, gi, bi, ai);
            builder.vertex(m,  0.5f,  0.5f, 0).texture(1f, 1f).color(ri, gi, bi, ai);
            builder.vertex(m,  0.5f, -0.5f, 0).texture(1f, 0f).color(ri, gi, bi, ai);
            builder.vertex(m, -0.5f, -0.5f, 0).texture(0f, 0f).color(ri, gi, bi, ai);
            ms.pop();
            return true;
        }

        private void drawBox(MatrixStack ms, int color) {
            float min = -0.5f, max = 0.5f;
            float r = ColorProvider.red(color) / 255f;
            float g = ColorProvider.green(color) / 255f;
            float b = ColorProvider.blue(color) / 255f;
            float a = ColorProvider.alpha(color) / 255f;
            float lineA = a / 4f, fillA = a / 12f;

            BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            Matrix4f m = ms.peek().getPositionMatrix();
            addLine(buf, m, min, min, min, max, min, min, r, g, b, lineA);
            addLine(buf, m, min, max, min, max, max, min, r, g, b, lineA);
            addLine(buf, m, min, min, max, max, min, max, r, g, b, lineA);
            addLine(buf, m, min, max, max, max, max, max, r, g, b, lineA);
            addLine(buf, m, min, min, min, min, max, min, r, g, b, lineA);
            addLine(buf, m, max, min, min, max, max, min, r, g, b, lineA);
            addLine(buf, m, min, min, max, min, max, max, r, g, b, lineA);
            addLine(buf, m, max, min, max, max, max, max, r, g, b, lineA);
            addLine(buf, m, min, min, min, min, min, max, r, g, b, lineA);
            addLine(buf, m, max, min, min, max, min, max, r, g, b, lineA);
            addLine(buf, m, min, max, min, min, max, max, r, g, b, lineA);
            addLine(buf, m, max, max, min, max, max, max, r, g, b, lineA);
            BufferRenderer.drawWithGlobalProgram(buf.end());

            BufferBuilder fb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            addFace(fb, m, min, min, min, max, min, max, r, g, b, fillA);
            addFace(fb, m, min, max, min, max, max, max, r, g, b, fillA);
            addFace(fb, m, min, min, min, max, max, min, r, g, b, fillA);
            addFace(fb, m, min, min, max, max, max, max, r, g, b, fillA);
            addFace(fb, m, min, min, min, min, max, max, r, g, b, fillA);
            addFace(fb, m, max, min, min, max, max, max, r, g, b, fillA);
            BufferRenderer.drawWithGlobalProgram(fb.end());
        }

        private void addLine(BufferBuilder buf, Matrix4f m,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float r, float g, float b, float a) {
            buf.vertex(m, x1, y1, z1).color(r, g, b, a);
            buf.vertex(m, x2, y2, z2).color(r, g, b, a);
        }

        private void addFace(BufferBuilder buf, Matrix4f m,
                              float x1, float y1, float z1, float x2, float y2, float z2,
                              float r, float g, float b, float a) {
            buf.vertex(m, x1, y1, z1).color(r, g, b, a);
            buf.vertex(m, x2, y1, z1).color(r, g, b, a);
            buf.vertex(m, x2, y1, z2).color(r, g, b, a);
            buf.vertex(m, x1, y1, z2).color(r, g, b, a);

            buf.vertex(m, x1, y2, z1).color(r, g, b, a);
            buf.vertex(m, x1, y2, z2).color(r, g, b, a);
            buf.vertex(m, x2, y2, z2).color(r, g, b, a);
            buf.vertex(m, x2, y2, z1).color(r, g, b, a);

            buf.vertex(m, x1, y1, z1).color(r, g, b, a);
            buf.vertex(m, x1, y2, z1).color(r, g, b, a);
            buf.vertex(m, x2, y2, z1).color(r, g, b, a);
            buf.vertex(m, x2, y1, z1).color(r, g, b, a);

            buf.vertex(m, x1, y1, z2).color(r, g, b, a);
            buf.vertex(m, x2, y1, z2).color(r, g, b, a);
            buf.vertex(m, x2, y2, z2).color(r, g, b, a);
            buf.vertex(m, x1, y2, z2).color(r, g, b, a);

            buf.vertex(m, x1, y1, z1).color(r, g, b, a);
            buf.vertex(m, x1, y1, z2).color(r, g, b, a);
            buf.vertex(m, x1, y2, z2).color(r, g, b, a);
            buf.vertex(m, x1, y2, z1).color(r, g, b, a);

            buf.vertex(m, x2, y1, z1).color(r, g, b, a);
            buf.vertex(m, x2, y2, z1).color(r, g, b, a);
            buf.vertex(m, x2, y2, z2).color(r, g, b, a);
            buf.vertex(m, x2, y1, z2).color(r, g, b, a);
        }
    }
}
