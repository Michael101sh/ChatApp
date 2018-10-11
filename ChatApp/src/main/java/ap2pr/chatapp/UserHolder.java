/* Michael Shachar
   Ido Ben El
   Michal Bar Ilan
   Betzalel Moshkovitz */

package ap2pr.chatapp;


public class UserHolder {
    private static String userNickname;

    public UserHolder(String mail) {
        userNickname = mail.replace("@gmail.com", "");
    }

    public static String getUserNickname() {
        return userNickname;
    }
}
