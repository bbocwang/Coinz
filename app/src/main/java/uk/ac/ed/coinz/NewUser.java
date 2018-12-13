package uk.ac.ed.coinz;

/*This is a NewUser class
*
* It is used in store the new user register date
* to implement the new user offer bonus feature
* */
class NewUser {
    private String newUserId;
    private String registerDate;

    NewUser(){}


    NewUser(String newUserId, String registerDate) {
        this.newUserId = newUserId;
        this.registerDate = registerDate;
    }

    public String getNewUserId() {
        return newUserId;
    }

    public String getRegisterDate() {
        return registerDate;
    }
}
