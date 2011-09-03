package com.jme3.scene.plugins.blender.constraints;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.BoneAnimation;
import com.jme3.animation.Skeleton;
import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Follow path' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintFollowPath extends Constraint {
	private static final Logger LOGGER = Logger.getLogger(ConstraintFollowPath.class.getName());
	
	/**
	 * This constructor creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
	 * @param boneOMA
	 *            the old memory address of the constraint owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param dataRepository
	 *            the data repository
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ConstraintFollowPath(Structure constraintStructure, Long boneOMA,
			Ipo influenceIpo, DataRepository dataRepository) throws BlenderFileException {
		super(constraintStructure, boneOMA, influenceIpo, dataRepository);
	}

	@Override
	public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation) {
		//TODO: implement when curves are implemented
		LOGGER.log(Level.INFO, "'Follow path' not implemented! Curves not yet implemented!");
	}
	
	@Override
	public ConstraintType getType() {
		return ConstraintType.CONSTRAINT_TYPE_FOLLOWPATH;
	}
}