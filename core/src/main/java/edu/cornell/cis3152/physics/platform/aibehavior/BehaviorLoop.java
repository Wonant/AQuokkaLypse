package edu.cornell.cis3152.physics.platform.aibehavior;

import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.btree.BehaviorTree;

public class BehaviorLoop<E> {
    private BehaviorTree<E> tree;

    /** the id of the enemy this behavior loop corresponds to*/
    private int id;

    private int step;

    private float delay;
    private float runtime;

    public BehaviorLoop(BehaviorTree<E> tree) {
        this.tree = tree;
        this.step = step;

        //defaults
        delay = 0.3f;
        runtime = 0;
    }

    public BehaviorLoop(BehaviorTree<E> tree, float delay, float runtime) {
        this.tree = tree;
        this.step = step;

        this.delay = delay;
        this.runtime = runtime;
    }

    public void update(float delta) {
        GdxAI.getTimepiece().update(delta);
        runtime += delta;
        if (runtime > delay) {
            tree.step();
            step++;
            runtime = 0;
        }
    }

    /** getters and setters */

    public BehaviorTree<E> getBehaviorTree() {
        return tree;
    }

    public int getId() {
        return id;
    }

    public float getDelay() {
        return delay;
    }

    public float getRuntime() {
        return runtime;
    }

    // if we want to dynamically change how fast a character may update
    public void setDelay(float d) {
        delay = d;
    }

    public void setRuntime(float rt) {
        runtime = rt;
    }
}
