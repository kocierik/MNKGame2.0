package mnkgame;

import java.util.ArrayList;
import java.util.List;

public class Albero {

    private MNKBoard value;
    private List<Albero> children;
    private Albero parent;

    public Albero(MNKBoard value) {
        this.value = value;
        children = new ArrayList<Albero>();
    }
    
    public Albero addChild(MNKBoard child) {
        Albero tmp = new Albero(child);
        tmp.setParent(this);
        children.add(tmp);
        return tmp;
    }
    
    public List<Albero> getChildren() {
        return children;
    }
    
    public MNKBoard getValue() {
        return value;
    }
    
    public void setValue(MNKBoard value) {
        this.value = value;
    }
    
    public void setParent(Albero parent) {
        this.parent = parent;
    }
    
    public Albero getParent() {
        return parent;
    }
 
    public boolean isLeaf() {
        if(children.size() == 0)
            return true;
        else
            return false;
    }

    public String toString() {
        switch(value.gameState()){
            case OPEN:
                return "State: open";
            case DRAW:
                return "State: draw";
            case WINP1:
                return "State: win p1";
            case WINP2:
                return "State: win p2";
            default:
                return "comeee";
        }
    }
}
