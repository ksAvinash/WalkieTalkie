package it.justdevelop.walkietalkie.helpers;

public class profile_object {
    private String phoneno, profile_pic, name;

    public profile_object(String phoneno, String name, String profile_pic) {
        this.phoneno = phoneno;
        this.profile_pic = profile_pic;
        this.name = name;
    }

    public void setPhoneno(String phoneno) {
        this.phoneno = phoneno;
    }

    public void setProfile_pic(String profile_pic) {
        this.profile_pic = profile_pic;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneno() {

        return phoneno;
    }

    public String getProfile_pic() {
        return profile_pic;
    }

    public String getName() {
        return name;
    }
}
