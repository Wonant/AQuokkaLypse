package edu.cornell.cis3152.physics.platform.aibehavior.tasks;

import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import edu.cornell.cis3152.physics.platform.CuriosityCritter;
import edu.cornell.cis3152.physics.platform.Enemy;

public class StunnedActionTask extends LeafTask<Enemy> {
    @TaskAttribute(required = true)
    public float stunDuration;

    private float elapsed;

    @Override
    public void start() {
        elapsed = 0f;

        if (getObject() instanceof CuriosityCritter) {
            CuriosityCritter c = (CuriosityCritter) getObject();

            if (!c.isStunned()) {
                c.setStunned(true);
                c.setMovement(0);
                c.applyForce();
            }
        }
    }

    @Override
    public Status execute() {
        if (!(getObject() instanceof CuriosityCritter)) {
            return Status.SUCCEEDED;
        }


        CuriosityCritter c = (CuriosityCritter) getObject();

        if (!c.isStunned()) {
            return Status.SUCCEEDED;

            // Remain stunned: no movement
            c.setMovement(0);
            c.applyForce(); // So velocity gets updated

            // Track elapsed time
            float dt = GdxAI.getTimepiece().getDeltaTime();
            elapsed += dt;

            // If we've served our full stun sentence, clear it and succeed
            if (elapsed >= stunDuration) {
                c.setActiveTexture(c.getScene().getAiManager().directory);
                c.setStunned(false);
                System.out.println("stun ended");
                return Status.SUCCEEDED;
            } else if (getObject() instanceof DreamDweller) {
                DreamDweller d = (DreamDweller) getObject();
                if (!d.isStunned()) {
                    return Status.SUCCEEDED;
                }

                // Remain stunned: no movement
                d.setMovement(0);

                // Track elapsed time
                dt = GdxAI.getTimepiece().getDeltaTime();
                elapsed += dt;

                // If we've served our full stun sentence, clear it and succeed
                if (elapsed >= stunDuration) {
                    c.setActiveTexture(c.getScene().getAiManager().directory);
                    c.setStunned(false);
                    System.out.println("stun ended");
                    return Status.SUCCEEDED;
                }

                // Otherwise keep running
            }

        }

        c.setMovement(0);
        c.applyForce();

        float dt = GdxAI.getTimepiece().getDeltaTime();
        elapsed += dt;

        if (elapsed >= stunDuration) {
            c.setStunned(false);
            c.setActiveTexture(c.getScene().getAiManager().directory); 
            System.out.println("stun ended");
            return Status.SUCCEEDED;
        }

        return Status.RUNNING;
    }

    @Override
    public void end() {

    }

    @Override
    protected Task<Enemy> copyTo(Task<Enemy> task) {
        StunnedActionTask other = (StunnedActionTask) task;
        other.stunDuration = this.stunDuration;
        return task;
    }
}
