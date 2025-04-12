package edu.cornell.cis3152.physics.platform.aibehavior;

import com.badlogic.gdx.ai.btree.BehaviorTree;
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser;
import com.badlogic.gdx.files.FileHandle;

import java.io.IOException;
import java.io.Reader;

public class BehaviorFactory<E> {
    private String source;

    /**
     * Create a behavior tree factory from the source code
     *
     * @param code  The behavior tree source code
     */
    public BehaviorFactory(String code) {
        source = code;
    }

    public BehaviorFactory(FileHandle file) {
        try {
            Reader reader = file.reader();
            StringBuilder builder = new StringBuilder();
            int c = reader.read();
            while (c != -1) {
                builder.append((char) c);
                c = reader.read();
            }
            source = builder.toString();
        } catch (IOException e) {

        }
    }

    /**
     * Returns a gameplay loop for the given blackboard object
     *
     * @param object    The blackboard object
     */
    public BehaviorLoop<E> createActor(E object) {
        BehaviorTreeParser<E> parser = new BehaviorTreeParser<E>(BehaviorTreeParser.DEBUG_HIGH);
        BehaviorTree<E> tree = parser.parse(source, object);
        BehaviorLoop<E> result = new BehaviorLoop<E>(tree);
        return result;
    }

    /**
     * Returns a gameplay loop for the given blackboard object
     *
     * This method will also attach the given external listener to the behavior
     * tree.
     *
     * @param object    The blackboard object
     * @param listener  The tree listener
     */
    public BehaviorLoop<E> createActor(E object, BehaviorTree.Listener<E> listener) {
        BehaviorTreeParser<E> parser = new BehaviorTreeParser<E>(BehaviorTreeParser.DEBUG_HIGH);
        BehaviorTree<E> tree = parser.parse(source, object);
        tree.addListener(listener);
        BehaviorLoop<E> result = new BehaviorLoop<E>(tree);
        return result;
    }
}
