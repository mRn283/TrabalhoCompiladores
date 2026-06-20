package pacotememe;

import java.util.ArrayList;
import java.util.List;

public class ASTNode {
    private String name;
    private String value;
    private int line;
    private int column;
    private List<ASTNode> children;

    public ASTNode(String name) {
        this(name, null, 0, 0);
    }

    public ASTNode(String name, String value) {
        this(name, value, 0, 0);
    }

    public ASTNode(String name, String value, int line, int column) {
        this.name = name;
        this.value = value;
        this.line = line;
        this.column = column;
        this.children = new ArrayList<>();
    }

    public void addChild(ASTNode child) {
        if (child != null) {
            this.children.add(child);
        }
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        if (value != null && !value.isEmpty()) {
            return name + ": \"" + value + "\"";
        }
        return name;
    }
}
