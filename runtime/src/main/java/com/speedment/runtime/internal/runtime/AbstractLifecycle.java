/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.runtime.internal.runtime;

import com.speedment.runtime.component.Lifecyclable;
import com.speedment.fika.logger.Logger;
import com.speedment.fika.logger.LoggerManager;
import static java.util.Objects.requireNonNull;

/**
 * This class provides an abstract implementation of a {@link Lifecyclable}. It 
 * also introduces the following overridable events for each 
 * {@code Lifecyclable} transition:
 * <ul>
 *      <li>{@link #onInitialize()}</li>
 *      <li>{@link #onLoad()}</li>
 *      <li>{@link #onResolve()}</li>
 *      <li>{@link #onStart()}</li>
 *      <li>{@link #onStop()}</li>
 * </ul>
 *<p>
 * It also introduces the following Runnables that can be set dynamically during
 * run-time:
 * <ul>
 *      <li>{@link #setPreInitialize(Runnable)}</li>
 *      <li>{@link #setPreLoad(Runnable)}</li>
 *      <li>{@link #setPreResolve(Runnable)}</li>
 *      <li>{@link #setPreStart(Runnable)}</li>
 *      <li>{@link #setPreStop(Runnable)}</li>
 *      <li>{@link #setPostStop(Runnable)}</li>
 * </ul>
 * <p>
 * The Runnable will be invoked upon its corresponding life-cycle method.
 *
 * @author     Per Minborg
 * @author     Emil Forslund
 * @param <T>  the self type
 * @see        Lifecyclable
 * @since      2.0
 */
public abstract class AbstractLifecycle<T extends Lifecyclable<T>> implements Lifecyclable<T> {
    
    private static final Logger LIFECYCLABLE_LOGGER = LoggerManager.getLogger(Lifecyclable.class);
    private static final Runnable NOTHING = () -> {};

    private State state;
    private Runnable preInit, preLoad, preResolve, preStart, preStop, postStop;
    
    protected AbstractLifecycle() {
        state = State.CREATED;
        preInit = preLoad = preResolve = preStart = preStop = postStop = NOTHING;
    }

    @Override
    public final void setState(State newState) {
        this.state = requireNonNull(newState);
    }

    @Override
    public final State getState() {
        return state;
    }

    /**
     * Sets the non-null pre-initialize {@link Runnable} that is to be run
     * before the {@link #onInitialize()} method is called.
     *
     * @param preInit Runnable to set
     */
    public final void setPreInitialize(Runnable preInit) {
        this.preInit = requireNonNull(preInit);
    }
    
    /**
     * Sets the non-null pre-load {@link Runnable} that is to be run
     * before the {@link #onLoad()} method is called.
     *
     * @param preLoad Runnable to set
     */
    public final void setPreLoad(Runnable preLoad) {
        this.preLoad = requireNonNull(preLoad);
    }

    /**
     * Sets the non-null pre-resolve {@link Runnable} that is to be run before
     * the {@link #onResolve()} method is called.
     *
     * @param preResolve Runnable to set
     */
    public final void setPreResolve(Runnable preResolve) {
        this.preResolve = requireNonNull(preResolve);
    }

    /**
     * Sets the non-null pre-start {@link Runnable} that is to be run before the
     * {@link #onStart()} method is called.
     *
     * @param preStart Runnable to set
     */
    public final void setPreStart(Runnable preStart) {
        this.preStart = requireNonNull(preStart);
    }

    /**
     * Sets the non-null pre-stop {@link Runnable} that is to be run before the
     * {@link #onStop()} method is called.
     *
     * @param preStop Runnable to set
     */
    public final void setPreStop(Runnable preStop) {
        this.preStop = requireNonNull(preStop);
    }

    /**
     * Sets the non-null post-stop {@link Runnable} that is to be run after the
     * {@link #onStop()} method has been called.
     *
     * @param postStop Runnable to set
     */
    public final void setPostStop(Runnable postStop) {
        this.postStop = requireNonNull(postStop);
    }
    
    @Override
    public void preInitialize() {
        preInit.run();
    }
    
    @Override
    public void preLoad() {
        preLoad.run();
    }
    
    @Override
    public void preResolve() {
        preResolve.run();
    }
    
    @Override
    public void preStart() {
        preStart.run();
    }
    
    @Override
    public void preStop() {
        preStop.run();
    }

    @Override
    public void postStop() {
        postStop.run();
    }
    
    @Override
    public final T initialize() {
        LIFECYCLABLE_LOGGER.debug("Initializing " + getClass().getSimpleName());
        getState().checkNextState(State.INIITIALIZED);
        preInitialize();
        onInitialize();
        setState(State.INIITIALIZED);
        @SuppressWarnings("unchecked")
        final T self = (T) this;
        return self;
    }
    
    @Override
    public final T load() {
        if (getState() == State.CREATED) {
            initialize();
        }
        LIFECYCLABLE_LOGGER.debug("Loading " + getClass().getSimpleName());
        getState().checkNextState(State.LOADED);
        preLoad();
        onLoad();
        setState(State.LOADED);
        @SuppressWarnings("unchecked")
        final T self = (T) this;
        return self;
    }
    
    @Override
    public final T resolve() {
        if (getState() == State.CREATED) {
            initialize();
        }
        if (getState() == State.INIITIALIZED) {
            load();
        }
        LIFECYCLABLE_LOGGER.debug("Resolving " + getClass().getSimpleName());
        getState().checkNextState(State.RESOLVED);
        preResolve();
        onResolve();
        setState(State.RESOLVED);
        @SuppressWarnings("unchecked")
        final T self = (T) this;
        return self;
    }
    
    @Override
    public final T start() {
        if (getState() == State.CREATED) {
            initialize();
        }
        if (getState() == State.INIITIALIZED) {
            load();
        }
        if (getState() == State.LOADED) {
            resolve();
        }
        LIFECYCLABLE_LOGGER.debug("Starting " + getClass().getSimpleName());
        getState().checkNextState(State.STARTED);
        preStart();
        onStart();
        setState(State.STARTED);
        @SuppressWarnings("unchecked")
        final T self = (T) this;
        return self;
    }

    @Override
    public final T stop() {
        LIFECYCLABLE_LOGGER.debug("Stopping " + getClass().getSimpleName());
        getState().checkNextState(State.STOPPED);
        preStop();
        onStop();
        postStop();
        setState(State.STOPPED);
        @SuppressWarnings("unchecked")
        final T self = (T) this;
        return self;
    }
    
    @Override
    public String toString() {
        return getState().toString();
    }
    
    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }
}