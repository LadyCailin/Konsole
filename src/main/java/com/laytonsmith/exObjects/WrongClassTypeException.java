/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.laytonsmith.exObjects;

/**
 * This exception is thrown when an inappropriate classification is assumed
 * and one of the wrong get or set functions are called. It extends RuntimeException,
 * so none of the thrown exceptions need to be checked, because most often the values
 * will be hard coded, but if the code relies of fairly dynamic values, it may be
 * wise to check for the exception anyways.
 */
public class WrongClassTypeException extends RuntimeException {

    public WrongClassTypeException(String s) {
        super(s);
    }
}
