package backend.data;

import org.json.JSONObject;

public abstract class ProjectTable {

    public abstract Long getID();

    public abstract JSONObject toJSON();

}
