package config;

public class Session {
    private static Session instance;
    private int id;
    private String name;
    private String email;
    private String department;
    
    // NEW FIELDS FOR STUDENT DATA
    private String course;
    private String section;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    // GETTERS AND SETTERS FOR STUDENT DATA
    public String getcourse() { return course; }
    public void setcourse(String course) { this.course = course; }

    public String getsection() { return section; }
    public void setsection(String section) { this.section = section; }

    // EXISTING CORE METHODS
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}