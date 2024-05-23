The VisualBoost plugin simply displays the VisualBoost interface by using `JBCefBrowser`.
Android Studios' runtime environment does not contain JCEF (Java Chromium Embedded Framework) by default.

You need to change the runtime environment of Android Studio to `JBR with JCEF`.

### How to install `JBR with JCEF`?

---

**Step 1:**  Navigate to **Help > Find Action**

![](https://plugins.jetbrains.com/files/24273/60869-page/b32497a2-f9db-4806-9fb9-e2c65c88535a)

**Step 2:** Find and select **Choose Boot Java Runtime for the IDE...**

![](https://plugins.jetbrains.com/files/24273/60869-page/b62599d9-aa4f-4950-b7c8-c85463b8289b)

**Step 3:** Select a Runtime environment that contains `JCEF`

![](https://plugins.jetbrains.com/files/24273/60869-page/ef169b80-5b55-4480-b61b-4510c6591f30)

**Step 4:** Confirm and restart the IDE.

VisualBoost will be displayed in the ToolWindow

![](https://plugins.jetbrains.com/files/24273/60869-page/0a8c19df-2aa2-4972-aa29-d51d1776748a)

### I still can't see VisualBoost in Android Studio. What can I do?

If you see a white or blank screen in your VisualBoost-ToolWindow, please add the property `ide.browser.jcef.sandbox.enable=false` to your IDE.

**Step 1:**  Navigate to **Help > Find Action**

![](https://plugins.jetbrains.com/files/24273/60869-page/b32497a2-f9db-4806-9fb9-e2c65c88535a)

**Step 2:** Find and select **Edit Custom Properties...**

![](https://plugins.jetbrains.com/files/24273/60869-page/8293f55e-16e8-4dad-8baf-320b038c7299)

**Step 3:** Add `ide.browser.jcef.sandbox.enable=false` to your `idea.properties` file.

![](https://plugins.jetbrains.com/files/24273/60869-page/89cf9b01-fa40-492f-b14b-0ac12483b360)

**Step 4:** Restart Android Studio