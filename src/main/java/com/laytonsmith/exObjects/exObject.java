/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.laytonsmith.exObjects;

import java.io.Serializable;

/**
 * The exObject class is an object that can wrap any type of java object, or primitive, including
 * null types and void types. There are several constructors each taking one of the primitives
 * or objects, and two static methods for creating object of null or void types. Each
 * constructor has a corresponding getValue and setValue functions that each take
 * or return a value of the correct type. There is also a corresponding isType function
 * to programmatically check the type of the object, or alternately, getClassification,
 * which returns an enum of the type. If a get or set function is called inappropriately,
 * a WrongClassTypeException is thrown. Note that this class has special support
 * for Strings, because they are so special. Technically they are Objects, but this
 * allows for more specifically describing the type.
 * @author Layton Smith
 */
public class exObject extends Object implements Serializable{
    //Each constructor (for the most part) sets these three values: type, classification,
    //and the corresponding Val.
    private Class type;
    private Classification classification;

    private Object objectVal;
    private String stringVal;
    private byte byteVal;
    private short shortVal;
    private int intVal;
    private long longVal;
    private float floatVal;
    private double doubleVal;
    private boolean booleanVal;
    private char charVal;


    /*/ General functions ******************************************************/
    /**
     * Private, no argument constructor for use with static functions
     */
    private exObject(){}

    /**
     * This constructor allows for a generic creation of a primitive object. It
     * allows for a primitive to be passed as a string, and the desired type
     * to be passed as a string, and the value will automatically be parsed
     * into the appropriate value. Valid values for type are any of the 8 primitive
     * types + the string type. Neither type nor value may be null, or a WrongClassTypeException is
     * thrown. Boolean types are handled slightly differently from Boolean.parseBoolean, as numeric
     * values are valid. 0 is false, and any other value is true.
     *
     * <p>In general, this function is used for dynamically parsing a primitive from a string,
     * if the type of the value is also stored as a string.</p>
     * @param type
     * @param value
     */
    public static exObject getPrimitive(String type, String value){
        if(type.equals("long")){
            return new exObject(Long.parseLong(value));
        } else if(type.equals("float")){
            return new exObject(Float.parseFloat(value));
        } else if(type.equals("double")){
            return new exObject(Double.parseDouble(value));
        } else if(type.equals("boolean")){
            //try to parse it as a number first
            try{
                int i = Integer.parseInt(value);
                //if it's 0, return false
                if(i == 0){
                    return new exObject(false);
                } else {
                //else return true
                    return new exObject(true);
                }
            } catch(NumberFormatException e){}
            //it's not a number, parse it regularly
            return new exObject(Boolean.parseBoolean(value));
        } else if(type.equals("char")){
            if(value.length() != 0){
                throw new WrongClassTypeException("Value has more than 1 character, even though char type was specified");
            } else{
                return new exObject(value.charAt(0));
            }
        } else if(type.equals("int")){
            return new exObject(Integer.parseInt(value));
        } else if(type.equals("byte")){
            return new exObject(Byte.parseByte(value), byte.class);
        } else if(type.equals("short")){
            return new exObject(Short.parseShort(value));
        } else if(type.toLowerCase().equals("string")){
            return new exObject(value);
        } else{
            throw new WrongClassTypeException("Type '" + type + "' isn't supported in the getPrimitive function");
        }
    }

    /**
     * This is a convenience function to send a Classification enum instead of a String.
     * @param type The type of object expected
     * @param value The value of the object
     * @return a new exObject
     */
    public static exObject getPrimitive(Classification type, String value){
        return getPrimitive(type.toString().toLowerCase(), value);
    }

    public boolean isPrimitive(){
        if(classification == Classification.NULL || classification == Classification.OBJECT ||
                classification == Classification.STRING || classification == Classification.VOID){
            return false;
        }
        return true;
    }

    /**
     * This function allows you to return the value of the wrapped value as
     * the desired type, if it at all makes sense. Some combinations won't map,
     * however, such as returning most objects as a primitive, but most primitives
     * can be mapped to each other (including boxed primitives), and this
     * function allows that "cast" to occur.
     * @param type The desired type
     * @return a new exObject "cast" as {@code type}, unless a WrongClassType exception
     * is thrown.
     */
    public exObject getAsType(Classification type) throws WrongClassTypeException{
        if(classification == Classification.OBJECT || this.toObject() == null){
            if(!(objectVal.getClass().equals(Long.class) ||
                    objectVal.getClass().equals(Float.class) ||
                    objectVal.getClass().equals(Double.class) ||
                    objectVal.getClass().equals(Boolean.class) ||
                    objectVal.getClass().equals(Character.class) ||
                    objectVal.getClass().equals(Integer.class) ||
                    objectVal.getClass().equals(Byte.class) ||
                    objectVal.getClass().equals(Short.class))){
                throw new WrongClassTypeException("Cannot \"cast\" the value of this object to a primitive");
            }
        }
        if(type == Classification.LONG){
            return new exObject(((Long)this.toObject()).longValue());
        }
        if(type == Classification.FLOAT){
            return new exObject(((Float)this.toObject()).floatValue());
        }
        if(type == Classification.DOUBLE){
            return new exObject(((Double)this.toObject()).doubleValue());
        }
        if(type == Classification.BOOLEAN){
            if(this.toObject().toString().matches("[0-9]*")){
                if(((Long)this.toObject()) == 0){
                    return new exObject(false);
                } else {
                    return new exObject(true);
                }
            }
            return new exObject(Boolean.parseBoolean(this.toObject().toString()));
        }
        if(type == Classification.CHAR){
            if(classification == Classification.STRING && stringVal.length() == 1){
                return new exObject(stringVal.charAt(0));
            } else{
                return new exObject(((Character)this.toObject()).charValue());
            }
        }
        if(type == Classification.INT){
            return new exObject(((Integer)this.toObject()).intValue());
        }
        if(type == Classification.BYTE){
            return new exObject(((Byte)this.toObject()).byteValue(), byte.class);
        }
        if(type == Classification.SHORT){
            return new exObject(((Short)this.toObject()).shortValue());
        }
        if(type == Classification.STRING){
            return new exObject(this.toObject().toString());
        }
        throw new WrongClassTypeException("getAsType does not support casting the value to " + type.name());
    }

    /**
     * Gets the class type of this object. This is valid for all classifications,
     * except null types, because no class value can be derived for null.
     * If you know the class type, but the object itself is null, use the
     * {@code getNullType(Class)} function instead of the
     * {@code exObject(Object)} constructor.
     * type, and simply send null.
     * @return the class of the contained value.
     * @throws SparkTabCore.exObject.WrongClassTypeException
     */
    public Class getClassType() throws WrongClassTypeException{
        if(classification == Classification.NULL){
            throw new WrongClassTypeException("Value is null, no Class type");
        } else {
            return type;
        }
    }

    /**
     * @return The classification of this value, one of the {@code Classification} values.
     * @see Classification
     */
    public Classification getClassification(){
        return classification;
    }

    /**
     * This returns the toString representation of the value; regardless
     * of what type it is. The appropriate toString function is called
     * when the wrapped value is a primitive. Void returns an empty string,
     * and a null object returns "null".
     * @return a String
     */
    @Override
    public String toString(){
        if(classification == Classification.NULL){
            return "null";
        } else if(classification == Classification.VOID){
            return "";
        } else if(classification == Classification.OBJECT){
            return objectVal.toString();
        } else if(classification == Classification.STRING){
            return stringVal;
        } else if(classification == Classification.BOOLEAN){
            return Boolean.toString(booleanVal);
        } else if(classification == Classification.BYTE){
            return Byte.toString(byteVal);
        } else if(classification == Classification.CHAR){
            return Character.toString(charVal);
        } else if(classification == Classification.DOUBLE){
            return Double.toString(doubleVal);
        } else if(classification == Classification.FLOAT){
            return Float.toString(floatVal);
        } else if(classification == Classification.INT){
            return Integer.toString(intVal);
        } else if(classification == Classification.LONG){
            return Long.toString(longVal);
        } else if(classification == Classification.SHORT){
            return Short.toString(shortVal);
        } else {
            return "null";
        }
    }

    /**
     * This method returns the wrapped object as if it were an object,
     * regardless of whether or not it is a primitive. The only
     * exception is if it wraps a void type object, in which case
     * it returns null. This allows you to get the object and use it
     * without checking to see what it's type is. In most cases, this
     * is perfectly acceptable, because autoboxing will handle the
     * details of converting it to a primitive if needed.
     * @return
     */
    public Object toObject(){
        if(classification == Classification.NULL){
            return null;
        } else if(classification == Classification.VOID){
            return null;
        } else if(classification == Classification.OBJECT){
            return objectVal;
        } else if(classification == Classification.STRING){
            return stringVal;
        } else if(classification == Classification.BOOLEAN){
            return Boolean.valueOf(booleanVal);
        } else if(classification == Classification.BYTE){
            return Byte.valueOf(byteVal);
        } else if(classification == Classification.CHAR){
            return Character.valueOf(charVal);
        } else if(classification == Classification.DOUBLE){
            return Double.valueOf(doubleVal);
        } else if(classification == Classification.FLOAT){
            return Float.valueOf(floatVal);
        } else if(classification == Classification.INT){
            return Integer.valueOf(intVal);
        } else if(classification == Classification.LONG){
            return Long.valueOf(longVal);
        } else if(classification == Classification.SHORT){
            return Short.valueOf(shortVal);
        } else {
            return null;
        }
    }

    /*/ Void Type **************************************************************/
    /**
     * Since it is not possible to send a void type object, you must use this
     * static method that returns a new {@code exObject}, instantiated as a
     * void type object.
     * @return a new void type object
     */
    public static exObject getVoidType(){
        exObject o = new exObject();
        o.type = void.class;
        o.classification = Classification.VOID;
        return o;
    }

    /**
     * @return Whether or not the wrapped value is a void type object
     */
    public boolean isVoidType(){
        return classification == Classification.VOID;
    }

    /*/ Null Type **************************************************************/
    /**
     * Since {@code new exObject(null)} would return a null value with classification
     * {@code OBJECT}, use this static method to return a new null type {@code exObject}
     * instead. If you know the type of the object, but it is null, (or unset, in the
     * case of primitives), you should use {@code getNullType(Class)} instead.
     * @return a null value object
     * @see getNullType(Class)
     */
    public static exObject getNullType() throws WrongClassTypeException{
        return getNullType(null);
    }

    /**
     * This static function returns a null object with type {@code type}. There
     * actually is no equivalent null value for a primitive, so if {@code type}
     * is a primitive (or void), an exception is thrown.
     * @param type
     * @return
     */
    public static exObject getNullType(Class type) throws WrongClassTypeException{
        exObject o = new exObject();
        o.type = type;
        if(type.equals(byte.class)){
            throw new WrongClassTypeException("Assigning null to byte primitive");
        } else if(type.equals(short.class)){
            throw new WrongClassTypeException("Assigning null to short primitive");
        } else if(type.equals(int.class)){
            throw new WrongClassTypeException("Assigning null to int primitive");
        } else if(type.equals(long.class)){
            throw new WrongClassTypeException("Assigning null to long primitive");
        } else if(type.equals(float.class)){
            throw new WrongClassTypeException("Assigning null to float primitive");
        } else if(type.equals(double.class)){
            throw new WrongClassTypeException("Assigning null to double primitive");
        } else if(type.equals(boolean.class)){
            throw new WrongClassTypeException("Assigning null to boolean primitive");
        } else if(type.equals(char.class)){
            throw new WrongClassTypeException("Assigning null to char primitive");
        } else if(type.equals(void.class)){
            throw new WrongClassTypeException("Assigning null to void");
        } else if(type == null){
            o.classification = Classification.NULL;
        } else if(type.equals(String.class)){
            o.classification = Classification.STRING;
        } else{
            o.classification = Classification.OBJECT;
        }
        return o;
    }

    /**
     * @return Whether or not the wrapped value is a null type object
     */
    public boolean isNullType(){
        return classification == Classification.NULL;
    }
    /*/ Object Type ***********************************************************/

    public exObject(Object o){
        type = o.getClass();
        objectVal = o;
        classification = Classification.OBJECT;
    }
    public boolean isObjectType(){
        return classification == Classification.OBJECT;
    }
    public Object getObject() throws WrongClassTypeException{
        if(classification != Classification.OBJECT){
            throw new WrongClassTypeException("Value type is " + classification.name() + ", asked for OBJECT");
        }
        return objectVal;
    }
    public void setObject(Object o) throws WrongClassTypeException{
        if(classification != Classification.OBJECT){
            throw new WrongClassTypeException("Trying to set OBJECT to  " + classification.name() + " type value");
        }
        objectVal = o;
    }

    /*/ String Type ***********************************************************/

    public exObject(String s){
        type = String.class;
        stringVal = s;
        classification = Classification.STRING;
    }
    public boolean isStringType(){
        return classification == Classification.STRING;
    }
    public String getString() throws WrongClassTypeException{
        if(classification != Classification.STRING){
            throw new WrongClassTypeException("Value type is " + classification.name() + ", asked for STRING");
        }
        return stringVal;
    }
    public void setString(String s) throws WrongClassTypeException{
        if(classification != Classification.STRING){
            throw new WrongClassTypeException("Trying to set STRING to " + classification.name() + " type value");
        }
        stringVal = s;
    }

    /*/ Long Type ***********************************************************/
    /**
     * Constructs a new {@code exObject} of type {@code long}
     */
    public exObject(long l){
        type = long.class;
        longVal = l;
        classification = Classification.LONG;
    }
    /**
     * @return whether or not the wrapped {@code exObject} is of type {@code long}
     */
    public boolean isLongType(){
        return classification == Classification.LONG;
    }
    /**
     * Gets the wrapped value.
     * @throws WrongClassTypeException if the wrapped value is not of type long
     */
    public long getLong() throws WrongClassTypeException{
        if(classification != Classification.LONG){
            throw new WrongClassTypeException("Value type is " + classification.name() + ", asked for LONG");
        }
        return longVal;
    }
    /**
     * Sets the wrapped value to l
     * @param l the long to set the wrapped value to.
     * @throws WrongClassTypeException
     */
    public void setLong(long l) throws WrongClassTypeException{
        if(classification != Classification.LONG){
            throw new WrongClassTypeException("Trying to set LONG to " + classification.name() + " type value");
        }
        longVal = l;
    }

    /*/ Float Type ***********************************************************/
    /**
     * Constructs a new {@code exObject} of type {@code float}
     */
    public exObject(float f){
        type = float.class;
        floatVal = f;
        classification = Classification.FLOAT;
    }
    /**
     * @return whether or not the wrapped {@code exObject} is of type {@code float}
     */
    public boolean isFloatType(){
        return classification == Classification.FLOAT;
    }
    /**
     * Gets the wrapped value.
     * @throws WrongClassTypeException if the wrapped value is not of type float
     */
    public float getFloat() throws WrongClassTypeException{
        if(classification != Classification.FLOAT){
            throw new WrongClassTypeException("Value type is " + classification.name() + ", asked for FLOAT");
        }
        return floatVal;
    }
    /**
     * Sets the wrapped value to f
     * @param f the float to set the wrapped value to.
     * @throws WrongClassTypeException
     */
    public void setFloat(float f) throws WrongClassTypeException{
        if(classification != Classification.FLOAT){
            throw new WrongClassTypeException("Trying to set FLOAT to " + classification.name() + " type value");
        }
        floatVal = f;
    }

    /*/ Double Type ***********************************************************/
    /**
     * Constructs a new {@code exObject} of type {@code double}
     */
    public exObject(double d){
        type = double.class;
        doubleVal = d;
        classification = Classification.DOUBLE;
    }
    /**
     * @return whether or not the wrapped {@code exObject} is of type {@code double}
     */
    public boolean isDoubleType(){
        return classification == Classification.DOUBLE;
    }
    /**
     * Gets the wrapped value.
     * @throws WrongClassTypeException if the wrapped value is not of type double
     */
    public double getDouble() throws WrongClassTypeException{
        if(classification != Classification.DOUBLE){
            throw new WrongClassTypeException("Value type is " + classification.name() + ", asked for DOUBLE");
        }
        return doubleVal;
    }
    /**
     * Sets the wrapped value to d
     * @param d the double to set the wrapped value to.
     * @throws WrongClassTypeException
     */
    public void setDouble(double d) throws WrongClassTypeException{
        if(classification != Classification.DOUBLE){
            throw new WrongClassTypeException("Trying to set DOUBLE to " + classification.name() + " type value");
        }
        doubleVal = d;
    }

    /*/ Boolean Type ***********************************************************/
    /**
     * Constructs a new {@code exObject} of type {@code boolean}
     */
    public exObject(boolean b){
        type = boolean.class;
        booleanVal = b;
        classification = Classification.BOOLEAN;
    }
    /**
     * @return whether or not the wrapped {@code exObject} is of type {@code boolean}
     */
    public boolean isBooleanType(){
        return classification == Classification.BOOLEAN;
    }
    /**
     * Gets the wrapped value.
     * @throws WrongClassTypeException if the wrapped value is not of type boolean
     */
    public boolean getBoolean() throws WrongClassTypeException{
        if(classification != Classification.BOOLEAN){
            throw new WrongClassTypeException("Value type is " + classification.name() + ", asked for BOOLEAN");
        }
        return booleanVal;
    }
    /**
     * Sets the wrapped value to b
     * @param b the boolean to set the wrapped value to.
     * @throws WrongClassTypeException
     */
    public void setBoolean(boolean b) throws WrongClassTypeException{
        if(classification != Classification.BOOLEAN){
            throw new WrongClassTypeException("Trying to set BOOLEAN to " + classification.name() + " type value");
        }
        booleanVal = b;
    }

    /*/ Char Type ***********************************************************/
    /**
     * Constructs a new {@code exObject} of type {@code char}
     */
    public exObject(char c){
        type = char.class;
        charVal = c;
        classification = Classification.CHAR;
    }
    /**
     * @return whether or not the wrapped {@code exObject} is of type {@code char}
     */
    public boolean isCharType(){
        return classification == Classification.CHAR;
    }
    /**
     * Gets the wrapped value.
     * @throws WrongClassTypeException if the wrapped value is not of type char
     */
    public char getChar() throws WrongClassTypeException{
        if(classification != Classification.CHAR){
            throw new WrongClassTypeException("Value type is " + classification.name() + ", asked for CHAR");
        }
        return charVal;
    }
    /**
     * Sets the wrapped value to c
     * @param c the char to set the wrapped value to.
     * @throws WrongClassTypeException
     */
    public void setChar(char c) throws WrongClassTypeException{
        if(classification != Classification.CHAR){
            throw new WrongClassTypeException("Trying to set CHAR to " + classification.name() + " type value");
        }
        charVal = c;
    }

    /*/ Int Type ***********************************************************/
    /**
     * Constructs a new {@code exObject} of type {@code int}
     */
    public exObject(int i){
        type = int.class;
        intVal = i;
        classification = Classification.INT;
    }
    /**
     * @return whether or not the wrapped {@code exObject} is of type {@code int}
     */
    public boolean isIntType(){
        return classification == Classification.INT;
    }
    /**
     * Gets the wrapped value.
     * @throws WrongClassTypeException if the wrapped value is not of type int
     */
    public int getInt() throws WrongClassTypeException{
        if(classification != Classification.INT){
            throw new WrongClassTypeException("Value type is " + classification.name() + ", asked for INT");
        }
        return intVal;
    }
    /**
     * Sets the wrapped value to i
     * @param i the int to set the wrapped value to.
     * @throws WrongClassTypeException
     */
    public void setInt(int i) throws WrongClassTypeException{
        if(classification != Classification.INT){
            throw new WrongClassTypeException("Trying to set INT to " + classification.name() + " type value");
        }
        intVal = i;
    }

    /*/ Byte Type ***********************************************************/
    /**
     * Since bytes are ints, it is not possible to create a byte object with the same
     * signature as the int constructor. For this reason, bytes have a special constructor
     * that allows them to be created. Alternately, you can use the static byte creator.
     * @param b The byte to store
     * @param c A Class object. Can be any class, but typically, use {@code byte.class} to
     * make it more obvious.
     */
    public exObject(byte b, Class c){
        type = byte.class;
        byteVal = b;
        classification = Classification.BYTE;
    }

    /**
     * The static version of the byte constructor
     * @param b the byte to wrap
     * @return a new exObject
     */
    public static exObject getByteType(byte b){
        exObject o = new exObject();
        o.type = byte.class;
        o.byteVal = b;
        o.classification = Classification.BYTE;
        return o;
    }
    /**
     * @return whether or not the wrapped {@code exObject} is of type {@code byte}
     */
    public boolean isByteType(){
        return classification == Classification.BYTE;
    }
    /**
     * Gets the wrapped value.
     * @throws WrongClassTypeException if the wrapped value is not of type byte
     */
    public byte getByte() throws WrongClassTypeException{
        if(classification != Classification.BYTE){
            throw new WrongClassTypeException("Value type is " + classification.name() + ", asked for BYTE");
        }
        return byteVal;
    }
    /**
     * Sets the wrapped value to b
     * @param b the byte to set the wrapped value to.
     * @throws WrongClassTypeException
     */
    public void setByte(byte b) throws WrongClassTypeException{
        if(classification != Classification.BYTE){
            throw new WrongClassTypeException("Trying to set BYTE to " + classification.name() + " type value");
        }
        byteVal = b;
    }

    /*/ Short Type ***********************************************************/
    /**
     * Constructs a new {@code exObject} of type {@code short}
     */
    public exObject(short s){
        type = short.class;
        shortVal = s;
        classification = Classification.SHORT;
    }
    /**
     * @return whether or not the wrapped {@code exObject} is of type {@code short}
     */
    public boolean isShortType(){
        return classification == Classification.SHORT;
    }
    /**
     * Gets the wrapped value.
     * @throws WrongClassTypeException if the wrapped value is not of type short
     */
    public short getShort() throws WrongClassTypeException{
        if(classification != Classification.SHORT){
            throw new WrongClassTypeException("Value type is " + classification.name() + ", asked for SHORT");
        }
        return shortVal;
    }
    /**
     * Sets the wrapped value to s
     * @param s the short to set the wrapped value to.
     * @throws WrongClassTypeException
     */
    public void setShort(short s) throws WrongClassTypeException{
        if(classification != Classification.SHORT){
            throw new WrongClassTypeException("Trying to set SHORT to " + classification.name() + " type value");
        }
        shortVal = s;
    }
}
