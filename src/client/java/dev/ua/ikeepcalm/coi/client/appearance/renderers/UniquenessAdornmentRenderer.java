package dev.ua.ikeepcalm.coi.client.appearance.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.client.appearance.TraitGeometry;
import dev.ua.ikeepcalm.coi.client.appearance.UniquenessParticleManager;
import dev.ua.ikeepcalm.coi.client.ClientAppearanceState;
import dev.ua.ikeepcalm.coi.client.mcf.AvatarRenderStateAccessor;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Frame-bound particle signatures: their shapes clear with appearance state and cannot accumulate. */
public final class UniquenessAdornmentRenderer {
    private static final TraitGeometry G = TraitGeometry.INSTANCE;
    private UniquenessAdornmentRenderer() {}

    public static void submit(String pathway, PoseStack stack, SubmitNodeCollector collector,
                              AvatarRenderState state, PlayerModel model) {
        if (pathway.equals("death") || pathway.equals("tyrant") || state.distanceToCameraSq > 48 * 48) return;
        float time = state.ageInTicks;
        String uuid = ((AvatarRenderStateAccessor) state).coi$getPlayerUuid();
        int idle = UniquenessParticleManager.idleTicks(uuid);
        String authority = "";
        int moonPhase = 0;
        for (String trait : ClientAppearanceState.getTraits(uuid)) {
            if (trait.startsWith("authority:")) authority = trait.substring(10);
            if (trait.startsWith("moon-phase:") && trait.length() == 12
                    && trait.charAt(11) >= '0' && trait.charAt(11) <= '7') moonPhase = trait.charAt(11) - '0';
        }
        String rule = authority;
        int phase = moonPhase;
        boolean holder = pathway.equals(UniquenessParticleManager.resolvePathway(uuid, ClientAppearanceState.getTraits(uuid)))
                && dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.shouldRenderUniqueness(uuid);
        var movement = UniquenessParticleManager.movement(uuid, time-(float)Math.floor(time));
        PoseStack bodyPose=new PoseStack();model.body.translateAndRotate(bodyPose);
        Matrix4f attachment=new Matrix4f(bodyPose.last().pose());
        float groundY=state.isCrouching?21.8f:23.8f;
        double spacing = .48 * Math.clamp(Math.sqrt(state.distanceToCameraSq) / 8, 1, 3);
        stack.pushPose();
        // Restrained breathing drift keeps every idle motif visibly alive.
        double driftPhase = time * .018 + pathway.hashCode() * .0007;
        stack.translate(Math.sin(driftPhase) * .012, Math.sin(driftPhase * .73) * .006, Math.cos(driftPhase * .61) * .010);
        if(pathway.equals("emperor") && holder) model.head.translateAndRotate(stack);
        else if(!pathway.equals("abyss") && !(pathway.equals("fool") && holder) && !pathway.equals("door") && !pathway.equals("emperor")) model.body.translateAndRotate(stack);
        collector.order(4).submitCustomGeometry(stack, RenderTypes.entityTranslucent(TraitRenderSupport.WHITE_TEXTURE),
                (pose, consumer) -> new Motif(pose, consumer, time, spacing, idle, rule, phase, movement, holder, attachment, groundY).draw(pathway));
        stack.popPose();
        if(holder && (pathway.equals("mother") || pathway.equals("chained"))) {
            for(int limb=0;limb<2;limb++) {
                boolean left=limb==0;
                stack.pushPose();
                if(pathway.equals("mother")) (left?model.leftLeg:model.rightLeg).translateAndRotate(stack);
                else (left?model.leftArm:model.rightArm).translateAndRotate(stack);
                collector.order(4).submitCustomGeometry(stack,RenderTypes.entityTranslucent(TraitRenderSupport.WHITE_TEXTURE),
                        (pose,consumer) -> {
                            var motif=new Motif(pose,consumer,time,spacing,idle,rule,phase,movement,true,attachment,groundY);
                            if(pathway.equals("mother"))motif.legVine(left);else motif.armChain(left);
                        });
                stack.popPose();
            }
        }
        if(holder && ClientAppearanceState.hasTrait(uuid,"ability:"+pathway)) {
            stack.pushPose();
            if(!pathway.equals("emperor") && !pathway.equals("abyss") && !pathway.equals("door"))model.body.translateAndRotate(stack);
            collector.order(4).submitCustomGeometry(stack, RenderTypes.entityTranslucent(TraitRenderSupport.WHITE_TEXTURE),
                    (pose,consumer) -> new Motif(pose,consumer,time,spacing,idle,rule,phase,movement,false,attachment,groundY).draw(pathway));
            stack.popPose();
        }
    }

    /** Body-local pixels. Signatures occupy short-lived strokes rather than complete solid emblems. */
    private record Motif(PoseStack.Pose pose, VertexConsumer consumer, float time, double spacing, int idle, String authority, int moonPhase, UniquenessParticleManager.Movement movement, boolean holder, Matrix4f attachment, float groundY) {
        private static final double TAU=Math.PI*2;
        private double cycle(double speed,double phase) { return (time*speed+phase)%1; }
        private double envelope(double t) { return Math.pow(Math.sin(Math.PI*t),2); }
        private void spark(double x,double y,double z,double size,int rgb,double alpha) {
            float intensity=dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity;
            float opacity=(float)Math.clamp(Math.sqrt(Math.max(0,alpha))*.8*Math.sqrt(intensity),0,.85);
            if(opacity<.018f)return;
            float r=((rgb>>16)&255)/255f,g=((rgb>>8)&255)/255f,b=(rgb&255)/255f;
            float px=(float)x/16,py=(float)y/16,pz=(float)z/16,half=(float)size/16;
            G.drawBox(pose,consumer,px-half,py-half,pz-half,px+half,py+half,pz+half,r,g,b,opacity*.08f,TraitRenderSupport.FULL_BRIGHT);
            half*=.55f;
            G.drawBox(pose,consumer,px-half,py-half,pz-half,px+half,py+half,pz+half,r,g,b,opacity,TraitRenderSupport.FULL_BRIGHT);
        }
        private void line(double x,double y,double z,double xx,double yy,double zz,int rgb,double alpha) {
            double dx=xx-x,dy=yy-y,dz=zz-z;
            int n=Math.max(1,(int)Math.ceil(Math.sqrt(dx*dx+dy*dy+dz*dz)/spacing));
            for(int i=0;i<=n;i++) {
                double t=i/(double)n;
                spark(x+dx*t,y+dy*t,z+dz*t,.32,rgb,alpha);
            }
        }
        private void panel(double left,double top,double right,double bottom,double near,double far,int rgb,double alpha) {
            if(right-left<.001)return;
            float intensity=dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity;
            var tint=new TraitGeometry.Tint(((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,
                    (float)(alpha*Math.sqrt(intensity)));
            G.quad(pose,consumer,G.pointPixels((float)left,(float)top,(float)near),
                    G.pointPixels((float)right,(float)top,(float)far),
                    G.pointPixels((float)right,(float)bottom,(float)far),
                    G.pointPixels((float)left,(float)bottom,(float)near),tint,TraitRenderSupport.FULL_BRIGHT);
        }
        private void arc(double x,double y,double z,double rx,double ry,double start,double length,int rgb,double alpha) {
            int n=Math.max(4,(int)(Math.abs(length)*Math.max(rx,ry)/spacing));
            for(int i=0;i<=n;i++) {
                double t=i/(double)n,a=start+t*length;
                spark(x+Math.cos(a)*rx,y+Math.sin(a)*ry,z+Math.sin(a*2+time*.025)*.4,.34,rgb,alpha*Math.sin(Math.PI*t));
            }
        }
        private void draw(String pathway) {
            if(!holder && !pathway.equals("justiciar")) { abilityAura(pathway); return; }
            switch(pathway) {
                case "door" -> lights(); case "fool" -> worms(); case "fortune" -> infinity(); case "chained" -> chains();
                case "error" -> clock(); case "moon" -> moon(); case "mother" -> bloom();
                case "visionary" -> thought(); case "sun" -> sun(); case "giant" -> twilight();
                case "priest" -> war(); case "abyss" -> abyss(); case "darkness" -> night();
                case "demoness" -> mirrors(); case "emperor" -> disorder(); case "hanged" -> sacrifice();
                case "hermit" -> knowledge(); case "justiciar" -> order(); case "paragon" -> assembly();
                case "tower" -> pages(); case "tyrant" -> { } default -> { }
            }
        }
        private static int mix(int a,int b,float t) {
            int r=(int)(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
            int g=(int)(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
            int blue=(int)((a&255)*(1-t)+(b&255)*t);
            return (r<<16)|(g<<8)|blue;
        }

        private void lights() {
            for(int light=0;light<7;light++) {
                double a=time*.006+light*TAU/7,r=8+Math.sin(time*.002+light)*1.2;
                double height=5+Math.sin(time*.005)*8;
                double x=Math.cos(a)*r+movement.sway()*.15,y=height+Math.sin(a*.7+light*.8)*7,z=Math.sin(a)*6.7;
                spark(x,y,z,.58,0xFFF9EE,.65);
                for(int i=1;i<=3;i++) {
                    double trail=a-i*.035;
                    double pastRadius=8+Math.sin((time-i*.035/.006)*.002+light)*1.2;
                    spark(Math.cos(trail)*pastRadius+movement.sway()*.15,height+Math.sin(trail*.7+light*.8)*7,
                            Math.sin(trail)*6.7,.24,0xE6E5EE,(1-i/4.0)*.2);
                }
            }
        }

        private void worms() {
            int segments=Math.max(36,(int)(60*.48/spacing));
            float intensity=(float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity);
            for(int arm=0;arm<6;arm++) {
                double side=arm<3?-1:1,azimuth=(arm%3-1)*.60+(side<0?Math.PI:0);
                double phase=time*(.018+arm*.0009)+arm*1.37,groundShift=groundY-23.8;
                double base=12.3+Math.sin(phase*.7)*.35-movement.activity()*.4;
                double cx=Math.cos(azimuth)*base,cz=Math.sin(azimuth)*base;
                double spin=phase*.95,coil=3.2+(arm%3)*.30;
                double startX=cx+Math.cos(spin)*coil,startZ=cz+Math.sin(spin)*coil;
                double winding=TAU*(1.18+(arm%2)*.16);
                double dx=-Math.sin(spin)*coil*winding-Math.cos(spin)*coil*.9;
                double dz=Math.cos(spin)*coil*winding-Math.sin(spin)*coil*.9;
                double dy=-Math.PI*(.9+arm%3);
                Vector3f root=new Vector3f((float)((arm-2.5)*.95)/16,(float)(11.8+arm%3*.4)/16,2.6f/16);
                attachment.transformPosition(root);root.mul(16);
                var points=new TraitGeometry.Point[segments+1];var radii=new float[segments+1];var colors=new TraitGeometry.Tint[segments+1];
                for(int i=0;i<=segments;i++) {
                    double u=i/(double)segments,x,y,z;
                    if(u<.34) {
                        double t=u/.34,v=1-t;
                        x=v*v*v*root.x+3*v*v*t*side*8+3*v*t*t*(startX-dx*.17)+t*t*t*startX;
                        y=v*v*v*root.y+3*v*v*t*(14+groundShift*.3)+3*v*t*t*(23+groundShift-dy*.17)+t*t*t*(23+groundShift);
                        z=v*v*v*root.z+3*v*v*t*7+3*v*t*t*(startZ-dz*.17)+t*t*t*startZ;
                    } else {
                        double t=(u-.34)/.66,a=spin+t*TAU*(1.18+(arm%2)*.16)+Math.sin(phase*.7+t*TAU)*.12;
                        double radius=coil*(1-t*.90);
                        x=cx+Math.cos(a)*radius;
                        z=cz+Math.sin(a)*radius;
                        y=23+groundShift-Math.sin(t*Math.PI)*(.9+arm%3)+Math.sin(a-spin)*1.5*Math.sin(t*Math.PI);
                    }
                    double free=Math.sin(u*Math.PI*.5);
                    double writing=Math.sin(phase*1.7+u*TAU*2.6+arm*.8);
                    x+=Math.sin(phase+u*5)*.85*free+writing*.32*free+movement.sway()*.20*free;
                    z+=Math.cos(phase*.83+u*4)*.72*free+Math.cos(phase*1.35+u*TAU*2.1)*.28*free+movement.lag()*.18*free;
                    radii[i]=(float)((1.0+arm%3*.12)*Math.pow(1-u,.65)+.045);
                    y+=Math.sin(phase*.7+u*5)*.62*free+Math.sin(phase*1.4+u*TAU*2.3)*.22*free;
                    double floor=groundY+.05-radii[i];
                    y=floor-Math.log1p(Math.exp((floor-y)*4))/4;
                    points[i]=G.pointPixels((float)x,(float)y,(float)z);
                    float band=(float)(.93+.07*Math.cos(u*Math.PI*22));
                    colors[i]=new TraitGeometry.Tint(.52f*band,.55f*band,.53f*band,(.74f-(float)u*.16f)*intensity);
                }
                G.drawTube(pose,consumer,points,radii,7,colors,TraitRenderSupport.FULL_BRIGHT);
                // Pale raised nodules follow the surface, giving the coils organic volume and texture.
                for(int i=5;i<segments-2;i+=3) {
                    var tangent=points[i+1].subtract(points[i-1]).normalize();
                    var reference=new TraitGeometry.Vec(0,-.7f,1);
                    float dot=tangent.x()*reference.x()+tangent.y()*reference.y()+tangent.z()*reference.z();
                    var normal=reference.add(tangent.scale(-dot)).normalize();
                    var across=normal.cross(tangent).normalize();
                    var center=points[i].add(normal.scale(radii[i]/16*.97f));
                    float r=radii[i]/16*.33f;
                    var tint=new TraitGeometry.Tint(.85f,.86f,.78f,.65f*intensity);
                    for(int wedge=0;wedge<8;wedge++) {
                        double a=wedge*TAU/8,b=(wedge+1)*TAU/8;
                        var aa=center.add(tangent.scale((float)Math.cos(a)*r)).add(across.scale((float)Math.sin(a)*r*.75f));
                        var bb=center.add(tangent.scale((float)Math.cos(b)*r)).add(across.scale((float)Math.sin(b)*r*.75f));
                        G.quad(pose,consumer,center,aa,bb,center,tint,TraitRenderSupport.FULL_BRIGHT);
                    }
                }
            }
        }

        private void infinity() {
            double head=time*.035;
            for(int i=0;i<90;i++) {
                double trail=i/90.0,a=head-trail*TAU;
                double x=8*Math.cos(a)/(1+Math.sin(a)*Math.sin(a));
                double y=4+7*Math.sin(a)*Math.cos(a)/(1+Math.sin(a)*Math.sin(a));
                spark(x,y,6.3+Math.sin(a)*.8,.38,0xC3D2D3,Math.pow(1-trail,1.6));
            }
        }
        private void chains() {
            for(int link=0;link<12;link++) {
                double a=link*TAU/12+time*.004;
                double radius=11.3+Math.sin(time*.009+link*.5)*.35;
                double x=Math.cos(a)*radius,z=Math.sin(a)*8.2;
                double diagonal=envelope(cycle(.0009,0))*5;
                double y=8+Math.cos(a)*diagonal+Math.sin(a*2+time*.015)*1.1+movement.sway()*Math.sin(a)*.45+movement.lag()*Math.cos(a)*.25;
                var points=new TraitGeometry.Point[25];var radii=new float[25];
                for(int i=0;i<25;i++) {
                    double t=i*TAU/24,tilt=link%2==0?1:.25;
                    points[i]=G.pointPixels((float)(x-Math.sin(a)*Math.cos(t)*3.05),(float)(y+Math.sin(t)*tilt),
                            (float)(z+Math.cos(a)*Math.cos(t)*3.05+Math.sin(t)*(1-tilt)));
                    radii[i]=.17f;
                }
                G.drawTube(pose,consumer,points,radii,5,new TraitGeometry.Tint[]{new TraitGeometry.Tint(.39f,.40f,.48f,
                        .75f*(float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity))},TraitRenderSupport.FULL_BRIGHT);
            }
        }

        private void clock() {
            double wobble=Math.sin(time*.02)*.3;
            for(int i=0;i<12;i++) {
                double a=i*TAU/12+wobble,slip=i==2?Math.sin(time*.14)*.8:0;
                double r=6.8+slip,fade=.25+.55*envelope(cycle(.004,i*.083));
                line(Math.sin(a)*r,3-Math.cos(a)*r,6,Math.sin(a)*(r-.65),3-Math.cos(a)*(r-.65),6,i%2==0?0x92949A:0xBA965D,fade);
            }
            arc(0,3,6,6.1,6.1,-time*.012,Math.PI*.9,0x787B83,.5);
            double hand=time*.035+Math.sin(time*.11)*.9;
            line(0,3,6,Math.sin(hand)*5,3-Math.cos(hand)*5,6,0xD8BD7A,.75);
            line(0,3,6,Math.sin(-hand*.31)*3,3-Math.cos(-hand*.31)*3,6,0xB0B1B4,.6);
            for(int i=0;i<5;i++) {
                double t=cycle(.009,i*.2),a=i*1.9-time*.015;
                spark(Math.sin(a)*(7+t*2),3-Math.cos(a)*(7+t*2),6,.28,0xC1A169,envelope(t)*.65);
            }
        }
        private void moon() {
            // Vanilla order: full, waning gibbous/quarter/crescent, new, then waxing.
            double light=Math.cos(moonPhase*Math.PI/4), direction=moonPhase<4?1:-1;
            double radius=4.6, y=2+Math.sin(time*.01)*.3;
            for(double yy=-radius+.2;yy<radius;yy+=.4) {
                double edge=Math.sqrt(Math.max(0,radius*radius-yy*yy));
                double boundary=-edge*light*direction;
                if(direction>0) {
                    panel(-edge,y+yy-.2,boundary,y+yy+.2,5.3,5.3,0x463548,.13);
                    if(moonPhase!=4)panel(boundary,y+yy-.2,edge,y+yy+.2,5.3,5.3,0xD49AA4,.60);
                } else {
                    panel(-edge,y+yy-.2,boundary,y+yy+.2,5.3,5.3,0xD49AA4,.60);
                    panel(boundary,y+yy-.2,edge,y+yy+.2,5.3,5.3,0x463548,.13);
                }
            }
            for(int i=0;i<=64;i++) {
                double a=i*TAU/64,xx=Math.cos(a)*radius,yy=Math.sin(a)*radius;
                if(moonPhase!=4 && xx*direction>=-Math.abs(Math.cos(a))*radius*light-.01)
                    spark(xx,y+yy,5.6,.3,0xD77486,.45);
            }
            if(moonPhase!=0 && moonPhase!=4)for(int i=0;i<=32;i++) {
                double yy=-radius+i*radius*2/32,edge=Math.sqrt(Math.max(0,radius*radius-yy*yy));
                spark(-edge*light*direction,y+yy,5.6,.27,0xBC5C76,.35);
            }
            // Stable surface markings follow the illuminated portion of each phase.
            for(int crater=0;crater<7;crater++) {
                double cx=Math.sin(crater*2.7)*radius*.62,cy=Math.cos(crater*1.9)*radius*.64;
                double r=.35+(crater%3)*.18;
                for(int row=-3;row<=3;row++) {
                    double yy=cy+row*r/4,edge=Math.sqrt(Math.max(0,radius*radius-yy*yy));
                    double half=r*Math.sqrt(Math.max(0,1-row*row/16.0));
                    double left=Math.max(-edge,cx-half),right=Math.min(edge,cx+half),boundary=-edge*light*direction;
                    if(direction>0)left=Math.max(left,boundary);else right=Math.min(right,boundary);
                    if(moonPhase!=4)panel(left,y+yy-.1,right,y+yy+.1,5.4,5.4,0x805268,.30);
                }
            }
            if(moonPhase==0) eye(0,y,5.7,3.1,0xF4CCD1,cycle(.0015,0));
        }

        private void bloom() {
            double bloom=movement.bloom(),drift=Math.sin(time*.004)*.06;
            float opacity=.7f*(float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity);
            for(int vine=0;vine<2;vine++) {
                double start=vine==0?Math.PI/4:Math.PI*3/4;
                var points=new TraitGeometry.Point[37];
                var radii=new float[37];
                for(int i=0;i<points.length;i++) {
                    double u=i/(double)(points.length-1),a=start+u*TAU*.8+drift;
                    double radius=4.8+u*.55,x=Math.cos(a)*radius+movement.sway()*(.12+u*.18);
                    double y=9+u*2.8,z=Math.sin(a)*3.1+movement.lag()*(.08+u*.17);
                    points[i]=G.pointPixels((float)x,(float)y,(float)z);radii[i]=.12f;
                    if(i%9==0)line(x,y,z,x+Math.cos(a)*1.1,y-.7,z+Math.sin(a)*.7,0x80975B,.45);
                }
                G.drawTube(pose,consumer,points,radii,4,
                        new TraitGeometry.Tint[]{new TraitGeometry.Tint(.27f,.40f,.23f,opacity)},TraitRenderSupport.FULL_BRIGHT);
                double a=start+drift;
                double x=Math.cos(a)*4.8+movement.sway()*.12;
                double y=9+Math.sin(time*.02+vine)*.12,z=Math.sin(a)*4.2+movement.lag()*.08;
                int petal=mix(0xDED5AE,0x98815A,movement.wilt());
                var tint=new TraitGeometry.Tint(((petal>>16)&255)/255f,((petal>>8)&255)/255f,(petal&255)/255f,opacity);
                for(int leaf=0;leaf<6;leaf++) {
                    double b=leaf*TAU/6+movement.sway()*.04,r=1.7*bloom,droop=(1-bloom)*1.1;
                    G.quad(pose,consumer,G.pointPixels((float)x,(float)y,(float)(z+.3)),
                            G.pointPixels((float)(x+Math.cos(b-.35)*r*.65),(float)(y+Math.sin(b-.35)*r*.65+droop*.5),(float)(z+1)),
                            G.pointPixels((float)(x+Math.cos(b)*r),(float)(y+Math.sin(b)*r+droop),(float)(z+.6+Math.sin(time*.012+leaf)*.16)),
                            G.pointPixels((float)(x+Math.cos(b+.35)*r*.65),(float)(y+Math.sin(b+.35)*r*.65+droop*.5),(float)(z+1)),
                            tint,TraitRenderSupport.FULL_BRIGHT);
                }
                spark(x,y,z+.65,.4,0xCDA657,.65);
                if(movement.wilt()>.05) {
                    double t=cycle(.005,vine*.5);
                    spark(x+Math.sin(t*5)*.6,y+t*4,z+.5,.24,0xB09A69,envelope(t)*movement.wilt()*.4);
                }
            }
        }

        private void legVine(boolean left) {
            var points=new TraitGeometry.Point[30];var radii=new float[30];
            for(int i=0;i<30;i++) {
                double u=i/29.0,a=u*TAU*1.1+(left?0:Math.PI)+Math.sin(time*.004)*.10;
                double r=3.0+Math.sin(u*5+time*.01)*.08,x=Math.cos(a)*r,y=.8+u*10,z=Math.sin(a)*r;
                points[i]=G.pointPixels((float)x,(float)y,(float)z);radii[i]=.11f;
                if(i%8==0)line(x,y,z,x+Math.cos(a)*.8,y-.6,z+Math.sin(a)*.8,0x80995D,.4);
            }
            float alpha=.62f*(float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity);
            G.drawTube(pose,consumer,points,radii,4,new TraitGeometry.Tint[]{new TraitGeometry.Tint(.29f,.42f,.22f,alpha)},TraitRenderSupport.FULL_BRIGHT);
        }

        private void armChain(boolean left) {
            double phase=cycle(.0009,left?0:.5),blend=envelope(Math.clamp((phase-.55)/.45,0,1));
            if(blend<.02)return;
            float alpha=(float)(blend*.7*Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity));
            for(int link=0;link<10;link++) {
                double u=link/9.0,a=u*TAU*1.5+time*.003,x=(left?1:-1)+Math.cos(a)*3.25,z=Math.sin(a)*3.25,y=.8+u*8;
                var points=new TraitGeometry.Point[17];var radii=new float[17];
                for(int i=0;i<17;i++) {
                    double b=i*TAU/16,w=link%2==0?.65:.25;
                    points[i]=G.pointPixels((float)(x-Math.sin(a)*Math.cos(b)*.85),(float)(y+Math.sin(b)*w),
                            (float)(z+Math.cos(a)*Math.cos(b)*.85+Math.sin(b)*(1-w)));
                    radii[i]=.14f;
                }
                G.drawTube(pose,consumer,points,radii,4,new TraitGeometry.Tint[]{new TraitGeometry.Tint(.39f,.40f,.48f,alpha)},TraitRenderSupport.FULL_BRIGHT);
            }
        }

        private void thought() {
            eye(0,6,7,4.8,0xC8AE72,cycle(.0014,0));
        }

        private void eye(double x,double y,double z,double radius,int rgb,double life) {
            if(life>.5)return;
            double fade=Math.clamp(Math.min(life/.06,(.5-life)/.07),0,1);
            double blink=1-.995*Math.exp(-Math.pow((life-.2)/.010,2))
                    -.995*Math.exp(-Math.pow((life-.38)/.010,2));
            double opening=Math.max(0,fade*blink);
            for(int i=0;i<=36;i++) {
                double u=i/36.0,xx=(u-.5)*radius*2,yy=Math.sin(u*Math.PI)*radius*.36*opening;
                spark(x+xx,y-yy,z,.26,rgb,fade*.5);
                spark(x+xx,y+yy,z,.26,rgb,fade*.5);
            }
            if(opening<.12)return;
            double look=Math.sin(time*.012)*radius*.3;
            double aperture=Math.sin((look/radius+1)*Math.PI/2)*radius*.36*opening;
            double up=Math.sin(time*.008)*aperture*.16,half=aperture*.58;
            float alpha=(float)(fade*.55*Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity));
            for(int i=0;i<24;i++) {
                double a=i*TAU/24,b=(i+1)*TAU/24,wide=Math.min(radius*.17,half*.6);
                G.addTriangle(pose,consumer,(float)(x+look)/16,(float)(y+up)/16,(float)(z+.25)/16,
                        (float)(x+look+Math.cos(a)*wide)/16,(float)(y+up+Math.sin(a)*half)/16,(float)(z+.25)/16,
                        (float)(x+look+Math.cos(b)*wide)/16,(float)(y+up+Math.sin(b)*half)/16,(float)(z+.25)/16,
                        ((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,alpha,TraitRenderSupport.FULL_BRIGHT);
            }
            double slit=half*.2;
            var pupil=new TraitGeometry.Tint(.06f,.035f,.07f,alpha*1.5f);
            G.quad(pose,consumer,G.pointPixels((float)(x+look),(float)(y+up-half),(float)(z+.45)),
                    G.pointPixels((float)(x+look+slit),(float)(y+up),(float)(z+.45)),
                    G.pointPixels((float)(x+look),(float)(y+up+half),(float)(z+.45)),
                    G.pointPixels((float)(x+look-slit),(float)(y+up),(float)(z+.45)),pupil,TraitRenderSupport.FULL_BRIGHT);
            spark(x+look+half*.27,y+up-half*.35,z+.47,.12,0xF4E8D5,fade*.3);
        }

        private void sun() {
            double breath=1+Math.sin(time*.018)*.04,radius=(7.1+movement.activity()*.45)*breath;
            double turn=time*.003+movement.spin()*.15;
            arc(0,4,7,radius,radius,turn,TAU,0xE9CE80,.42);
            arc(0,4,7.3,radius-.45,radius-.45,-turn,TAU,0xB99553,.35);
            for(int ray=0;ray<12;ray++) {
                double a=ray*TAU/12+turn;
                double length=1.1+movement.activity()*.9;
                line(Math.cos(a)*(radius+.3),4+Math.sin(a)*(radius+.3),7,
                        Math.cos(a)*(radius+length),4+Math.sin(a)*(radius+length),7,0xE7C779,.4);
            }
            for(int arm=0;arm<3;arm++) {
                var points=new TraitGeometry.Point[33];var radii=new float[33];
                for(int i=0;i<33;i++) {
                    double u=i/32.0,a=arm*TAU/3+turn+u*2.8,r=radius+.3+u*(1.5+movement.activity());
                    points[i]=G.pointPixels((float)(Math.cos(a)*r),(float)(4+Math.sin(a)*r),7.2f);
                    radii[i]=(float)(.15*(1-u*.85));
                }
                G.drawTube(pose,consumer,points,radii,4,new TraitGeometry.Tint[]{new TraitGeometry.Tint(.88f,.73f,.39f,.6f*
                        (float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity))},TraitRenderSupport.FULL_BRIGHT);
            }
        }

        private void twilight() {
            double x=movement.sway()*.12,z=5.8+movement.lag()*.08;
            // Silver shield, with the sword's grip and guard projecting above its rim.
            box(x+1,-4,4.8,x+1.65,0,5.5,0x665348,.9);
            for(int wrap=0;wrap<5;wrap++)line(x+1,-3.7+wrap*.65,5.6,x+1.65,-3.45+wrap*.65,5.6,0xC1B19B,.5);
            box(x-.3,-.7,4.6,x+3,-.25,5.7,0xB8BDC1,.8);
            spark(x+1.3,-4.2,5.2,.48,0xD1BD8E,.7);
            for(double y=0;y<11;y+=.4) {
                double w=y<7?3.7:3.7*(11-y)/4;
                panel(x-w,y,x+w,y+.4,z,z,0xBAC3CD,.83);
            }
            line(x-3.7,0,z+.1,x+3.7,0,z+.1,0xD2D9D8,.65);
            line(x-3.7,0,z+.1,x-3.7,7,z+.1,0xD2D9D8,.65);
            line(x+3.7,0,z+.1,x+3.7,7,z+.1,0xD2D9D8,.65);
            line(x-3.7,7,z+.1,x,11,z+.1,0xBEA477,.65);
            line(x+3.7,7,z+.1,x,11,z+.1,0xBEA477,.65);
            line(x,1,z+.2,x,9,z+.2,0xD8C491,.35);
            var rim=new TraitGeometry.Point[]{G.pointPixels((float)(x-3.7),0,(float)z),G.pointPixels((float)(x+3.7),0,(float)z),
                    G.pointPixels((float)(x+3.7),7,(float)z),G.pointPixels((float)x,11,(float)z),G.pointPixels((float)(x-3.7),7,(float)z),G.pointPixels((float)(x-3.7),0,(float)z)};
            G.drawTube(pose,consumer,rim,new float[]{.22f,.22f,.22f,.22f,.22f,.22f},4,
                    new TraitGeometry.Tint[]{new TraitGeometry.Tint(.72f,.77f,.82f,.8f*(float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity))},TraitRenderSupport.FULL_BRIGHT);
            double glint=cycle(.003,0)*10;
            line(x-2,glint,z+.3,x+2,glint-.7,z+.3,0xE1C88E,.18);

        }

        private void war() {
            for(int side=-1;side<=1;side+=2) {
                double x=side*2.7,z=9;
                line(x,-2,z,x,12,z,0x9B7860,.45);
                for(int i=0;i<14;i++) {
                    double u=i/14.0,v=(i+1)/14.0,x0=x+side*(.2+u*3.8),x1=x+side*(.2+v*3.8);
                    double z0=z+Math.sin(time*.018-u*3+side)*(.12+movement.activity()*.35)*u+movement.lag()*u*.4;
                    double z1=z+Math.sin(time*.018-v*3+side)*(.12+movement.activity()*.35)*v+movement.lag()*v*.4;
                    panel(Math.min(x0,x1),-1+(u+v)*.45,Math.max(x0,x1),7.8+(u+v)*.6,
                            side<0?z1:z0,side<0?z0:z1,0x872332,.76);
                }
                line(x,-.5,z+.1,x+side*2.4,2,z+.2,0xCB9D5B,.55);
                line(x+side*1.2,2,z+.35,x+side*2.6,5,z+.35,0xCF9B63,.45);
            }
        }

        private void abyss() {
            if(movement.groundGap()<.14) {
                float y=groundY+movement.groundGap()*16;
                float alpha=.32f*(float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity);
                for(int i=0;i<28;i++) {
                    double a=i*TAU/28,b=(i+1)*TAU/28;
                    G.addTriangle(pose,consumer,0,y/16,0,(float)Math.cos(a)*5.5f/16,y/16,(float)Math.sin(a)*3.8f/16,
                            (float)Math.cos(b)*5.5f/16,y/16,(float)Math.sin(b)*3.8f/16,.035f,.025f,.05f,alpha,TraitRenderSupport.FULL_BRIGHT);
                }
            }
            for(int limb=0;limb<4;limb++) {
                double life=cycle(.0018,limb*.25),grow=envelope(life)*(.55+movement.proximity()*.45);
                if(grow<.03)continue;
                var points=new TraitGeometry.Point[26];var radii=new float[26];
                for(int i=0;i<26;i++) {
                    double u=i/25.0,a=limb*TAU/4+Math.sin(time*.005+u*3)*.35;
                    double r=3+u*(2+movement.proximity()*4)*grow;
                    double y=groundY-.6-u*(7+movement.proximity()*7)*grow;
                    points[i]=G.pointPixels((float)(Math.cos(a)*r+movement.sway()*u*.12),(float)y,(float)(Math.sin(a)*r));
                    radii[i]=(float)(.5*grow*(1-u*.9));
                }
                G.drawTube(pose,consumer,points,radii,5,
                        new TraitGeometry.Tint[]{new TraitGeometry.Tint(.065f,.045f,.085f,(float)(.65*grow))},TraitRenderSupport.FULL_BRIGHT);
            }
        }

        private void night() {
            for(int veil=0;veil<3;veil++)for(int i=0;i<27;i++) {
                double u=i/26.0,a=time*.003+veil*TAU/3;
                // Keep the ribbons outside the full arm/torso envelope.
                double x=Math.cos(a)*(11.3+Math.sin(u*4+time*.01)*.3);
                double z=Math.sin(a)*9.5;
                spark(x+movement.sway()*u*.12,-2+u*17,z,.23,0xA4B1C7,Math.sin(u*Math.PI)*.22);
            }
            for(int star=0;star<6;star++) {
                double a=star*TAU/6+time*.002,t=cycle(.0018,star/6.0);
                double x=Math.cos(a)*11.3,y=-2+Math.sin(a*2)*7,z=Math.sin(a)*9.5,fade=.15+envelope(t)*.35;
                spark(x,y,z,.36,0xD4D5E0,fade);
                line(x-.7,y,z,x+.7,y,z,0xA4B2D1,fade);
                line(x,y-.9,z,x,y+.9,z,0xA4B2D1,fade);
            }
        }

        private void box(double x,double y,double z,double xx,double yy,double zz,int rgb,double alpha) {
            float intensity=dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity;
            G.drawBox(pose,consumer,(float)x/16,(float)y/16,(float)z/16,(float)xx/16,(float)yy/16,(float)zz/16,
                    ((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f,(float)(alpha*Math.sqrt(intensity)),TraitRenderSupport.FULL_BRIGHT);
        }

        private void abilityAura(String pathway) {
            switch(pathway) {
                case "sun" -> {
                    for(int i=0;i<3;i++) {
                        double t=cycle(.004,i/3.0);
                        arc(0,18-t*14,5.5,5+t*2,1.4,time*.006,TAU,0xE6C476,envelope(t)*.35);
                    }
                }
                case "giant", "hanged" -> {
                    for(int side=-1;side<=1;side+=2) {
                        double x=side*5.2,fade=.3+.15*Math.sin(time*.02);
                        line(x,0,3,x,12,3,pathway.equals("hanged")?0x71464B:0xBAC8CE,fade);
                        line(x,0,3,side*2,1.5,3,0xD5BC85,fade);
                        line(x,12,3,side*2,10.5,3,0xD5BC85,fade);
                    }
                }
                case "priest", "darkness", "abyss", "chained" -> {
                    int rgb=switch(pathway){case "priest"->0xC66139;case "abyss"->0x563347;case "chained"->0x8B829C;default->0x827F9C;};
                    for(int i=0;i<4;i++)for(int j=0;j<18;j++) {
                        double u=j/17.0,life=cycle(.009,i*.25),side=i%2==0?1:-1;
                        spark(side*(5+Math.sin(u*5+time*.015+i)),22-u*20,3+u,.3,rgb,
                                envelope(life)*Math.sin(u*Math.PI)*.4);
                    }
                }
                case "emperor", "demoness", "mother" -> {
                    for(int i=0;i<4;i++) {
                        double a=i*TAU/4+time*.004,x=Math.cos(a)*5,z=Math.sin(a)*3.5,skew=Math.sin(time*.012+i);
                        line(x-1,22,z,x+1+skew,22,z,pathway.equals("mother")?0x7A9952:pathway.equals("demoness")?0xAA909A:0xBFA16A,.4);
                        line(x+1+skew,22,z,x+1,23,z,0x826F88,.4);
                    }
                }
                case "fool", "moon", "fortune" -> {
                    int rgb=pathway.equals("fool")?0xD4D1BD:pathway.equals("moon")?0xBA526E:0xBCD2D0;
                    for(int i=0;i<3;i++) {
                        double t=cycle(.003,i/3.0),y=18-t*10;
                        arc(Math.sin(i*2)*2,y,5.5,3+t*2,1.1,time*.006+i,Math.PI*1.4,rgb,envelope(t)*.35);
                    }
                }
                case "door" -> {
                    for(int i=0;i<4;i++) {
                        double a=i*TAU/4+time*.004,x=Math.cos(a)*6,z=Math.sin(a)*4,fade=envelope(cycle(.002,i*.25));
                        line(x,5,z,x,15,z,0xDDDDE5,fade*.4);line(x,5,z,x+1,6,z,0xEEE9D8,fade*.4);
                    }
                }
                case "visionary" -> {
                    for(int i=0;i<3;i++){double t=cycle(.003,i/3.0);arc(0,0,4.5,3+t*4,1+t,time*.003,TAU,0xBBA86A,envelope(t)*.32);}
                }
                case "hermit" -> {
                    for(int i=0;i<6;i++) {
                        double t=cycle(.004,i/6.0),a=i*TAU/6+time*.006,x=Math.cos(a)*5;
                        line(x,18-t*16,Math.sin(a)*4,x+.6,17-t*16,Math.sin(a)*4,0x89C9D5,envelope(t)*.4);
                    }
                }
                case "paragon" -> {
                    for(int i=0;i<2;i++)arc(-5.5,8,3+i*.4,1.4+i*.6,1.4+i*.6,time*(i==0?.012:-.01),Math.PI*1.7,0xC5A671,.4);
                }
                default -> { }
            }
        }

        private void mirrors() {
            // A fractured mirror separates into suspended glass shards rather than paired rectangular frames.
            double separation=.2+movement.activity()*.3;
            float alpha=.35f*(float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity);
            for(int shard=0;shard<8;shard++) {
                double a=shard*TAU/8+.04,b=(shard+1)*TAU/8-.04,mid=(a+b)/2;
                double drift=separation+Math.sin(time*.009+shard)*.06,x=Math.cos(mid)*drift,y=5+Math.sin(mid)*drift;
                double z=7+Math.sin(time*.012+shard)*.2,radius=4.5+Math.sin(shard*2)*.25;
                double ax=x+Math.cos(a)*radius,ay=y+Math.sin(a)*radius,bx=x+Math.cos(b)*radius,by=y+Math.sin(b)*radius;
                G.addTriangle(pose,consumer,(float)x/16,(float)y/16,(float)(z+.2)/16,
                        (float)ax/16,(float)ay/16,(float)z/16,(float)bx/16,(float)by/16,(float)z/16,
                        .68f,.66f+shard%2*.06f,.76f,alpha,TraitRenderSupport.FULL_BRIGHT);
                line(ax,ay,z,bx,by,z,0xC5ACBD,.5);
                line(x,y,z+.2,ax,ay,z+.1,0xB77993,.3);
                double t=cycle(.002,shard/8.0);
                spark(x+Math.cos(mid)*(radius+t),y+Math.sin(mid)*(radius+t),z,.24,0xD99CB3,envelope(t)*.2);
            }
        }

        private void disorder() {
            for(int i=0;i<32;i++) {
                double a=i*TAU/32,b=(i+1)*TAU/32,x=Math.cos(a)*5,z=Math.sin(a)*5;
                box(x-.43,-10,z-.43,x+.43,-8.6,z+.43,0x17151A,.92);
                line(x,-8.6,z,Math.cos(b)*5,-8.6,Math.sin(b)*5,0xCEAC55,.85);
                line(x,-10,z,Math.cos(b)*5,-10,Math.sin(b)*5,0xCEAC55,.85);
                if(i%4==0) {
                    double height=i%8==0?3:2;
                    for(int step=0;step<12;step++) {
                        double u=step/11.0,angle=a+u*TAU/8,y=-10-Math.sin(u*Math.PI)*height;
                        spark(Math.cos(angle)*5,y,Math.sin(angle)*5,.35,0xD7B865,.65);
                    }
                    spark(x*1.03,-9.3,z*1.03,.46,0xA53742,.85);
                    if(i%8==0)spark(Math.cos(a+TAU/16)*5,-13,Math.sin(a+TAU/16)*5,.48,0x952939,.85);
                }
            }
        }

        private void sacrifice() {
            // Inverted cross, thorn knots and a crimson grazing eye.
            box(-.55,-5,5.3,.55,18,6,0x33272A,.9);
            box(-6,9.3,5.3,6,10.1,6,0x33272A,.8);
            line(-.5,-5,6.1,-.5,18,6.1,0x9D7B54,.5);
            line(.5,-5,6.1,.5,18,6.1,0x9D7B54,.5);
            line(-6,9.1,6.1,6,9.1,6.1,0x9D7B54,.5);
            for(int knot=0;knot<4;knot++) {
                double y=-2+knot*5;
                line(-1,y-.5,6.2,1,y+.5,6.2,0x9E5960,.5);
                line(-1,y+.5,6.2,1,y-.5,6.2,0x9E5960,.5);
                line(-.5,y,6.1,-1.7,y-1,6.5,0x80604B,.55);
                line(.5,y+.5,6.1,1.7,y-.3,6.5,0x80604B,.55);
            }
            eye(0,9.7,6.6,1.4,0xA44850,.25);
            for(int thread=0;thread<3;thread++)for(int i=0;i<30;i++) {
                double u=i/29.0,a=u*TAU*1.8-time*.012+thread*TAU/3;
                spark(Math.cos(a)*(1.2+thread*.3),-4+u*22,6.2+Math.sin(a)*.8,.23,
                        0xA93146,.35*(1-u*.35));
            }
            for(int i=0;i<3;i++) {
                double t=cycle(.006,i/3.0);
                spark(Math.sin(i*2)*.5,17+t*5,6.3,.3*(1-t*.5),0xBD4356,envelope(t)*.5);
            }
        }

        private void knowledge() {
            eye(0,6,7,4.1,0xB49BC9,cycle(.0014,0));
            for(int rune=0;rune<5;rune++) {
                double a=time*.003+rune*TAU/5,x=Math.cos(a)*7,y=6+Math.sin(a)*5;
                double fade=.2+.25*envelope(cycle(.002,rune*.2));
                line(x,y-.6,7,x,y+.7,7,0xAE93BA,fade);
                line(x,y,7,x+.8,y-.3,7,0xC1AB77,fade);
            }
        }

        private void order() {
            if(!holder && authority.isEmpty())return;
            double ruleTilt=switch(authority) {
                case "consequence" -> Math.sin(time*.025)*.2;
                case "suppression" -> -.23;
                case "confinement" -> -.10;
                case "balance" -> Math.sin(time*.012)*.04;
                default -> 0;
            };
            double tilt=ruleTilt+movement.sway()*.06+Math.sin(time*.012)*.015;
            double spread=authority.equals("isolation")?8:6,z=7;
            if(holder)arc(0,4,z+.1,10.5,10.5,time*.001,TAU,0xC8AD68,.25);
            line(0,-5,z,0,15,z,0xD2B778,.55);
            line(-Math.cos(tilt)*spread,2-Math.sin(tilt)*spread,z,
                    Math.cos(tilt)*spread,2+Math.sin(tilt)*spread,z,0xD2B778,.6);
            for(int side=-1;side<=1;side+=2) {
                double x=side*Math.cos(tilt)*spread,y=2+side*Math.sin(tilt)*spread;
                double swing=movement.sway()*.085+Math.sin(time*.02+side)*.025;
                double px=x+Math.sin(swing)*4,py=y+Math.cos(swing)*4;
                line(x,y,z,px-1.8,py,z,0xC4A365,.5);
                line(x,y,z,px+1.8,py,z,0xC4A365,.5);
                arc(px,py,z,1.8,.8,0,Math.PI,0xE0C47B,.65);
                for(int row=0;row<4;row++) {
                    double depth=row*.18,width=1.8*Math.sqrt(Math.max(0,1-depth*depth/.64));
                    panel(px-width,py+depth,px+width,py+depth+.18,z+.1,z+.1,0xB89A54,.5);
                }
                if(authority.equals("confinement")||authority.equals("no_teleportation")) {
                    line(px-1.8,py-.8,z,px+1.8,py-.8,z,0xE0C47B,.65);
                    for(int bar=-1;bar<=1;bar++)line(px+bar,py-.8,z,px+bar,py+.5,z,0xD4AB61,.45);
                }
                if(authority.equals("consequence"))spark(px,py+.25,z+.2,.5,0xBC6856,.55);
                if(authority.equals("suppression"))line(px-1.5,py+1,z,px+1.5,py+1,z,0xD4AE61,.5);
                if(authority.equals("no_teleportation"))arc(px,py+.2,z+.2,2.2,1.1,time*.005,TAU,0xD8BC80,.45);
                if(authority.equals("quell_disorder")) {
                    double settle=1-envelope(cycle(.002,side*.2+.2));
                    line(px-1.5,py+settle,z,px+1.5,py-settle,z,0xD3CCB0,.45);
                }
            }
        }

        private void assembly() {
            double assembled=1-movement.activity(),distance=.25+movement.activity()*5;
            for(int gear=0;gear<3;gear++) {
                double a=gear*TAU/3,r=gear==0?2.3+assembled*2:1.65;
                if(gear>0 && assembled>.97)continue;
                double x=(gear==0?0:Math.cos(a)*distance)+movement.sway()*.3;
                double y=5+(gear==0?0:Math.sin(a)*distance)+movement.lag()*.25,z=6.5;
                double turn=movement.spin()*(gear%2==0?1:-1)+movement.sway()*.025;
                for(int tooth=0;tooth<32;tooth++) {
                    double a0=turn+tooth*TAU/32,a1=turn+(tooth+1)*TAU/32;
                    double outer=r+(tooth%4==1||tooth%4==2?.5:0);
                    var tint=new TraitGeometry.Tint(.64f+gear*.04f,.48f+gear*.025f,.27f,.72f*(gear==0?1f:(float)(1-assembled))*
                            (float)Math.sqrt(dev.ua.ikeepcalm.coi.client.config.AppearanceConfig.get().uniquenessParticleIntensity));
                    G.quad(pose,consumer,G.pointPixels((float)(x+Math.cos(a0)*r*.63),(float)(y+Math.sin(a0)*r*.63),(float)z),
                            G.pointPixels((float)(x+Math.cos(a0)*outer),(float)(y+Math.sin(a0)*outer),(float)z),
                            G.pointPixels((float)(x+Math.cos(a1)*outer),(float)(y+Math.sin(a1)*outer),(float)z),
                            G.pointPixels((float)(x+Math.cos(a1)*r*.63),(float)(y+Math.sin(a1)*r*.63),(float)z),tint,TraitRenderSupport.FULL_BRIGHT);
                }
                for(int tooth=0;tooth<8;tooth++) {
                    double b=turn+tooth*TAU/8;
                    line(x+Math.cos(b)*r,y+Math.sin(b)*r,z,
                            x+Math.cos(b)*(r+.55),y+Math.sin(b)*(r+.55),z,0xD5B16B,.5*(gear==0?1:1-assembled));
                }
                spark(x,y,z,.36,0x91B0AD,.4*(gear==0?1:1-assembled));
            }
            for(int corner=0;corner<4;corner++) {
                double a=corner*Math.PI/2+Math.PI/4,r=8.5-assembled*2;
                double x=Math.cos(a)*r,y=5+Math.sin(a)*r,z=6.5;
                double sx=Math.signum(x),sy=Math.signum(y-5);
                line(x,y,z,x-sx*2,y,z,0xD6AF69,.45*assembled);
                line(x,y,z,x,y-sy*2,z,0xD6AF69,.45*assembled);
            }
        }

        private void pages() {
            double open=Math.clamp((idle-12)/30.0,0,1),width=2+2.4*open;
            double z=5+movement.lag()*.1;
            for(int side=-1;side<=1;side+=2) {
                double edge=z+1.5-open*.5;
                if(side<0)panel(-width,1,0,9,edge,z,0xD5C9AB,.82);
                else panel(0,1,width,9,z,edge,0xE0D4B9,.82);
                line(side*width,1,edge,side*width,9,edge,0x8E6C49,.6);
                for(int row=0;row<3;row++)
                    line(side*.5,2+row*2,z+.1,side*(width-.5),2+row*2,edge+.1,0x77685B,.25);
            }
            box(-.28,.5,z-.3,.28,9.5,z+.15,0x6B4B36,.9);
            line(0,.7,z+.2,0,9.3,z+.2,0xBCA16A,.7);
            for(int clasp=0;clasp<2;clasp++)line(-.55,2+clasp*5,z+.3,.55,2+clasp*5,z+.3,0xD2B97B,.55);
            double turn=Math.sin(time*.014);
            // Every turning leaf remains behind the independent eye plane.
            double leafZ=z+1.5+Math.cos(time*.014)*.35;
            if(turn<0)panel(turn*width,1,0,9,leafZ,z,0xF0E5C9,.65*open);
            else panel(0,1,turn*width,9,z,leafZ,0xF0E5C9,.65*open);
            if(open>.85)eye(0,5,8.2,2.3,0xC9B775,.10+Math.floorMod(idle-12,200)/200.0*.32);
        }

    }
}
