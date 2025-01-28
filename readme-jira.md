Here’s the updated version of your document, reflecting the changes and focusing on SSO to GSAS from Orion:

---

# Instructions for Configuring SSO to GSAS from Orion  

## Overview  
These instructions guide you through the process of configuring and performing Single Sign-On (SSO) from Orion to Goldman Sachs Advisor Solutions (GSAS). The integration uses OAuth 2.0 to authenticate users and allow access to GSAS services.

---

## Contents  
1. Configure SSO to GSAS  
2. User Authorization in GSAS  
3. Post-Authorization Steps  

---

### 1. Configure SSO to GSAS  
To initiate the SSO process:  
- Click the **Set SSO to GSAS** button on the Orion interface.  
- This action redirects the user to the GSAS User Authorization URL:  
  `https://advisorsolutions.gs.com/app/authorizations/<client-id>`  
- The user is prompted to authorize Orion to access their GSAS account.  

This step ensures that Orion is configured to request access to GSAS on behalf of the user.

---

### 2. User Authorization in GSAS  
Once redirected to the GSAS authorization page:  
- The user logs in and clicks **I authorize** to grant Orion access to GSAS.  
- Upon successful authorization, GSAS redirects the user back to Orion with a response containing key information:  
  ```json
  {
    "loginid": "string",
    "expiresIn": int
  }
  ```  
- Orion processes the response to map the user for SSO purposes.

---

### 3. Post-Authorization Steps  

#### Step 1 - Obtain Access Token  
Orion generates an access token to interact with GSAS APIs by making a call to the token endpoint using the `clientId` and `clientSecret`:  
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
This access token is used for further authentication steps.

---

#### Step 2 - Generate User Token  
Using the access token, Orion retrieves a user-specific token to proceed with the SSO process:  
- Make a POST request to the GSAS endpoint:  
  ```bash
  /api/v2/oauth-apps/${clientId}/tokens
  ```  
  Request body:  
  ```json
  {
    "loginid": "string"
  }
  ```  
- A successful response provides a token for the logged-in user.

---

#### Step 3 - Set User Cookie  
The token obtained in Step 2 is used to set a cookie in the user’s browser:  
- Make a POST request to:  
  ```bash
  /api/v2/oauth-apps/${clientId}/set-cookie
  ```  
- Orion uses this cookie to authenticate the user and open the GSAS home page in a new browser tab.

---

### Important Notes  
- The `clientSecret` is confidential and must be securely stored by Orion administrators.  
- Access tokens have a limited validity and must be refreshed periodically.  
- Users can revoke access through their GSAS settings, and administrators can manage permissions for specific integrations.  

--- 

Let me know if you need additional adjustments!

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

