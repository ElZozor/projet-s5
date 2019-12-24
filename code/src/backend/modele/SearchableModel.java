package backend.modele;

import javax.swing.table.TableModel;

public abstract class SearchableModel implements TableModel {

    public abstract TableModel retrieveSearchModel(final String searched);

}
