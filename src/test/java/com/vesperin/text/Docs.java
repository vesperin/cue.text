package com.vesperin.text;

import com.vesperin.text.Selection.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Huascar Sanchez
 */
public class Docs {
  private Docs(){}

  public static List<Document> documents(){
    final List<String> names = Docs.qualifiedClassNames();

    final List<Selection.Document> documents = new ArrayList<>();

    for(int idx = 0; idx < names.size(); idx++){
      documents.add(new Selection.DocumentImpl(idx, names.get(idx)));
    }

    return documents;

  }

  public static List<String> qualifiedClassNames(){
    return Arrays.asList("org.dyn4j.dynamics.joint.MotorJoint",
      "com.jme3.light.DirectionalLight", "org.ode4j.ode.internal.gimpact.GimTriCollision",
      "com.jme3.input.event.MouseMotionEvent",
      "net.smert.jreactphysics3d.collision.broadphase.BroadPhaseAlgorithm",
      "com.jme3.shader.ShaderVariable", "com.jme3.renderer.opengl.TextureUtil",
      "com.jme3.effect.shapes.EmitterShape", "com.jme3.collision.bih.BIHTriangle",
      "org.ode4j.ode.DDoubleHingeJoint", "org.dyn4j.geometry.WoundIterator",
      "com.jme3.light.LightList", "org.dyn4j.collision.narrowphase.Raycast",
      "org.dyn4j.dynamics.DetectBroadphaseFilter",
      "net.smert.jreactphysics3d.collision.narrowphase.EPA.TriangleComparison",
      "com.jme3.input.controls.AnalogListener", "com.jme3.animation.AnimationFactory",
      "net.smert.jreactphysics3d.collision.narrowphase.SphereVsSphereAlgorithm",
      "com.jme3.scene.debug.WireSphere", "org.ode4j.ode.internal.CollideCylinderBox",
      "com.jme3.effect.influencers.DefaultParticleInfluencer", "org.ode4j.math.DMatrix3",
      "com.jme3.light.Light", "com.jme3.effect.shapes.EmitterPointShape",
      "org.ode4j.ode.internal.CollideTrimeshSphere", "org.ode4j.math.DMatrixN",
      "com.jme3.math.AbstractTriangle", "com.jme3.shadow.SpotLightShadowRenderer",
      "org.dyn4j.dynamics.BodyFixture", "com.jme3.effect.shapes.EmitterSphereShape",
      "com.jme3.effect.ParticleMesh", "net.smert.jreactphysics3d.collision.shapes.ConeShape",
      "com.jme3.light.SpotLight", "com.jme3.scene.shape.AbstractBox",
      "com.jme3.effect.influencers.RadialParticleInfluencer",
      "com.jme3.shadow.DirectionalLightShadowFilter", "com.jme3.scene.LightNode",
      "com.jme3.shader.ShaderNode", "com.jme3.light.LightProbe",
      "org.dyn4j.collision.narrowphase.FallbackNarrowphaseDetector",
      "com.jme3.material.MaterialProcessor",
      "net.smert.jreactphysics3d.collision.broadphase.NoBroadPhaseAlgorithm",
      "net.smert.jreactphysics3d.collision.shapes.CapsuleShape",
      "com.jme3.light.PointLight", "com.jme3.shader.ShaderUtils", "com.jme3.texture.TextureProcessor",
      "org.ode4j.ode.internal.CollideBoxBox", "com.jme3.collision.bih.TriangleAxisComparator",
      "org.dyn4j.collision.narrowphase.NarrowphaseDetector", "org.dyn4j.dynamics.DetectListener",
      "org.ode4j.ode.internal.CollisionLibccdCylinderStacking",
      "org.ode4j.ode.internal.joints.DxJointLimitMotor", "com.jme3.scene.debug.SkeletonInterBoneWire",
      "org.dyn4j.collision.narrowphase.CircleDetector", "org.dyn4j.geometry.decompose.SweepLine",
      "org.ode4j.ode.DBallJoint", "org.ode4j.ode.internal.Matrix",
      "com.jme3.environment.generation.IrradianceMapGenerator", "org.dyn4j.geometry.AbstractShape",
      "com.jme3.collision.Collidable", "org.ode4j.ode.internal.DxSphere", "com.jme3.effect.ParticleEmitter",
      "com.jme3.export.OutputCapsule", "com.jme3.environment.LightProbeFactory",
      "com.jme3.shader.ShaderNodeVariable", "net.smert.jreactphysics3d.collision.shapes.BoxShape",
      "org.ode4j.ode.internal.CollideTrimeshPlane", "org.dyn4j.geometry.hull.LinkedVertexHull",
      "com.jme3.effect.ParticlePointMesh", "com.jme3.light.LightFilter",
      "com.jme3.cinematic.MotionPathListener", "org.ode4j.ode.internal.joints.DxJointPlane2D",
      "org.dyn4j.geometry.Transformable", "com.jme3.light.DefaultLightFilter",
      "com.jme3.animation.Animation", "org.ode4j.ode.internal.joints.DxJointBall",
      "org.dyn4j.dynamics.RaycastListener", "com.jme3.texture.TextureArray",
      "com.jme3.scene.instancing.InstancedGeometry", "com.jme3.light.PoiLightProbeLightFilter",
      "com.jme3.material.ShaderGenerationInfo", "org.dyn4j.dynamics.joint.PrismaticJoint",
      "org.ode4j.ode.internal.joints.DxJointHinge2", "com.jme3.scene.GeometryGroupNode",
      "org.ode4j.ode.internal.DxCollisionUtil", "org.dyn4j.geometry.Rotatable",
      "com.jme3.animation.BoneTrack", "org.ode4j.ode.DDoubleBallJoint",
      "com.jme3.scene.debug.SkeletonPoints", "net.smert.jreactphysics3d.constraint.HingeJoint",
      "org.dyn4j.dynamics.CollisionAdapter", "org.ode4j.ode.internal.CollideTrimeshBoxOld",
      "org.ode4j.math.DQuaternionC", "com.jme3.texture.TextureCubeMap", "org.dyn4j.dynamics.RaycastAdapter",
      "org.dyn4j.collision.Collidable", "org.ode4j.ode.DRotation",
      "org.ode4j.ode.internal.gimpact.GimTrimeshCapsuleCollision",
      "org.dyn4j.geometry.decompose.EarClippingVertex",
      "net.smert.jreactphysics3d.collision.narrowphase.EPA.TriangleEPA",
      "org.ode4j.ode.internal.CollideTrimeshBox", "org.ode4j.ode.internal.CollideCylinderPlane",
      "net.smert.jreactphysics3d.mathematics.Matrix2x2", "com.jme3.cinematic.events.AnimationTrack",
      "com.jme3.texture.Texture3D", "com.jme3.shadow.PointLightShadowFilter",
      "com.jme3.asset.ShaderNodeDefinitionKey", "com.jme3.effect.shapes.EmitterMeshVertexShape",
      "com.jme3.effect.shapes.EmitterBoxShape", "net.smert.jreactphysics3d.mathematics.Quaternion",
      "com.jme3.animation.SpatialTrack", "com.jme3.cinematic.events.CinematicEventListener",
      "org.dyn4j.dynamics.DetectResult", "org.ode4j.ode.DAMotorJoint", "com.jme3.scene.debug.SkeletonDebugger",
      "com.jme3.shader.Glsl150ShaderGenerator", "com.jme3.effect.shapes.EmitterMeshFaceShape",
      "org.ode4j.ode.internal.gimpact.GimTrimeshRayCollision",
      "org.dyn4j.collision.broadphase.BroadphaseDetector", "com.jme3.scene.shape.Cylinder",
      "com.jme3.texture.Texture2D", "com.jme3.renderer.queue.TransparentComparator",
      "com.jme3.light.LightProbeBlendingProcessor", "org.ode4j.ode.DLMotorJoint",
      "org.ode4j.ode.internal.CollideTrimeshTrimesh", "net.smert.jreactphysics3d.engine.Impulse",
      "org.dyn4j.dynamics.BodyIterator", "com.jme3.cinematic.Cinematic",
      "org.ode4j.ode.internal.CollideTrimeshCCylinder", "org.dyn4j.geometry.Triangle",
      "com.jme3.shader.ShaderGenerator", "com.jme3.renderer.queue.GeometryComparator",
      "net.smert.jreactphysics3d.mathematics.Matrix3x3", "org.dyn4j.collision.AbstractCollidable",
      "com.jme3.light.BasicProbeBlendingStrategy", "com.jme3.light.AmbientLight", "com.jme3.math.Triangle",
      "com.jme3.effect.influencers.NewtonianParticleInfluencer", "org.ode4j.ode.DPlane2DJoint",
      "com.jme3.shadow.DirectionalLightShadowRenderer", "org.ode4j.ode.internal.joints.DxJointPiston",
      "org.ode4j.ode.internal.CollideTrimeshRay", "org.ode4j.ode.internal.joints.DxJointDHinge",
      "com.jme3.util.TangentBinormalGenerator", "org.dyn4j.dynamics.RaycastBroadphaseFilter",
      "org.ode4j.ode.internal.joints.DxJointHinge", "org.ode4j.ode.internal.CollideCylinderTrimesh",
      "com.jme3.math.Matrix3f", "org.ode4j.ode.DCapsule", "org.dyn4j.geometry.hull.HullGenerator",
      "net.smert.jreactphysics3d.collision.broadphase.SweepAndPruneAlgorithm",
      "com.jme3.effect.Particle", "com.jme3.scene.CollisionData",
      "org.ode4j.ode.internal.gimpact.GimTrimeshSphereCollision",
      "com.jme3.material.Material", "org.dyn4j.collision.narrowphase.SegmentDetector",
      "net.smert.jreactphysics3d.collision.BroadPhasePair", "com.jme3.post.SceneProcessor",
      "com.jme3.shadow.PointLightShadowRenderer", "com.jme3.scene.control.LightControl",
      "org.ode4j.ode.internal.CollisionLibccd", "org.ode4j.ode.internal.joints.DxJointTransmission",
      "org.ode4j.math.DMatrix3C", "org.ode4j.ode.DMatrix", "org.dyn4j.dynamics.CollisionListener",
      "org.dyn4j.dynamics.joint.FrictionJoint", "net.smert.jreactphysics3d.engine.CollisionWorld",
      "org.dyn4j.geometry.decompose.Triangulator", "com.jme3.effect.influencers.ParticleInfluencer",
      "com.jme3.effect.ParticleTriMesh", "com.jme3.ui.Picture", "org.dyn4j.geometry.decompose.EarClipping",
      "net.smert.jreactphysics3d.collision.CollisionDetection",
      "org.dyn4j.collision.broadphase.AbstractBroadphaseDetector", "com.jme3.collision.CollisionResults",
      "com.jme3.util.mikktspace.MikktspaceTangentGenerator", "org.ode4j.ode.DPistonJoint",
      "net.smert.jreactphysics3d.collision.shapes.CylinderShape",
      "org.ode4j.ode.internal.gimpact.GimTrimeshTrimeshCol", "org.ode4j.ode.internal.DxCapsule",
      "com.jme3.material.MaterialDef", "com.jme3.scene.shape.Sphere", "org.ode4j.ode.DCylinder",
      "com.jme3.shadow.SpotLightShadowFilter", "com.jme3.scene.Spatial", "org.dyn4j.dynamics.RaycastResult",
      "org.ode4j.ode.internal.joints.DxJointLMotor", "org.dyn4j.collision.narrowphase.DistanceDetector",
      "org.dyn4j.dynamics.DetectAdapter", "org.ode4j.ode.DHingeJoint", "org.ode4j.ode.DColliderFn",
      "com.jme3.shader.Glsl100ShaderGenerator", "com.jme3.renderer.opengl.GLTimingState", "com.jme3.math.Spline",
      "org.ode4j.ode.DTransmissionJoint", "com.jme3.math.Matrix4f", "org.ode4j.ode.internal.CollideBoxPlane",
      "com.jme3.collision.SweepSphere", "com.jme3.bounding.Intersection",
      "com.jme3.cinematic.events.AbstractCinematicEvent", "org.dyn4j.geometry.Capsule",
      "org.dyn4j.dynamics.joint.RopeJoint", "com.jme3.shader.ShaderNodeDefinition",
      "com.jme3.effect.shapes.EmitterMeshConvexHullShape",
      "net.smert.jreactphysics3d.collision.narrowphase.EPA.TrianglesStore",
      "com.jme3.scene.debug.SkeletonWire", "com.jme3.shader.Shader", "com.jme3.animation.SkeletonControl",
      "org.ode4j.ode.internal.Rotation", "com.jme3.effect.influencers.EmptyParticleInfluencer",
      "com.jme3.material.MaterialList", "com.jme3.texture.Texture", "org.dyn4j.collision.continuous.TimeOfImpactDetector",
      "org.ode4j.ode.internal.DxGimpactCollision",
      "com.jme3.cinematic.events.MotionEvent",
      "com.jme3.cinematic.events.MotionTrack", "com.jme3.cinematic.events.CinematicEvent",
      "org.ode4j.ode.internal.CollideSpaceGeom", "net.smert.jreactphysics3d.engine.Material", "net.smert.jreactphysics3d.collision.shapes.ConvexMeshShape",
      "org.dyn4j.collision.Collisions", "com.jme3.collision.CollisionResult", "org.dyn4j.geometry.decompose.SweepLineState",
      "org.dyn4j.collision.narrowphase.Penetration", "org.dyn4j.geometry.decompose.SweepLineVertex", "com.jme3.asset.TextureKey",
      "net.smert.jreactphysics3d.body.CollisionBody", "org.ode4j.ode.DHinge2Joint", "com.jme3.cinematic.events.AnimationEvent", "org.ode4j.ode.internal.joints.DxJointDBall",
      "org.ode4j.ode.internal.CollideCylinderSphere", "com.jme3.light.LightProbeBlendingStrategy", "net.smert.jreactphysics3d.collision.broadphase.BodyPair",
      "org.dyn4j.geometry.Matrix33", "org.dyn4j.geometry.decompose.SweepLineEdge", "com.jme3.renderer.opengl.GLTiming", "org.ode4j.ode.internal.joints.DxJointAMotor",
      "org.ode4j.ode.internal.DxCylinder", "org.dyn4j.collision.narrowphase.RaycastDetector", "org.dyn4j.geometry.Shape", "com.jme3.scene.SceneGraphVisitor",
      "net.smert.jreactphysics3d.constraint.HingeJointInfo",
      "com.jme3.export.InputCapsule", "com.jme3.animation.Skeleton", "org.ode4j.ode.internal.CollideConvexTrimesh",
      "net.smert.jreactphysics3d.collision.shapes.SphereShape", "net.smert.jreactphysics3d.collision.BodyIndexPair",
      "com.jme3.cinematic.MotionPath", "org.dyn4j.geometry.Matrix22", "org.dyn4j.geometry.Slice",
      "org.ode4j.ode.DSphere", "com.jme3.collision.MotionAllowedListener", "org.ode4j.math.DQuaternion", "com.jme3.asset.MaterialKey",
      "net.smert.jreactphysics3d.collision.narrowphase.NarrowPhaseAlgorithm", "com.jme3.math.Quaternion",
      "net.smert.jreactphysics3d.collision.shapes.CollisionShape", "com.jme3.animation.AnimationUtils",
      "com.jme3.scene.shape.PQTorus", "com.jme3.scene.shape.Torus", "org.dyn4j.dynamics.Torque",
      "com.jme3.scene.SceneGraphVisitorAdapter", "org.ode4j.ode.internal.AbstractStepper");
  }
}
