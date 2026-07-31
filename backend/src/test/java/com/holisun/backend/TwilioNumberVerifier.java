package com.holisun.backend;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.ValidationRequest;
import com.twilio.type.PhoneNumber;

public class TwilioNumberVerifier {
    public static void main(String[] args) {
        String accountSid = "YOUR_ACCOUNT_SID_HERE";
        String authToken = "YOUR_AUTH_TOKEN_HERE";
        String myPhoneNumber = "+40774585226"; // Your real phone number
        Twilio.init(accountSid, authToken);
        try {
            // THIS is the line that verifies your number! Do not change it to Message.creator
            ValidationRequest validationRequest = ValidationRequest.creator(new PhoneNumber(myPhoneNumber))
                    .setFriendlyName("My Dev Phone")
                    .create();
            System.out.println("\nSUCCESS! VALIDATION CODE: " + validationRequest.getValidationCode());
            System.out.println("Twilio is calling you right now! Answer and type this code in.\n");
        } catch (Exception e) {
            System.err.println("\n[ERROR] Twilio rejected the request: " + e.getMessage());
        }
    }
}
