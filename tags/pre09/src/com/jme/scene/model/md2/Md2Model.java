/*
 * Copyright (c) 2003, jMonkeyEngine - Mojo Monkey Coding All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the Mojo Monkey Coding, jME, jMonkey Engine, nor the
 * names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.jme.scene.model.md2;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import com.jme.animation.VertexKeyframeController;
import com.jme.bounding.BoundingSphere;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.TriMesh;
import com.jme.scene.model.Face;
import com.jme.scene.model.Model;
import com.jme.system.JmeException;
import com.jme.util.BinaryFileReader;
import com.jme.util.LoggingSystem;

/**
 * <code>Md2Model</code> defines a model using the MD2 model format made
 * common by Quake 2. This loader builds the mesh of each frame of animation
 * then builds the animation controller that allows the shown mesh to be
 * displayed at any given time. The memory footprint may be quite large
 * depending on how many key frames exist, and how many vertices within the
 * mesh.
 *
 * @author Mark Powell
 * @version $Id: Md2Model.java,v 1.16 2004-04-06 22:13:44 renanse Exp $
 */
public class Md2Model extends Model {
    private BinaryFileReader bis = null;

    private Header header;

    private Vector2f[] texCoords;
    private Md2Face[] triangles;
    private Md2Frame[] frames;

    //holds each keyframe.
    private TriMesh[] triMesh;
    //controller responsible for handling keyframe morphing.
    private VertexKeyframeController controller;

    /**
     * Constructor creates a new <code>Md2Model</code> object. A
     * later call to <code>load</code> is required to initialize
     * the model with MD2 data.
     * @param name the name of the scene element. This is required for identification and
     * 		comparision purposes.
     */
    public Md2Model(String name) {
        super(name);
    }

    /**
     * Constructor creates a new <code>Md2Mode</code> object. The
     * filename corresponding to the MD2 model is provided, loading
     * the data of the model.
     * @param name the name of the scene element. This is required for identification and
     * 		comparision purposes.
     * @param filename the filename of the model to load.
     */
    public Md2Model(String name, String filename) {
        super(name);
        load(filename);
    }

    /**
     * Loads an MD2 model. The corresponding
     * <code>TriMesh</code> objects are created and attached to the
     * model. Each keyframe is then loaded and assigned to a
     * <code>VertexKeyframeController</code>. MD2 does not keep track
     * of it's own texture or material settings, so the user is
     * responsible for setting these.
     * @param filename the file to load.
     */
    public void load(String filename) {
        try {
            URL file = new URL("file:"+filename);
            load(file);
        } catch (MalformedURLException e) {
            LoggingSystem.getLogger().log(Level.WARNING, "Could not load: " +
                    filename);
        }
    }

    public void load(String filename, String textureDirectory) {
        load(filename);
    }

    public void load(URL filename, String textureDirectory) {
        load(filename);
    }

    /**
     * Loads an MD2 model. The corresponding
     * <code>TriMesh</code> objects are created and attached to the
     * model. Each keyframe is then loaded and assigned to a
     * <code>VertexKeyframeController</code>. MD2 does not keep track
     * of it's own texture or material settings, so the user is
     * responsible for setting these.
     * @param filename the url of the file to load.
     */
    public void load(URL filename) {
        if(null == filename) {
            throw new JmeException("Null data. Cannot load.");
        }
        InputStream is = null;
        bis = new BinaryFileReader(filename);

        header = new Header();

        if (header.version != 8) {
            throw new JmeException(
                "Invalid file format (Version not 8): " + filename + "!");
        }

        parseMesh();
        convertDataStructures();

        triangles = null;
        texCoords = null;
        frames = null;
    }

    /**
     * <code>getAnimationController</code> returns the animation controller
     * used for MD2 animation (VertexKeyframeController).
     *
     * @return @see com.jme.scene.model.Model#getAnimationController()
     */
    public Controller getAnimationController() {
        return controller;
    }

    /**
     *
     * <code>parseMesh</code> reads the MD2 file and builds the
     * necessary data structures. These structures are specific to
     * MD2 and therefore require later conversion to jME data structures.
     *
     */
    private void parseMesh() {
        String[] skins = new String[header.numSkins];
        texCoords = new Vector2f[header.numTexCoords];
        triangles = new Md2Face[header.numTriangles];
        frames = new Md2Frame[header.numFrames];

        //start with skins. Move the file pointer to the correct position.
        bis.setOffset(header.offsetSkins);

        // Read in each skin for this model
        for (int j = 0; j < header.numSkins; j++) {
            skins[j] = bis.readString(64);
        }

        //Now read in texture coordinates.
        bis.setOffset(header.offsetTexCoords);
        for (int j = 0; j < header.numTexCoords; j++) {
            texCoords[j] = new Vector2f();
            texCoords[j].x = bis.readShort();
            texCoords[j].y = bis.readShort();
        }

        //read the vertex data.
        bis.setOffset(header.offsetTriangles);
        for (int j = 0; j < header.numTriangles; j++) {
            triangles[j] = new Md2Face();
        }
        bis.setOffset(header.offsetFrames);

        //Each keyframe has the same type of data, so read each
        //keyframe one at a time.
        for (int i = 0; i < header.numFrames; i++) {
            VectorKeyframe frame = new VectorKeyframe();
            frames[i] = new Md2Frame();

            frames[i].vertices = new Triangle[header.numVertices];
            Vector3f[] aliasVertices = new Vector3f[header.numVertices];
            int[] aliasLightNormals = new int[header.numVertices];

            // Read in the first frame of animation
            for (int j = 0; j < header.numVertices; j++) {
                aliasVertices[j] =
                    new Vector3f(
                        bis.readByte(),
                        bis.readByte(),
                        bis.readByte());
                aliasLightNormals[j] = bis.readByte();
            }

            // Copy the name of the animation to our frames array
            frames[i].name = frame.name;
            Triangle[] verices = frames[i].vertices;

            for (int j = 0; j < header.numVertices; j++) {
                verices[j] = new Triangle();
                verices[j].vertex.x =
                    aliasVertices[j].x * frame.scale.x + frame.translate.x;
                verices[j].vertex.z =
                    -1
                        * (aliasVertices[j].y * frame.scale.y
                            + frame.translate.y);
                verices[j].vertex.y =
                    aliasVertices[j].z * frame.scale.z + frame.translate.z;
            }
        }
    }

    /**
     *
     * <code>convertDataStructures</code> takes the loaded MD2 data and
     * converts it into jME data.
     *
     */
    private void convertDataStructures() {
        triMesh = new TriMesh[header.numFrames];
        Vector2f[] texCoords2 = new Vector2f[header.numVertices];

        for (int i = 0; i < header.numFrames; i++) {
            int numOfVerts = header.numVertices;
            int numTexVertex = header.numTexCoords;
            int numOfFaces = header.numTriangles;
            triMesh[i] = new TriMesh(frames[i].name);
            Vector3f[] verts = new Vector3f[numOfVerts];
            Vector2f[] texVerts = new Vector2f[numTexVertex];

            Face[] faces = new Face[numOfFaces];

            //assign a vector array for the trimesh.
            for (int j = 0; j < numOfVerts; j++) {
                verts[j] = new Vector3f();
                verts[j].x = frames[i].vertices[j].vertex.x;
                verts[j].y = frames[i].vertices[j].vertex.y;
                verts[j].z = frames[i].vertices[j].vertex.z;
            }

            //set up the initial indices array.
            for (int j = 0; j < numOfFaces; j++) {
                faces[j] = new Face();
                faces[j].vertIndex[0] = triangles[j].vertexIndices[0];
                faces[j].vertIndex[1] = triangles[j].vertexIndices[1];
                faces[j].vertIndex[2] = triangles[j].vertexIndices[2];

                faces[j].coordIndex[0] = triangles[j].textureIndices[0];
                faces[j].coordIndex[1] = triangles[j].textureIndices[1];
                faces[j].coordIndex[2] = triangles[j].textureIndices[2];
            }

            if (i == 0) {
                //texture coordinates.
                for (int j = 0; j < numTexVertex; j++) {
                    texVerts[j] = new Vector2f();
                    texVerts[j].x = texCoords[j].x / (float) (header.skinWidth);
                    texVerts[j].y =
                        1 - texCoords[j].y / (float) (header.skinHeight);
                }

                //reorginize coordinates to match the vertex index.
                if (numTexVertex != 0) {
                  for (int j = 0; j < numOfFaces; j++) {
                    int index = faces[j].vertIndex[0];
                    texCoords2[index] = new Vector2f();
                    texCoords2[index] = texVerts[faces[j].coordIndex[0]];

                    index = faces[j].vertIndex[1];
                    texCoords2[index] = new Vector2f();
                    texCoords2[index] = texVerts[faces[j].coordIndex[1]];

                    index = faces[j].vertIndex[2];
                    texCoords2[index] = new Vector2f();
                    texCoords2[index] = texVerts[faces[j].coordIndex[2]];
                  }
                }

                int[] indices = new int[numOfFaces * 3];
                int count = 0;
                for (int j = 0; j < numOfFaces; j++) {
                    indices[count] = faces[j].vertIndex[0];
                    count++;
                    indices[count] = faces[j].vertIndex[1];
                    count++;
                    indices[count] = faces[j].vertIndex[2];
                    count++;
                }
                triMesh[i].setIndices(indices);
            }

            triMesh[i].setVertices(verts);
            triMesh[i].setNormals(computeNormals(faces, verts));

            if (i == 0) {
                triMesh[i].setTextures(texCoords2);
                triMesh[i].setModelBound(new BoundingSphere());
                triMesh[i].updateModelBound();
            }
        }


        //build controller. Attach everything.
        controller = new VertexKeyframeController();
        controller.setKeyframes(triMesh);
        controller.setDisplayedMesh(triMesh[0]);
        this.attachChild(triMesh[0]);
        this.addController(controller);

    }

    /**
     *
     * <code>computeNormals</code> calculates the normals of
     * the model.
     * @param faces the faces of the model.
     * @param verts the vertices of the model.
     * @return the array of normals.
     */
    private Vector3f[] computeNormals(Face[] faces, Vector3f[] verts) {
        Vector3f[] returnNormals = new Vector3f[verts.length];

        Vector3f[] normals = new Vector3f[faces.length];
        Vector3f[] tempNormals = new Vector3f[faces.length];

        for (int i = 0; i < faces.length; i++) {
            tempNormals[i] =
                verts[faces[i].vertIndex[0]].subtract(
                    verts[faces[i].vertIndex[2]]).cross(
                    verts[faces[i].vertIndex[2]].subtract(
                        verts[faces[i].vertIndex[1]]));
            normals[i] = tempNormals[i].normalize();
        }

        Vector3f sum = new Vector3f();
        int shared = 0;

        for (int i = 0; i < verts.length; i++) {
            for (int j = 0; j < faces.length; j++) {
                if (faces[j].vertIndex[0] == i
                    || faces[j].vertIndex[1] == i
                    || faces[j].vertIndex[2] == i) {
                    sum.addLocal(tempNormals[j]);
                    shared++;
                }
            }

            returnNormals[i] = sum.divide(-shared);
            returnNormals[i] = returnNormals[i].normalizeLocal().negateLocal();

            sum.zero();
            shared = 0;
        }

        return returnNormals;
    }

    // This holds the header information that is read in at the beginning of
    // the file
    private class Header {
        int magic; // This is used to identify the file
        int version; // The version number of the file (Must be 8)
        int skinWidth; // The skin width in pixels
        int skinHeight; // The skin height in pixels
        int frameSize; // The size in bytes the frames are
        int numSkins; // The number of skins associated with the model
        int numVertices; // The number of vertices (constant for each frame)
        int numTexCoords; // The number of texture coordinates
        int numTriangles; // The number of faces (polygons)
        int numGlCommands; // The number of gl commands
        int numFrames; // The number of animation frames
        int offsetSkins; // The offset in the file for the skin data
        int offsetTexCoords; // The offset in the file for the texture data
        int offsetTriangles; // The offset in the file for the face data
        int offsetFrames; // The offset in the file for the frames data
        int offsetGlCommands;
        // The offset in the file for the gl commands data
        int offsetEnd; // The end of the file offset

        Header() {
            magic = bis.readInt();
            version = bis.readInt();
            skinWidth = bis.readInt();
            skinHeight = bis.readInt();
            frameSize = bis.readInt();
            numSkins = bis.readInt();
            numVertices = bis.readInt();
            numTexCoords = bis.readInt();
            numTriangles = bis.readInt();
            numGlCommands = bis.readInt();
            numFrames = bis.readInt();
            offsetSkins = bis.readInt();
            offsetTexCoords = bis.readInt();
            offsetTriangles = bis.readInt();
            offsetFrames = bis.readInt();
            offsetGlCommands = bis.readInt();
            offsetEnd = bis.readInt();
        }

    };

    // This stores the normals and vertices for the frames
    private class Triangle {
        Vector3f vertex = new Vector3f();
        Vector3f normal = new Vector3f();
    };

    // This stores the indices into the vertex and texture coordinate arrays
    private class Md2Face {
        int[] vertexIndices = new int[3]; // short
        int[] textureIndices = new int[3]; // short

        Md2Face() {
            vertexIndices =
                new int[] { bis.readShort(), bis.readShort(), bis.readShort()};
            textureIndices =
                new int[] { bis.readShort(), bis.readShort(), bis.readShort()};
        }
    };

    // This stores the animation scale, translation and name information for a
    // frame, plus verts
    private class VectorKeyframe {
        private Vector3f scale = new Vector3f();
        private Vector3f translate = new Vector3f();
        private String name;

        VectorKeyframe() {
            scale.x = bis.readFloat();
            scale.y = bis.readFloat();
            scale.z = bis.readFloat();

            translate.x = bis.readFloat();
            translate.y = bis.readFloat();
            translate.z = bis.readFloat();
            name = bis.readString(16);
        }
    };

    // This stores the frames vertices after they have been transformed
    private class Md2Frame {
        String name; // char [16]
        Triangle[] vertices;

        Md2Frame() {
        }
    };
}