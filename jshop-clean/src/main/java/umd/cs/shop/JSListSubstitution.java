package umd.cs.shop;

import java.util.*;

import java.io.*;


public class JSListSubstitution {

    /*==== instance variables ====*/

    private String label;
    Vector<JSSubstitution> substitutionVector;


    JSListSubstitution() {
        substitutionVector = new Vector<>();

    }


    public boolean fail() {
        return (substitutionVector.size() == 0);
    }

    public void print() {

        JSUtil.print("(");

        JSSubstitution s1;
        Enumeration<JSSubstitution> s = substitutionVector.elements();
        while (s.hasMoreElements()) {
            s1 = s.nextElement();
            if (s1 != null)
                s1.print();
        }
        JSUtil.println(")");
    }


}

