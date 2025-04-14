import java.util.regex.Pattern;

public class UserFactory {
    private final static Pattern facultyNumberPattern = Pattern.compile("^\\d{9}$");
    private final static Pattern egnPattern = Pattern.compile("^\\d{10}$");
    private final static Pattern emailPattern = Pattern.compile("^[a-z]+@tu-sofia.bg$");
    private final static Pattern passwordPattern = Pattern.compile("^.{5,}$");

    public static User createUser(UserType userType, String username, String password) throws CredentialException, IllegalArgumentException{
        switch(userType){
            case STUDENT:{
                if(!facultyNumberPattern.matcher(username).matches()){
                    throw new CredentialException("Invalid faculty number format!");
                }
                if(!egnPattern.matcher(password).matches()){
                    throw new CredentialException("Invalid EGN format!");
                }
                return new Student(username, password);
            }

            case TEACHER:{
                if(!emailPattern.matcher(username).matches()){
                    throw new CredentialException("Invalid email format!");
                }
                if(!passwordPattern.matcher(password).matches()){
                    throw new CredentialException("Invalid password format!");
                }
                return new Teacher(username, password);
            }

            default:
                throw new IllegalArgumentException();
        }
    }
}
