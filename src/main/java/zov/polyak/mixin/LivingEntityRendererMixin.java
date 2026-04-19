package zov.polyak.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zov.polyak.module.list.combat.KillAura;
import zov.polyak.module.list.render.Chams;
import zov.polyak.util.IMinecraft;
import zov.polyak.util.base.Instance;
import zov.polyak.util.math.RotationUtil;
import zov.polyak.util.player.combat.PredictUtils;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> implements FeatureRendererContext<S, M>, IMinecraft {

    @Shadow
    private static float clampBodyYaw(LivingEntity entity, float degrees, float tickDelta) {
        return 0;
    }

    @Shadow
    public static boolean shouldFlipUpsideDown(LivingEntity entity) {
        return false;
    }

    protected LivingEntityRendererMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    
    @Overwrite
    public void updateRenderState(T livingEntity, S livingEntityRenderState, float f) {
        super.updateRenderState(livingEntity, livingEntityRenderState, f);

        float yaw = MathHelper.lerpAngleDegrees(f, livingEntity.prevHeadYaw, livingEntity.headYaw);

        livingEntityRenderState.bodyYaw = clampBodyYaw(livingEntity, yaw, f);
        livingEntityRenderState.yawDegrees = MathHelper.wrapDegrees(yaw - livingEntityRenderState.bodyYaw);
        livingEntityRenderState.pitch = livingEntity.getLerpedPitch(f);

        livingEntityRenderState.customName = livingEntity.getCustomName();
        livingEntityRenderState.flipUpsideDown = shouldFlipUpsideDown(livingEntity);

        if (livingEntityRenderState.flipUpsideDown) {
            livingEntityRenderState.pitch *= -1.0F;
            livingEntityRenderState.yawDegrees *= -1.0F;
        }

        if (!livingEntity.hasVehicle() && livingEntity.isAlive()) {
            livingEntityRenderState.limbFrequency = livingEntity.limbAnimator.getPos(f);
            livingEntityRenderState.limbAmplitudeMultiplier = livingEntity.limbAnimator.getSpeed(f);
        } else {
            livingEntityRenderState.limbFrequency = 0.0F;
            livingEntityRenderState.limbAmplitudeMultiplier = 0.0F;
        }

        if (livingEntity.getVehicle() instanceof LivingEntity livingEntity2)
            livingEntityRenderState.headItemAnimationProgress = livingEntity2.limbAnimator.getPos(f);
        else livingEntityRenderState.headItemAnimationProgress = livingEntityRenderState.limbFrequency;

        livingEntityRenderState.baseScale = livingEntity.getScale();
        livingEntityRenderState.ageScale = livingEntity.getScaleFactor();
        livingEntityRenderState.pose = livingEntity.getPose();
        livingEntityRenderState.sleepingDirection = livingEntity.getSleepingDirection();
        if (livingEntityRenderState.sleepingDirection != null)
            livingEntityRenderState.standingEyeHeight = livingEntity.getEyeHeight(EntityPose.STANDING);
        livingEntityRenderState.shaking = livingEntity.isFrozen();
        livingEntityRenderState.baby = livingEntity.isBaby();
        livingEntityRenderState.touchingWater = livingEntity.isTouchingWater();
        livingEntityRenderState.usingRiptide = livingEntity.isUsingRiptide();
        livingEntityRenderState.hurt = livingEntity.hurtTime > 0 || livingEntity.deathTime > 0;
        livingEntityRenderState.deathTime = livingEntity.deathTime > 0 ? (float) livingEntity.deathTime + f : 0.0F;
        livingEntityRenderState.invisibleToPlayer = livingEntityRenderState.invisible && livingEntity.isInvisibleTo(MinecraftClient.getInstance().player);
        livingEntityRenderState.hasOutline = MinecraftClient.getInstance().hasOutline(livingEntity);

        
        if (livingEntity == mc.player && livingEntity.isGliding()) {
            KillAura killAura = Instance.get(KillAura.class);

            if (killAura != null && killAura.isEnabled() && killAura.visualElytraRotation.getValue()) {
                LivingEntity target = killAura.getTarget();

                if (target != null && target.isAlive() && target.isGliding()) {
                    
                    Vec3d playerPos = livingEntity.getLerpedPos(f);
                    Vec3d targetPos = target.getLerpedPos(f);

                    
                    Vec3d targetLook = target.getRotationVec(f).normalize();
                    
                    Vec3d targetToPlayer = playerPos.subtract(targetPos);

                    
                    
                    
                    double dot = targetToPlayer.dotProduct(targetLook);

                    
                    Vec3d predict = PredictUtils.predict(target, killAura.predictValue.getValue());
                    double distToPredict = playerPos.distanceTo(predict);

                    
                    
                    
                    
                    if (dot > 0.0 && distToPredict < 6.0) {

                        Vec3d center = targetPos.add(0.0, target.getHeight() / 2.0, 0.0);
                        Vec2f rotation = RotationUtil.calculate(center);

                        livingEntityRenderState.bodyYaw = rotation.x;
                        livingEntityRenderState.yawDegrees = 0.0F;
                        livingEntityRenderState.pitch = rotation.y;
                    }
                }
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (state instanceof PlayerEntityRenderState) {
            Chams chams = Instance.get(Chams.class);
            if (chams != null && chams.isEnabled()) {
                ci.cancel();
            }
        }
    }
}