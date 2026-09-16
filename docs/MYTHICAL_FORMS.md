# Mythical Creature Forms System

This document outlines the architecture, network protocol, rendering pipeline, and existing implementations of the Mythical Creature Forms system in the COI Client mod. Use this guide to understand how forms are registered, processed, and rendered, and how to improve their aesthetics.

---

## 1. Project Overview & Architecture

The Mythical Creature Forms system allows players to transform into mythical beings based on their pathway (as described in `mythical_creatures.json`). The server controls transformations, and the client handles all procedural rendering.

### Key Classes
*   **[`MythicalCreatureForm.java`](file:///F:/Mysterria/coi-client/src/client/java/dev/ua/ikeepcalm/coi/client/effects/MythicalCreatureForm.java)**: The base interface for all forms. Contains default methods `drawBox` and `addVertex` for drawing 3D geometry.
*   **[`MythicalFormManager.java`](file:///F:/Mysterria/coi-client/src/client/java/dev/ua/ikeepcalm/coi/client/effects/MythicalFormManager.java)**: The registry that manages active transformations per player. Handles key normalization (spaces, underscores, casing) and dispatches rendering to the active form.
*   **[`PlayerRendererMixin.java`](file:///F:/Mysterria/coi-client/src/client/java/dev/ua/ikeepcalm/coi/mixin/PlayerRendererMixin.java)**: Intercepts `LivingEntityRenderer`'s `submit` method. If the entity is a player with an active form, it cancels vanilla rendering and submits custom geometry.
*   **[`EffectDebugScreen.java`](file:///F:/Mysterria/coi-client/src/client/java/dev/ua/ikeepcalm/coi/client/screen/EffectDebugScreen.java)**: Developer debug screen (opened via F8 in dev environment) that contains a dynamic cycling button to test all 20 registered forms on the local player.

---

## 2. Rendering Pipeline (Minecraft 1.21.2+)

In Minecraft 1.21.2+, the traditional `render(...)` model in entity renderers is replaced with a deferred, state-based system using `SubmitNodeCollector`.

### Interception & Matrix Rotations
The custom rendering starts in [`PlayerRendererMixin.java`](file:///F:/Mysterria/coi-client/src/client/java/dev/ua/ikeepcalm/coi/mixin/PlayerRendererMixin.java):
1.  **Gaze Alignment**: Before rendering the custom form, the mixin rotates the `PoseStack` around the Y-axis using `state.bodyRot` (which holds the player's head/camera look direction):
    ```java
    poseStack.pushPose();
    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - state.bodyRot));
    ```
2.  **Custom Geometry Submission**: The geometry is submitted using a white texture `coi-client:textures/entity/white.png` (translucent color overlays are applied per vertex):
    ```java
    collector.order(0).submitCustomGeometry(
        poseStack,
        RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath("coi-client", "textures/entity/white.png")),
        (pose, consumer) -> MythicalFormManager.renderFormSubmit(form, avatarState, pose, consumer)
    );
    ```
3.  **Nameplate Preservation**: The mixin invokes `callSubmitNameDisplay` to ensure player name tags remain visible above the custom form, then cancels vanilla player model rendering with `ci.cancel()`.

---

## 3. Creating & Modifying Forms

All forms are located in the package [`dev.ua.ikeepcalm.coi.client.effects.forms`](file:///F:/Mysterria/coi-client/src/client/java/dev/ua/ikeepcalm/coi/client/effects/forms) and implement `MythicalCreatureForm`.

### Draw Helpers
The `MythicalCreatureForm` interface provides:
*   `drawBox(pose, consumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, alpha, light)`: Draws a 3D box at local coordinates with custom colors (0.0f - 1.0f) and translucency.
*   `addVertex(pose, consumer, x, y, z, r, g, b, alpha, u, v, nx, ny, nz, light)`: Directly feeds vertices to the buffer.

### Boilerplate Example
```java
package dev.ua.ikeepcalm.coi.client.effects.forms;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalCreatureForm;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class MyNewForm implements MythicalCreatureForm {
    @Override
    public String getPathwayName() {
        return "My New Form";
    }

    @Override
    public void render(AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        float time = state.ageInTicks;
        int light = state.lightCoords;
        // Render shapes using drawBox...
    }
}
```

---

## 4. Guide for Improving Aesthetics

When optimizing the visual appearance of these forms, consider the following parameters:
1.  **Colors**: Keep them vibrant and aligned with their pathway descriptors (e.g., `Fool` = purple/indigo, `Tyrant` = lightning cyan, `Sun` = blazing gold, `Death` = green gas/bone white).
2.  **Opacity**: Adjust the alpha channels (e.g. `0.2f` for glassmorphic elements, `0.95f` for dense core shadows) to make shapes feel dimensional and ethereal.
3.  **Procedural Math**: Add more segments or sub-motions. You can introduce multiple sine/cosine frequencies in the `time` parameter inside individual form classes to avoid repetitive patterns.
4.  **Density**: Adjust loops (like `maggotCount`, `particleCount`, `gear teeth`) for fuller volumes, but keep CPU calculations cheap to preserve client-side frames.
