package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.AppearanceTraitRenderer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Textured hair shells with bounded secondary movement and separate head/torso attachments. */
public final class HairTraitRenderer implements AppearanceTraitRenderer {
    public enum Style {LAYERED, TIED, SWEPT, TOUSLED, WAVY, CROPPED}
    private static final TraitGeometry G = TraitGeometry.INSTANCE;
    private static final RenderType MATERIAL = RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath("coi-client", "textures/entity/hair_strands.png"));
    private final java.util.Map<net.minecraft.world.entity.player.Player, Motion> motions = new java.util.WeakHashMap<>();
    private static final class Motion {
        float age, yaw;
        double x, y, z;
        final float[] offset = new float[81], velocity = new float[81];
    }
    private float[] motion(AvatarRenderState state) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        var entity = level == null ? null : level.getEntity(state.id);
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) return new float[81];
        Motion m = motions.computeIfAbsent(player, ignored -> new Motion());
        float dt = state.ageInTicks - m.age;
        double dx = state.x-m.x, dy=state.y-m.y, dz=state.z-m.z;
        if (m.age == 0 || dt < 0 || dt > 20 || dx*dx+dy*dy+dz*dz > 16) {
            java.util.Arrays.fill(m.offset, 0); java.util.Arrays.fill(m.velocity, 0);
        } else if (dt > .0001f) {
            double yaw = Math.toRadians(state.bodyRot);
            float turn = net.minecraft.util.Mth.wrapDegrees(state.bodyRot+state.yRot-m.yaw)/dt;
            float[] target = {
                Math.clamp((float)((dx*Math.cos(yaw)+dz*Math.sin(yaw))/dt)*14-turn*.09f,-2.5f,2.5f),
                Math.clamp((float)(dy/dt)*-8,-1.4f,1.4f),
                Math.clamp((float)((dz*Math.cos(yaw)-dx*Math.sin(yaw))/dt)*16+state.walkAnimationSpeed*3,0,6)
            };
            int steps = Math.max(1,(int)Math.ceil(dt*4));
            float h = dt/steps;
            for(int step=0;step<steps;step++) for(int lock=0;lock<9;lock++) for(int joint=0;joint<3;joint++) for(int axis=0;axis<3;axis++) {
                int index=lock*9+joint*3+axis;
                float goal=joint==0?target[axis]*.3f:m.offset[index-3]*1.35f+target[axis]*.35f;
                float stiffness=.13f+(lock%3)*.012f,damping=.25f-joint*.025f;
                m.velocity[index] += ((goal-m.offset[index])*stiffness-m.velocity[index]*damping)*h;
                m.offset[index] = Math.clamp(m.offset[index]+m.velocity[index]*h,-6,6);
            }
        }
        if(dt != 0) {
            m.age=state.ageInTicks; m.x=state.x; m.y=state.y; m.z=state.z;
            m.yaw=state.bodyRot+state.yRot;
        }
        return m.offset.clone();
    }
    private final String traitId;
    private final Style style;
    private final TraitGeometry.Tint base, highlight;

    public HairTraitRenderer(String traitId, Style style, float red, float green, float blue) {
        this.traitId = traitId;
        this.style = style;
        float lift = Math.max(red, Math.max(green, blue)) < .06f ? .09f : 0;
        base = new TraitGeometry.Tint(red + lift, green + lift, blue + lift, 1);
        highlight = new TraitGeometry.Tint(Math.min(1, base.r() * 1.35f + .025f),
                Math.min(1, base.g() * 1.35f + .025f), Math.min(1, base.b() * 1.35f + .025f), 1);
    }

    @Override public String traitId() { return traitId; }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, AvatarRenderState state, PlayerModel model) {
        var settings = AppearanceConfig.get();
        int light = state.lightCoords;
        float[] sway = motion(state);
        float stride = Math.clamp(state.walkAnimationSpeed,0,1);
        stack.pushPose();
        model.head.translateAndRotate(stack);
        stack.translate(0, settings.hairYOffsetPixels / 16, 0);
        collector.order(1).submitCustomGeometry(stack, MATERIAL, (pose, consumer) -> head(pose, consumer, light, sway));
        stack.popPose();
        if (style == Style.LAYERED || style == Style.TIED) {
            stack.pushPose();
            model.body.translateAndRotate(stack);
            stack.translate(0, settings.hairYOffsetPixels / 16, 0);
            collector.order(1).submitCustomGeometry(stack, MATERIAL, (pose, consumer) -> locks(pose, consumer, light, sway, stride, settings.hairLength));
            stack.popPose();
        }
    }

    private void head(PoseStack.Pose p, VertexConsumer c, int light, float[] sway) {
        boolean cropped=style==Style.CROPPED;
        float crown=style==Style.TOUSLED?-.18f:style==Style.WAVY?-.12f:0;
        // A connected scalp and broad fringe keep the silhouette quiet and the eyes clear.
        box(p,c,-4.25f,-8.3f+crown,-4.23f,4.25f,-6.25f,4.3f,base,light);
        float end=cropped?-2.8f:style==Style.TOUSLED?1.4f:style==Style.WAVY?1.0f:.35f;
        box(p,c,-4.3f,-6.5f,3.8f,4.3f,end,4.48f,base,light);
        float leftEnd=style==Style.SWEPT?-1.6f:style==Style.TOUSLED?-1.1f:cropped?-3.2f:-.25f;
        float rightEnd=style==Style.SWEPT?-3.1f:style==Style.TOUSLED?-1.5f:leftEnd;
        box(p,c,-4.38f,-6.6f,-2.9f,-3.83f,leftEnd,4.3f,base,light);
        box(p,c,3.83f,-6.6f,-2.9f,4.38f,rightEnd,4.3f,base,light);
        float movement=Math.clamp(sway[7]*.12f,-.18f,.18f);
        float tipX=Math.clamp(sway[6]*.1f,-.25f,.25f),tipZ=Math.clamp(sway[8]*.08f,0,.3f);
        float left=cropped?-6.2f:style==Style.SWEPT?-4.95f:style==Style.TOUSLED?-6.1f:-5.5f;
        float right=cropped?-6.2f:style==Style.SWEPT?-6.0f:-5.8f;
        quad(p,c,-4.15f,-6.6f,-4.37f,-4.15f+tipX,left+movement,-4.37f-tipZ,.65f+tipX,left+.3f+movement,-4.37f-tipZ,.65f,-6.6f,-4.37f,base,light,false);
        quad(p,c,.55f,-6.6f,-4.36f,.55f+tipX,right-.2f+movement,-4.36f-tipZ,4.15f+tipX,right+movement,-4.36f-tipZ,4.15f,-6.6f,-4.36f,base,light,false);
        if(style==Style.SWEPT || style==Style.TOUSLED) {
            // Low, broad overlapping sections replace the former pointed roof-like tufts.
            float lift=style==Style.SWEPT?.22f:.12f;
            box(p,c,-3.9f,-8.45f-lift,-3.8f,.8f,-8.15f,3.9f,base,light);
            box(p,c,.6f,-8.4f,-3.4f,3.95f,-8.1f,4.0f,base,light);
        }
        if(style==Style.WAVY || style==Style.LAYERED) {
            for(int side=-1;side<=1;side+=2) for(int i=0;i<3;i++) {
                float x=side<0?-4.43f:3.87f,z=-2.8f+i*2.2f;
                float hem=(style==Style.WAVY?.9f:.3f)+(float)Math.sin(i*1.7)*.25f+sway[7]*.08f;
                box(p,c,x,-2,z,x+.56f,hem,z+2.3f,base,light);
            }
        }
    }

    private record Point(float x,float y,float z) {}

    private void locks(PoseStack.Pose p, VertexConsumer c, int light, float[] motion, float stride, float length) {
        if (style == Style.TIED) ponytail(p,c,light,motion,stride,length);
        else layeredChunks(p,c,light,motion,stride,length);
    }

    private void layeredChunks(PoseStack.Pose p, VertexConsumer c, int light, float[] motion, float stride, float length) {
        // A shallow shoulder mantle joins the scalp to three broad, blunt lower layers.
        box(p,c,-3.65f,.15f,3.95f,3.65f,3.0f,5.28f,base,light);
        float[] centers={-2.35f,0,2.35f}, widths={2.65f,2.8f,2.65f}, ends={9.5f,10.7f,9.8f};
        for(int lock=0;lock<3;lock++) {
            float end=ends[lock]*length, root=1.8f;
            for(int section=0;section<3;section++) {
                float t=section/3f,u=(section+1)/3f,w=widths[lock]-(section==2?.35f:0);
                float y=root+(end-root)*t+bend(motion,lock*3,t,1)*t*.35f;
                float yy=root+(end-root)*u+bend(motion,lock*3,u,1)*u*.35f;
                float x=Math.clamp(centers[lock]+bend(motion,lock*3,t,0)*.35f,-3.65f+w/2,3.65f-w/2);
                float xx=Math.clamp(centers[lock]+bend(motion,lock*3,u,0)*.35f,-3.65f+w/2,3.65f-w/2);
                float z=4.05f+Math.max(0,bend(motion,lock*3,t,2))*.55f+Math.max(0,y-9)*stride*.85f;
                float zz=4.05f+Math.max(0,bend(motion,lock*3,u,2))*.55f+Math.max(0,yy-9)*stride*.85f;
                chunk(p,c,light,new Point(x,y-.06f,z),new Point(xx,yy+.06f,zz),w);
            }
        }
    }

    private void chunk(PoseStack.Pose p,VertexConsumer c,int light,Point a,Point b,float width) {
        float w=width/2,d=1.05f;
        quad(p,c,a.x-w,a.y,a.z+d,a.x+w,a.y,a.z+d,b.x+w,b.y,b.z+d,b.x-w,b.y,b.z+d,base,light,true);
        quad(p,c,a.x+w,a.y,a.z,a.x-w,a.y,a.z,b.x-w,b.y,b.z,b.x+w,b.y,b.z,base,light,true);
        quad(p,c,a.x-w,a.y,a.z,a.x-w,a.y,a.z+d,b.x-w,b.y,b.z+d,b.x-w,b.y,b.z,base,light,true);
        quad(p,c,a.x+w,a.y,a.z+d,a.x+w,a.y,a.z,b.x+w,b.y,b.z,b.x+w,b.y,b.z+d,base,light,true);
        quad(p,c,a.x-w,a.y,a.z,a.x+w,a.y,a.z,a.x+w,a.y,a.z+d,a.x-w,a.y,a.z+d,base,light,true);
        quad(p,c,b.x-w,b.y,b.z+d,b.x+w,b.y,b.z+d,b.x+w,b.y,b.z,b.x-w,b.y,b.z,base,light,true);
    }

    private Point tailPoint(float[] motion,int side,float t,float stride,float length) {
        double angle=side*Math.PI/4;
        float width=1.7f+.8f*(float)Math.sin(Math.PI*t),depth=.9f+.35f*(float)Math.sin(Math.PI*t);
        float y=-.8f+11*t*length+bend(motion,4,t,1)*t*.4f;
        float x=(float)Math.cos(angle)*width+bend(motion,4,t,0)*t*.4f;
        float z=4.7f+(float)Math.sin(angle)*depth+Math.max(0,bend(motion,4,t,2))*t*.8f+Math.max(0,y-9)*stride*.85f;
        return new Point(Math.clamp(x,-3.6f,3.6f),y,z);
    }

    private void ponytail(PoseStack.Pose p,VertexConsumer c,int light,float[] motion,float stride,float length) {
        box(p,c,-1.45f,-1.1f,4,1.45f,.25f,5.55f,base,light);
        box(p,c,-1.55f,-1.35f,4.0f,1.55f,-.95f,5.65f,highlight,light);
        for(int side=0;side<8;side++) {
            Point left=tailPoint(motion,side,0,stride,length),right=tailPoint(motion,side+1,0,stride,length);
            for(int segment=1;segment<=7;segment++) {
                float t=segment/7f;
                Point nextLeft=tailPoint(motion,side,t,stride,length),nextRight=tailPoint(motion,side+1,t,stride,length);
                quad(p,c,right.x,right.y,right.z,left.x,left.y,left.z,nextLeft.x,nextLeft.y,nextLeft.z,nextRight.x,nextRight.y,nextRight.z,base,light,true);
                left=nextLeft;right=nextRight;
            }
            Point center=tailPoint(motion,0,1,stride,length),opposite=tailPoint(motion,4,1,stride,length);
            float cx=(center.x+opposite.x)/2,cz=(center.z+opposite.z)/2;
            quad(p,c,right.x,right.y,right.z,left.x,left.y,left.z,cx,left.y,cz,cx,left.y,cz,base,light,true);
        }
    }

    private static float bend(float[] motion,int lock,float t,int axis) {
        float position=t*3;
        int segment=Math.min(2,(int)position);
        float from=segment==0?0:motion[lock*9+(segment-1)*3+axis];
        float to=motion[lock*9+segment*3+axis];
        return from+(to-from)*(position-segment);
    }

    private static void box(PoseStack.Pose p,VertexConsumer c,float x0,float y0,float z0,
                            float x1,float y1,float z1,TraitGeometry.Tint t,int light) {
        quad(p,c,x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,t,light,false);
        quad(p,c,x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0,t,light,false);
        quad(p,c,x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1,t,light,false);
        quad(p,c,x0,y0,z0,x0,y0,z1,x1,y0,z1,x1,y0,z0,t,light,false);
        quad(p,c,x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1,t,light,false);
        quad(p,c,x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,t,light,false);
    }

    private static void quad(PoseStack.Pose p,VertexConsumer c,
                             float ax,float ay,float az,float bx,float by,float bz,
                             float cx,float cy,float cz,float dx,float dy,float dz,
                             TraitGeometry.Tint t,int light,boolean drape) {
        float ux=bx-ax,uy=by-ay,uz=bz-az,vx=cx-ax,vy=cy-ay,vz=cz-az;
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(length<.0001f)return;
        nx/=length;ny/=length;nz/=length;
        vertex(p,c,ax,ay,az,nx,ny,nz,t,light,drape);
        vertex(p,c,bx,by,bz,nx,ny,nz,t,light,drape);
        vertex(p,c,cx,cy,cz,nx,ny,nz,t,light,drape);
        vertex(p,c,dx,dy,dz,nx,ny,nz,t,light,drape);
    }

    private static void vertex(PoseStack.Pose p,VertexConsumer c,float x,float y,float z,
                               float nx,float ny,float nz,TraitGeometry.Tint t,int light,boolean drape) {
        float u=drape?(x+4.3f)/8.6f:(Math.abs(nx)>.5f?z+4.5f:x+4.5f)/9;
        float v=drape?(y+2)/20:(Math.abs(ny)>.5f?(z+4.5f)/9:(y+9)/10);
        G.addVertex(p,c,x/16,y/16,z/16,t.r(),t.g(),t.b(),t.a(),u,v,nx,ny,nz,light);
    }
}
