package model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Abanoub Wagdy on 7/7/2015.
 */

/**
 * This class holds LegalRepresentative additional attributes
 * Note:this class contains LegalRepresentativeLookup object and details about him like start date , end date,personal phto ...etc
 */
public class LegalRepresentativeLookup {

    public String url;
    @SerializedName("Id")
    public String ID;
    @SerializedName("Name")
    public String Name;
    @SerializedName("Nationality__c")
    public String Nationality;
    @SerializedName("Personal_Photo__pc")
    public String Personal_Photo;
    public CurrentPassport currentPassport;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getNationality() {
        return Nationality;
    }

    public void setNationality(String nationality) {
        Nationality = nationality;
    }

    public String getPersonal_Photo() {
        return Personal_Photo;
    }

    public void setPersonal_Photo(String personal_Photo) {
        Personal_Photo = personal_Photo;
    }

    public CurrentPassport getCurrentPassport() {
        return currentPassport;
    }

    public void setCurrentPassport(CurrentPassport currentPassport) {
        this.currentPassport = currentPassport;
    }
}
