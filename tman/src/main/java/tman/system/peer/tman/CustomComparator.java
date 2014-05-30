
package tman.system.peer.tman;

import java.util.Comparator;

/**
 * A comparator interface to group underneath, all the other comparators.
 * 
 * @author vangelis
 * @param <E> The peer descriptor being compared
 */
public abstract class CustomComparator<E> implements Comparator<E> {

    @Override
    public abstract int compare(E p1, E p2);
}
