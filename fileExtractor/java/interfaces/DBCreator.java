package interfaces;

import java.io.File;
import java.util.Hashtable;

public interface DBCreator {
    public File[] fileGetter(String dir);
    public void insert(Hashtable<String, String> protocolLinks);
}