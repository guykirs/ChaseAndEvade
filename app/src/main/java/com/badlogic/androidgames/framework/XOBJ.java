package com.badlogic.androidgames.framework;

public class XOBJ<T> {

    public T Value;

    public  XOBJ() {

    }
    public XOBJ(T value) {
        this.Value = value;
    }
    //
    // Method: Ref()
    // Purpose: returns a Reference Parameter object using the XOBJ value
    //
    public XREF<T> Ref()
    {
        XREF<T> ref = new XREF<T>();
        ref.Obj = this;
        return(ref);
    }
    //
    // Method: Out()
    // Purpose: returns an Out Parameter Object using the XOBJ value
    //
    public XOUT<T> Out()
    {
        XOUT<T> out = new XOUT<T>();
        out.Obj = this;
        return(out);
    }
    //
    // Method get()
    // Purpose: returns the value
    // Note: Because this is combersome to edit in the code,
    // the Value object has been made public
    //
    public T get() {
        return Value;
    }
    //
    // Method get()
    // Purpose: sets the value
    // Note: Because this is combersome to edit in the code,
    // the Value object has been made public
    //
    public void set(T anotherValue) {
        Value = anotherValue;
    }


    @Override
    public String toString() {
        return Value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return Value.equals(obj);
    }

    @Override
    public int hashCode() {
        return Value.hashCode();
    }
}