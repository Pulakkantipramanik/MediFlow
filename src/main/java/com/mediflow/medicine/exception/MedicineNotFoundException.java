package com.mediflow.medicine.exception;

public class MedicineNotFoundException extends RuntimeException{
    public  MedicineNotFoundException(String message)//this is constructor , when we write throw new Medi.. that time only this constructor called
    {
        super(message);
    }
}
