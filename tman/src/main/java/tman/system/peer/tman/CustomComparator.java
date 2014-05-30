/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tman.system.peer.tman;

import java.util.Comparator;

/**
 *  A comparator interface to group underneath, all the other comparators.
 * 
 * @author vangelis
 * @param <E> The peer descriptor being compared
 */
public abstract class CustomComparator<E> implements Comparator<E> {

    @Override
    public abstract int compare(E p1, E p2);
}
