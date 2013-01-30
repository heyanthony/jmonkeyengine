/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.blender.filetypes;

import com.jme3.gde.blender.BlenderTool;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.assets.SpatialAssetDataObject;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public abstract class AbstractBlenderAssetDataObject extends SpatialAssetDataObject {

    protected String SUFFIX;

    public AbstractBlenderAssetDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
    }

    @Override
    public Spatial loadAsset() {
        if (SUFFIX == null) {
            throw new IllegalStateException("Suffix for blender filetype is null! Set SUFFIX = \"sfx\" in constructor!");
        }
        ProjectAssetManager mgr = getLookup().lookup(ProjectAssetManager.class);
        if (mgr == null) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("File is not part of a project!\nCannot load without ProjectAssetManager."));
            return null;
        }
        FileObject mainFile = getPrimaryFile();
        BlenderTool.runConversionScript(SUFFIX, mainFile);
        FileObject outFile = FileUtil.findBrother(mainFile, BlenderTool.TEMP_SUFFIX);
        if (outFile == null) {
            logger.log(Level.SEVERE, "Failed to create model, blend file cannot be found");
            return null;
        }
        String assetKey = mgr.getRelativeAssetPath(outFile.getPath());
        FileLock lock = null;
        try {
            lock = getPrimaryFile().lock();
            listListener.start();
            Spatial spatial = mgr.loadModel(assetKey);
            replaceFiles();
            listListener.stop();
            savable = spatial;
            return spatial;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
            try {
                outFile.delete();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }

    protected void replaceFiles() {
        for (int i = 0; i < assetList.size(); i++) {
            FileObject fileObject = assetList.get(i);
            if (fileObject.hasExt(BlenderTool.TEMP_SUFFIX)) {
                assetList.remove(i);
                assetKeyList.remove(i);
                assetList.add(i, getPrimaryFile());
                assetKeyList.add(getAssetKey());
                return;
            }
        }
    }
}
