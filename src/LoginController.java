public class LoginController {
    public String login(String name, String pwd) {
        if ("admin".equals(name) && "123".equals(pwd)) {
            return "Login Success";
        }
        return "Login Failed";
    }
}