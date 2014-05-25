/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package main;

import java.util.LinkedList;

/**
 *
 * @author Evangelos Savvidis
 */
public class Test {

    public static void main(String args[]){
        
        
        LinkedList<Integer> list = new LinkedList<Integer>();
        
        list.add(1);
        list.add(2);
        list.add(3);

        Integer a = list.remove();
        
        System.out.println("REMOVED ELEMENT " + a);
        //list.remove();
        
        System.out.println("List size is " + list.size() + " the elements are " + list);
    }
}
