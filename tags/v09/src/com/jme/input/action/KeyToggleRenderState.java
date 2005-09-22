/*
 * Copyright (c) 2003-2005 jMonkeyEngine
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

package com.jme.input.action;

import com.jme.scene.Node;
import com.jme.scene.state.RenderState;

/**
 * Toggles a renderstate enabled/disabled.
 * 
 * Created on Jul 21, 2004
 *
 * @author Joel Schuster
 *  
 */
public class KeyToggleRenderState extends KeyInputAction {

    //the state to manipulate
    private RenderState state = null;

    //the node that owns this state.
    private Node ownerNode = null;

    /**
     * instantiates a new KeyToggleRenderState object. The state to switch and
     * the owner of the state are supplied during creation.
     * 
     * @param state
     *            the state to switch.
     * @param owner
     *            the owner of the state.
     */
    public KeyToggleRenderState(RenderState state, Node owner) {
        this.state = state;
        this.ownerNode = owner;
        this.setAllowsRepeats(false);
    }

    /**
     * switch the state from on to off or off to on.
     * 
     * @param evt
     *            the event that executed the action.
     */
    public void performAction(InputActionEvent evt) {
        state.setEnabled(!state.isEnabled());
        ownerNode.updateRenderState();
    }
}