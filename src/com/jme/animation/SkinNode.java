/*
 * Copyright (c) 2003-2006 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jme.animation;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import com.jme.math.Matrix4f;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.scene.ConnectionPoint;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.batch.GeomBatch;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;
import com.jme.util.export.Savable;
import com.jme.util.geom.VertMap;

/**
 * SkinNode defines a scene node that contains skinned mesh data. A skinned mesh
 * is defined by a Geometry object representing the "skin" that is attached to a
 * skeleton (or a tree of Bones). The orientation, translation of these bones
 * define the position of the skin vertices. These bones can then be driven by
 * an animation system to provide the animation of the skin. SkinNode defines
 * for each vertex of the skin the bone that affects it and the weight
 * (BoneInfluence) of that affect. This allows multiple bones to share a single
 * vertex (although the total weight must add up to 1).
 * 
 * @author Joshua Slack
 * @author Mark Powell
 */
public class SkinNode extends Node implements Savable, BoneChangeListener {

    private static final long serialVersionUID = 1L;

    public static final float THROTTLE = 1/30f;
    public static final float OFFSCREEN_THROTTLE = 1/4f;

    protected static Vector3f vertex = new Vector3f();
    protected static Vector3f normal = new Vector3f();

    protected boolean recalcBounds = true;
    protected boolean recalcNormals = true;
    protected boolean needsRefresh = true;

    protected Geometry skin = null;

    protected ArrayList<Bone> skeletons = new ArrayList<Bone>();
    protected ArrayList<BoneInfluence>[][] cache = null;
    
    protected ArrayList<ConnectionPoint> connectionPoints;
    
    protected boolean newSkeletonAssigned = false;

    protected transient Matrix4f bindMatrix = new Matrix4f();

    protected float updateTime = 0;

    /**
     * Empty Constructor to be used internally only.
     */
    public SkinNode() {
        setLastFrustumIntersection(Camera.INSIDE_FRUSTUM);
    }

    /**
     * Constructor creates a new SkinNode object with the supplied name.
     * 
     * @param name
     *            the name of this SkinNode
     */
    public SkinNode(String name) {
        super(name);
    }

    /**
     * getSkin returns the skin (Geometry) that the SkinNode is controlling.
     * 
     * @return the skin of this SkinNode
     */
    public Geometry getSkin() {
        return skin;
    }

    /**
     * setSkin sets the skin that the SkinNode will affect.
     * 
     * @param skin
     *            the skin that this SkinNode will affect.
     */
    public void setSkin(Geometry skin) {
        this.skin = skin;
        attachChild(skin);
    }
    
    public void setAnimation(BoneAnimation anim) {
        //XXX assumption that we are only going to have one skeleton per skin-node 
        //XXX for the foreseeable future. This will need to change if that is not
        //XXX the case.
        if(skeletons == null || skeletons.size() == 0) {
            return;
        }
        
        if(skeletons.get(0) == null) {
            return;
        }
        
        if (skeletons.get(0).getAnimationController() != null) {
                skeletons.get(0).getAnimationController().setActiveAnimation(anim);
        }
    }
    
    public void setAnimation(int index) {
        if(skeletons == null || skeletons.size() == 0) {
            return;
        }
        
        if(skeletons.get(0) == null) {
            return;
        }
        
        if(skeletons.get(0).getAnimationController() != null) {
            skeletons.get(0).getAnimationController().setActiveAnimation(index);
        }
    }
    
    public void setAnimation(String name) {
        if(skeletons == null || skeletons.size() == 0) {
            return;
        }
        
        if(skeletons.get(0) == null) {
            return;
        }
        
        if(skeletons.get(0).getAnimationController() != null) {
            skeletons.get(0).getAnimationController().setActiveAnimation(name);
        }
    }
    
    public String getAnimationString() {
        return skeletons.get(0).getAnimationController().getActiveAnimation().getName();
    }

    /**
     * returns true if the bounding volume is recalculated on each update, false
     * otherwise. This is true by default.
     * 
     * @return true if bounding volumes will be calculated on each update, false
     *         otherwise.
     */
    public boolean isRecalcBounds() {
        return recalcBounds;
    }

    /**
     * sets whether the bounding volumes should be recalculated on each update
     * or not. True will recalculate the bounding volumes, while false will not.
     * This is true by default.
     * 
     * @param recalcBounds
     *            true to recalculate bounding volumes, false to not.
     */
    public void setRecalcBounds(boolean recalcBounds) {
        this.recalcBounds = recalcBounds;
    }

    /**
     * returns true if the normals are recalculated on each update, false
     * otherwise. This is true by default.
     * 
     * @return true if normals will be calculated on each update, false
     *         otherwise.
     */
    public boolean isRecalcNormals() {
        return recalcNormals;
    }

    /**
     * sets whether the normals should be recalculated on each update or not.
     * True will recalculate the normals, while false will not. This is true by
     * default.
     * 
     * @param recalcNormals
     *            true to recalculate normals, false to not.
     */
    public void setRecalcNormals(boolean recalcNormals) {
        this.recalcNormals = recalcNormals;
    }

    /**
     * addBoneInfluence defines how a vertex will be affected by a bone. This is
     * given with four values, the batch the vertex is found, the index to the
     * vertex in the batch, the index of the bone that has been or will be set
     * via setBones or addBone and the weight that this indexed bone affects the
     * vertex.
     * 
     * @param batch
     *            the batch that contains the vertex to be affected.
     * @param vert
     *            the index to the vertex.
     * @param bone
     *            the bone that affects the vertex.
     * @param weight
     *            the weight that the bone will affect the vertex.
     */
    public void addBoneInfluence(int batch, int vert, Bone bone,
            float weight) {
    	if (weight == 0) return;
        if (cache == null)
            recreateCache();

        ArrayList<BoneInfluence> infs = cache[batch][vert];
        if (infs == null) {
            infs = new ArrayList<BoneInfluence>(1);
            cache[batch][vert] = infs;
        }
        BoneInfluence i = new BoneInfluence(bone, weight);
        i.boneId = bone.getName();
        if (!infs.contains(i))
        	infs.add(i);
//        else System.err.println("ALREADY THERE(a)! "+i.boneId+" "+batch+","+vert);
    }
    
    public void addBoneInfluence(int batch, int vert, String boneId,
            float weight) {
    	if (weight == 0) return;
        if (cache == null) {
            recreateCache();
        }
        
        ArrayList<BoneInfluence> infs = cache[batch][vert];
        if (infs == null) {
            infs = new ArrayList<BoneInfluence>(1);
            cache[batch][vert] = infs;
        }
        BoneInfluence i = new BoneInfluence(null, weight);
        i.boneId = boneId;
        if (!infs.contains(i))
        	infs.add(i);
//        else System.err.println("ALREADY THERE(b)! "+i.boneId+" "+batch+","+vert+" w: "+weight);

    }
    
    public ConnectionPoint addConnectionPoint(String name, Bone b) {
        ConnectionPoint cp = new ConnectionPoint(name, b);
        if(connectionPoints == null) {
            connectionPoints = new ArrayList<ConnectionPoint>();
        }
        connectionPoints.add(cp);
        this.attachChild(cp);
        return cp;
    }
    
    public ArrayList<ConnectionPoint> getConnectionPoints() {
        return connectionPoints;
    }

    /**
     * recreateCache initializes the cache of BoneInfluences for use by the skin
     * node.
     */
    @SuppressWarnings("unchecked")
    public void recreateCache() {
        cache = new ArrayList[skin.getBatchCount()][];
        for (int x = 0; x < cache.length; x++) {
        	cache[x] = new ArrayList[skin.getBatch(x).getVertexCount()];
        }
    }

    /**
     * updateGeometricState overrides Spatials updateGeometric state to update
     * the skin mesh based on any changes the bones may have undergone. The
     * update is defined by the updateTime, only when that much time has passed
     * will the updateSkin method be called.
     * 
     * @param time
     *            the time that has passed between calls.
     * @param initiator
     *            true if this is the top level being called.
     */
    public void updateGeometricState(float time, boolean initiator) {
        if (newSkeletonAssigned) {
            assignSkeletonBoneInfluences();
        }
        updateTime += time;
        if (skin != null && needsRefresh && updateTime >= THROTTLE) {
            updateSkin();
            if (recalcBounds) {
                skin.updateModelBound();
            }
            needsRefresh = false;

            super.updateGeometricState(updateTime, initiator);
            updateTime = 0;
        }
        updateWorldVectors();
        if(skin != null) {
            skin.updateWorldVectors();
        }
    }

    /**
     * normalizeWeights insures that all vertex BoneInfluences equal 1. The total
     * BoneInfluence on a single vertex should be 1 otherwise the position of the
     * vertex will be multiplied.
     */
    public void normalizeWeights() {
        if (cache == null)
            return;
        for (int batch = cache.length; --batch >= 0;) {
            normalizeWeights(batch);
        }
    }

    public void normalizeWeights(int batch) {
        if (cache == null)
            return;
        for (int vert = cache[batch].length; --vert >= 0;) {
            ArrayList<BoneInfluence> infs = cache[batch][vert];
            if (infs == null)
                continue;
            float total = 0;
            for (int x = infs.size(); --x >= 0;) {
                BoneInfluence BoneInfluence = infs.get(x);
                total += BoneInfluence.weight;
            }
            for (int x = infs.size(); --x >= 0;) {
                BoneInfluence BoneInfluence = infs.get(x);
                BoneInfluence.weight /= total;
            }
        }
    }
    
    public void addSkeleton(Bone b) {
        if(skeletons == null) {
            skeletons = new ArrayList<Bone>();
        }
        skeletons.add(b);
        newSkeletonAssigned = true;
        
        b.addBoneListener(this);
    }
    
    public void setSkeleton(Bone b) {
        if(skeletons == null) {
            skeletons = new ArrayList<Bone>();
        }
        skeletons.clear();
        addSkeleton(b);
        
    }

    public void assignSkeletonBoneInfluences() {
        for(int i = 0; i < cache.length; i++) {
            
            for(int j = 0; j < cache[i].length; j++) {
            	if(cache[i][j] != null) {
	                for(int k = 0; k < cache[i][j].size(); k++) {
	                    cache[i][j].get(k).assignBone(skeletons.get(0));
	                }
            	}
            }
        }
        
        regenInfluenceOffsets();
        normalizeWeights();
        newSkeletonAssigned = false;
    }
    
    /**
     * regenInfluenceOffsets calculate the offset of a particular vertex from a
     * bone. This allows the bone's rotation to position the vertex in world
     * space. This should only be called a single time during initialization.
     */
    public void regenInfluenceOffsets() {
        if (cache == null)
            return;

        Vector3f vertex = new Vector3f();
        Vector3f normal = new Vector3f();

        FloatBuffer verts, norms;
        for (int batch = cache.length; --batch >= 0;) {
            GeomBatch tb = skin.getBatch(batch);
            verts = tb.getVertexBuffer();
            norms = tb.getNormalBuffer();
            verts.clear();
            norms.clear();
            for (int vert = 0, max = cache[batch].length; vert < max; vert++) {
                ArrayList<BoneInfluence> infs = cache[batch][vert];

                vertex.set(verts.get(), verts.get(), verts.get());
                normal.set(norms.get(), norms.get(), norms.get());

                if (infs == null)
                    continue;

                bindMatrix.mult(vertex, vertex);

                if (recalcNormals) {
                    bindMatrix.rotateVect(normal);
                }
                for (int x = infs.size(); --x >= 0;) {
                    BoneInfluence infl = infs.get(x);
                    infl.vOffset = new Vector3f(vertex);

                    if (recalcNormals) {
                        infl.nOffset = new Vector3f(normal);
                    }

                }
            }
        }
    }

    /**
     * updateSkin positions the vertices of the skin based on the bones and the
     * BoneInfluences those bones have on the vertices. Each vertex is placed into
     * world space for rendering.
     */
    public void updateSkin() {
        if (cache == null || skin == null)
            return;
        
        FloatBuffer verts, norms;

        for (int batch = cache.length; --batch >= 0;) {
            GeomBatch tb = skin.getBatch(batch);
            verts = tb.getVertexBuffer();
            norms = tb.getNormalBuffer();
            verts.clear();
            if (recalcNormals)
                norms.clear();
            tb.setHasDirtyVertices(true);
            for (int vert = 0, max = cache[batch].length; vert < max; vert++) {
                ArrayList<BoneInfluence> infs = cache[batch][vert];
                if (infs == null)
                    continue;
                vertex.zero();
                if (recalcNormals)
                    normal.zero();

                for (int x = infs.size(); --x >= 0;) {
                    BoneInfluence inf = infs.get(x);
                    if (inf.bone != null) {
                        inf.bone.applyBone(inf, vertex, normal);
                    } 
                }

                vertex.multLocal(worldScale);
                
                verts.put(vertex.x).put(vertex.y).put(vertex.z);
                if (recalcNormals) {
                    norms.put(normal.x).put(normal.y).put(normal.z);
                }
            }
        }
    }

    public ArrayList<BoneInfluence>[][] getCache() {
        return cache;
    }

    public void setCache(ArrayList<BoneInfluence>[][] cache) {
        this.cache = cache;
    }

    public ArrayList<Bone> getSkeletons() {
        return skeletons;
    }

    public void setBindMatrix(Matrix4f mat) {
        bindMatrix = mat;
    }
    
    public void batchChange(Geometry geometry, int index1, int index2) {
        if(geometry == skin) {
            ArrayList<BoneInfluence>[] temp1 = cache[index1];
            ArrayList<BoneInfluence>[] temp2 = cache[index2];
            cache[index1] = temp2;
            cache[index2] = temp1;
        }
    }

    public void write(JMEExporter e) throws IOException {
        revertToBind();
        super.write(e);
        OutputCapsule cap = e.getCapsule(this);

        cap.write(recalcBounds, "recalcBounds", true);
        cap.write(recalcNormals, "recalcNormals", true);
        cap.write(skin, "skin", null);
        cap.writeSavableArrayList(skeletons, "skeletons", null);
        cap.writeSavableArrayListArray2D(cache, "cache", null);
        cap.writeSavableArrayList(connectionPoints, "connectionPoints", null);
        cap.write(newSkeletonAssigned, "newSkeletonAssigned", false);
    }

    @SuppressWarnings("unchecked")
    public void read(JMEImporter e) throws IOException {
        super.read(e);
        InputCapsule cap = e.getCapsule(this);
        
        recalcBounds = cap.readBoolean("recalcBounds", true);
        recalcNormals = cap.readBoolean("recalcNormals", true);
        skin = (Geometry)cap.readSavable("skin", null);
        skeletons = cap.readSavableArrayList("skeletons", null);
        cache = cap.readSavableArrayListArray2D("cache", null);
        connectionPoints = cap.readSavableArrayList("connectionPoints", null);
        newSkeletonAssigned = cap.readBoolean("newSkeletonAssigned", false);
        regenInfluenceOffsets();
        skin.updateModelBound();
        updateWorldBound();
        for (Bone b : skeletons) {
            b.addBoneListener(this);
        }
    }

    public void revertToBind() {
        for (Bone b : skeletons) {
            b.getRootSkeleton().revertToBind();
        }
        updateSkin();
        bindMatrix.loadIdentity();
    }
    
    public void boneChanged(BoneChangeEvent e) {
        if (getLastFrustumIntersection() != Camera.OUTSIDE_FRUSTUM || updateTime >= OFFSCREEN_THROTTLE) {
            needsRefresh = true;
            setLastFrustumIntersection(Camera.OUTSIDE_FRUSTUM);
        }
    }
    
    public void remapInfluences(VertMap[] mappings) {
        for (int x = 0; x < mappings.length; x++) {
            remapInfluences(mappings[x], x);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void remapInfluences(VertMap mappings, int batchIndex) {
    	ArrayList<BoneInfluence>[] infls = cache[batchIndex];
        ArrayList<BoneInfluence>[] newInfls = new ArrayList[skin.getBatch(batchIndex).getVertexCount()];
        cache[batchIndex] = newInfls;
        for (int x = 0; x < infls.length; x++) {
        	for (int y = 0; y < infls[x].size(); y++) {
            	
                BoneInfluence bi = infls[x].get(y);
                if (bi.bone != null)
                    addBoneInfluence(batchIndex, mappings.getNewIndex(x), bi.bone, bi.weight);
                else
                    addBoneInfluence(batchIndex, mappings.getNewIndex(x), bi.boneId, bi.weight);
            }
        }
        normalizeWeights(batchIndex);
    }
    
    @SuppressWarnings("unchecked")
    public void removeBatch(int batchIndex) {
        ArrayList<BoneInfluence>[][] newCache = new ArrayList[skin.getBatchCount()][];
        for (int x = 0; x < cache.length-1; x++) {
            if (x < batchIndex)
                newCache[x] = cache[x];
            else
                newCache[x] = cache[x+1];
        }
        cache = newCache;
    }

    public void refreshSkeletons() {
        for (Bone b : skeletons) {
            b.updateGeometricState(0, true);
        }
    }
}
