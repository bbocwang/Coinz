package uk.ac.ed.coinz;

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
