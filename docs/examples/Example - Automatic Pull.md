# Example (Backend-Application):

**Prerequisites**:
- Webstorm with VisualBoost-Plugin
- Existing VisualBoost-Project

**Starting Point:**

We already created a Model `Person` in our VB-Project that contains a simple `GET` function.
This function will read a `Person` by its ID from the database and return the record to the client.


During the next steps, we will add a `POST`, `PUT` and `DELETE` function that will be helps us to create, update and delete a `Person`.

![](https://plugins.jetbrains.com/files/24273/60888-page/2b395fd5-569c-4044-b36c-1ee5e1e8e38d)

**Step 1:**

Double-click the model `Person` and navigate to `Functions`. Now click on the `Add-Button`.

![](https://plugins.jetbrains.com/files/24273/60888-page/c8ca955b-edb0-41e1-8a96-a57da3b8c758)

**Step 2:**

Select `CREATE`, `UPDATE` and `DELETE` and confirm it by clicking the `Add` button.

![](https://plugins.jetbrains.com/files/24273/60888-page/b40972b4-3cf3-48d7-85fe-0c25e1f20f6d)

The result will  look like the image below. Update the model `Person` by clicking on the `Update` on thr right bottom corner.

![](https://plugins.jetbrains.com/files/24273/60888-page/a491142e-2e3e-46c9-9394-a17a0da7907d)

**Step 3:**

Now Build the updated architecture by clicking on `Run > Build`.

![](https://plugins.jetbrains.com/files/24273/60888-page/3804e0b2-d36a-4b5e-8817-c00008ce2196)

**Step 4:**

Enter a commit message and confirm it by pressing `commit`.

![](https://plugins.jetbrains.com/files/24273/60888-page/907590fb-b564-4b49-9533-f61c54350504)

VisualBoost will generate your project and push the result into your GIT repositories. If the build was successfull, your will see a screen like below.
Press the `Close` button now to tell your VisualBoost-Plugin to automatically pull the new code.

![](https://plugins.jetbrains.com/files/24273/60888-page/a745c3ed-6e4d-40b1-99dd-70fdb126b63e)

> Attention: A `git pull` will be only executed if you have set the right `Project ID` and `Project type` in the settings.

After the plugin pulled the new content from your git repository, you will see a notification on the right bottom corner of your IDE:

![](https://plugins.jetbrains.com/files/24273/60888-page/e6f30136-cd9e-4ff7-ba1e-7ee2840cb0e7)

Furthermore, the router of our model `Person` contains the currently added functions.

![](https://plugins.jetbrains.com/files/24273/60888-page/c29f6753-d588-44fd-8e20-9379fc33e9be)