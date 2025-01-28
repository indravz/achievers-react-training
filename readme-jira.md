

Here’s the reformatted version of your document, aligned with the formatting and wording of the provided reference:

---

# Instructions for Performing Inbound SSO to Orion

## Overview  
These instructions outline the steps to configure and perform inbound Single Sign-On (SSO) for Orion using the Goldman Sachs Advisor Solutions (GSAS) platform.

## Contents  
1. Initiate SSO  
2. User Authorization  
3. Post-Authorization Steps  

---

### 1. Initiate SSO  
To initiate the SSO process:  
- Click the **Set SSO to GSAS** button on the Orion interface.  
- This action redirects the user to the GSAS User Authorization URL:  
  `https://advisorsolutions.gs.com/app/authorizations/<client-id>`  
- The user is prompted to authorize Orion SSO to GSAS.

---

### 2. User Authorization  
Once redirected:  
- The user signs in and clicks **I authorize** to grant access.  
- The page redirects back to Orion with a response:  
  ```json
  {
    "loginid": "string",
    "expiresIn": int
  }
  ```  
- Orion uses this response to perform user mapping.

---

### 3. Post-Authorization Steps  

#### Step 1 - Get Access Token  
Orion requests an access token by making a call to the token endpoint using the provided `clientId` and `clientSecret`:  
```bash
curl -u <clientId>:<clientSecret> --data "grant_type=client_credentials" https://idfs.gs.com/as/token.oauth2?access_token_manager_id=<access_manager_id>
```  
A successful response returns an access token:  
```json
{
  "access_token": "<redacted>",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

#### Step 2 - Get User Token  
Using the access token obtained above, Orion requests a user-specific token:  
- Make a POST request to `/api/v2/oauth-apps/${clientId}/tokens` with the following body:  
  ```json
  {
    "loginid": "string"
  }
  ```

#### Step 3 - Set Cookie  
The user token is used to set a cookie in the user's browser:  
- Make a POST request to `/api/v2/oauth-apps/${clientId}/set-cookie`.  
- Orion retrieves the cookie and uses it to open the GSAS home page in a new browser tab.

---

### Note  
The `clientSecret` must be safeguarded and is not retained by GSAS staff. Tokens are valid for a limited time and must be refreshed upon expiration. Access can be revoked by the user or by GSAS administrators.

--- 

Let me know if you need further refinements!
Because this is a shared VPC setup and there are no firm recommended strategies, we are using the AWS native ACLs, security group rules, and other controls to provide limited access to everything (least privileges) for services running on AWS. @Adam will come back on the ticket after discussing with Varun.

