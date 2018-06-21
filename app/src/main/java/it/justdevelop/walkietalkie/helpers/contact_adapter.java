package it.justdevelop.walkietalkie.helpers;

public class contact_adapter {
    private String id, name;
    private String[] phonenos;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhonenos(String[] phonenos) {
        this.phonenos = phonenos;
    }

    public String getId() {

        return id;
    }

    public String getName() {
        return name;
    }

    public String[] getPhonenos() {
        return phonenos;
    }

    public contact_adapter(String id, String name, String[] phonenos) {

        this.id = id;
        this.name = name;
        this.phonenos = phonenos;
    }
}
