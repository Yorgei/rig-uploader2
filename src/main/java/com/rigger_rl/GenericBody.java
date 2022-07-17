// Using this for now cus i really cant be fucked at 1am

package com.rigger_rl;
import lombok.Data;
import net.runelite.api.vars.AccountType;

@Data
class GenericBody {
    // All string just so it doesn't send useless info
    private String type;
    private String rsn;
    private AccountType accountType;
    private String enemy;
    private String area;
    private String image;
    private String itemName;
    private String price;
    private String quantity;
    private String skill;
    private String level;
    private String totalLevel;
    private String questPoints;
    private String questName;
    private String logCount;
}