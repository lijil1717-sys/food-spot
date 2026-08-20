document.addEventListener("DOMContentLoaded", () => {
    // --- 1. CONFIGURATION: DYNAMIC LOCAL / LIVE API ROUTING ---
    const API_URL = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
        ? 'http://localhost:8081/api/auth'
        : '/api/auth';

    // Nav Links
    const toRegisterLink = document.getElementById("toRegisterLink");
    const toLoginLink = document.getElementById("toLoginLink");
    const forgotPasswordLink = document.getElementById("forgotPasswordLink");
    const backToLoginFromForgot = document.getElementById("backToLoginFromForgot");

    // Form Containers
    const loginForm = document.getElementById("emailForm");
    const registerForm = document.getElementById("registerForm");
    const forgotForm = document.getElementById("forgotPasswordForm");

    // Inline verification action button elements inside HTML
    const sendCodeInlineBtn = registerForm ? registerForm.querySelector(".verify-action-btn") : null;
    const forgotVerifyBtn = forgotForm ? document.getElementById("forgotVerifyBtn") : null;

    // Password Validation Regex Rule: Min 6 chars, 1 Uppercase, 1 Lowercase, 1 Number, 1 Special Char
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!\%*?&]{6,}$/;

    /**
     * Helper Utility: Displays status messages on the specific form panel
     */
    function displayMessage(formElement, messageText, isSuccess) {
        if (!formElement) return;
        const messageBox = formElement.querySelector(".form-status-message");
        if (!messageBox) return;

        messageBox.classList.remove("hidden-msg", "msg-success", "msg-error");
        messageBox.textContent = messageText;
        
        if (isSuccess) {
            messageBox.classList.add("msg-success");
            messageBox.style.color = "green";
        } else {
            messageBox.classList.add("msg-error");
            messageBox.style.color = "red";
        }
    }

    /**
     * Helper Utility: Clears validation logs across forms
     */
    function clearMessages() {
        document.querySelectorAll(".form-status-message").forEach(box => {
            box.textContent = "";
            box.classList.add("hidden-msg");
            box.classList.remove("msg-success", "msg-error");
        });
    }

    /**
     * Helper Utility: Dynamic CSS Sliding Transition Animator Engine
     */
    function showPanel(targetForm, exitDirection) {
        clearMessages(); 
        const allForms = [loginForm, registerForm, forgotForm];
        
        allForms.forEach(form => {
            if (!form) return;
            
            if (form === targetForm) {
                form.classList.remove("hidden-panel");
                form.classList.add("active-panel");
                form.style.opacity = "1";
                form.style.transform = "translateX(0)";
                form.style.pointerEvents = "auto";
                form.style.position = "relative";
                form.style.visibility = "visible";
                form.style.height = "auto";
            } else if (form.classList.contains("active-panel")) {
                form.classList.remove("active-panel");
                form.classList.add("hidden-panel");
                form.style.opacity = "0";
                form.style.transform = `translateX(${exitDirection}px)`;
                form.style.pointerEvents = "none";
                form.style.position = "absolute";
                form.style.visibility = "hidden";
                form.style.height = "0";
            }
        });
    }

    // --- 2. INTERACTIVE SLIDING TAB NAVIGATION RULES ---
    if (toRegisterLink) { toRegisterLink.addEventListener("click", (e) => { e.preventDefault(); showPanel(registerForm, -50); }); }
    if (toLoginLink) { toLoginLink.addEventListener("click", (e) => { e.preventDefault(); showPanel(loginForm, 50); }); }
    if (forgotPasswordLink) { forgotPasswordLink.addEventListener("click", (e) => { e.preventDefault(); showPanel(forgotForm, -50); }); }
    if (backToLoginFromForgot) { backToLoginFromForgot.addEventListener("click", (e) => { e.preventDefault(); showPanel(loginForm, 50); }); }


    // --- 3. SUB-ACTION: INTERCEPT INLINE "VERIFY" BUTTON CLICKS ---
    
    // A. Registration Email OTP Request
    if (sendCodeInlineBtn) {
        sendCodeInlineBtn.addEventListener("click", async () => {
            const emailInput = document.getElementById("regEmail");
            const emailValue = emailInput ? emailInput.value.trim() : "";

            if (!emailValue || !emailInput.checkValidity()) {
                displayMessage(registerForm, "Please enter a valid email address first before requesting a code.", false);
                return;
            }

            sendCodeInlineBtn.textContent = "Sending...";
            sendCodeInlineBtn.disabled = true;

            const payload = {
                name: document.getElementById("regName").value,
                email: emailValue,
                password: "TEMPORARY_STUB_HOLD", 
                confirmPassword: "TEMPORARY_STUB_HOLD"
            };

            try {
                const response = await fetch(`${API_URL}/register-request`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });
                const textMessage = await response.text();

                if (response.ok) {
                    displayMessage(registerForm, "6-digit code has been sent! Check your Gmail.", true);
                } else {
                    displayMessage(registerForm, textMessage, false);
                }
            } catch (err) {
                displayMessage(registerForm, "Error: Network failure contacting verification server.", false);
            } finally {
                sendCodeInlineBtn.textContent = "Verify";
                sendCodeInlineBtn.disabled = false;
            }
        });
    }

    // B. Forgot Password Email OTP Request
    if (forgotVerifyBtn) {
        forgotVerifyBtn.addEventListener("click", async () => {
            const emailInput = document.getElementById("forgotEmail");
            const emailValue = emailInput ? emailInput.value.trim() : "";

            if (!emailValue || !emailInput.checkValidity()) {
                displayMessage(forgotForm, "Please enter a valid email address first before requesting a reset code.", false);
                return;
            }

            forgotVerifyBtn.textContent = "Sending...";
            forgotVerifyBtn.disabled = true;

            const payload = {
                email: emailValue
            };

            try {
                const response = await fetch(`${API_URL}/forgot-password-request`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });
                const textMessage = await response.text();

                if (response.ok) {
                    displayMessage(forgotForm, "Password reset code sent! Check your email inbox.", true);
                } else {
                    displayMessage(forgotForm, textMessage, false);
                }
            } catch (err) {
                displayMessage(forgotForm, "Error: Network failure contacting verification server.", false);
            } finally {
                forgotVerifyBtn.textContent = "Verify";
                forgotVerifyBtn.disabled = false;
            }
        });
    }


    // --- 4. FORM TRANSACTION POST ROUTINES ---

    // Form 1: Login Route
    if (loginForm) {
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const payload = {
                email: document.getElementById("emailInput").value,
                password: document.getElementById("password").value
            };

            try {
                const response = await fetch(`${API_URL}/login`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });
                const textMessage = await response.text();
                
                if (response.ok) {
                    clearMessages();
                    window.location.href = "/foodspothome";
                } else {
                    displayMessage(loginForm, textMessage, false);
                }
            } catch (err) {
                displayMessage(loginForm, "Error: Unable to reach the backend server.", false);
            }
        });
    }

    // Form 2: Register Button Submission Route with validations
    if (registerForm) {
        registerForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const pwd = document.getElementById("regPassword").value;
            const cpwd = document.getElementById("regConfirmPassword").value;
            const termsCheckbox = document.getElementById("termsCheckbox");
            
            // A. Password Policy Validation Check
            if (!passwordRegex.test(pwd)) {
                displayMessage(registerForm, "Password must be at least 6 characters with 1 uppercase, 1 lowercase, 1 number, and 1 special character.", false);
                return;
            }

            // B. Confirm Password Check
            if (pwd !== cpwd) {
                displayMessage(registerForm, "Passwords do not match.", false);
                return;
            }

            // C. Required Terms Checkbox Check
            if (termsCheckbox && !termsCheckbox.checked) {
                displayMessage(registerForm, "You must agree to the terms & policy to register.", false);
                return;
            }

            const payload = {
                name: document.getElementById("regName").value,
                email: document.getElementById("regEmail").value,
                verificationCode: document.getElementById("regCode").value,
                password: pwd
            };

            try {
                const response = await fetch(`${API_URL}/verify-code`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });
                const textMessage = await response.text();

                if (response.ok) {
                    clearMessages();
                    window.location.href = "/foodspothome";
                } else {
                    displayMessage(registerForm, textMessage, false);
                }
            } catch (err) {
                displayMessage(registerForm, "Error: Unable to complete data submission tracking.", false);
            }
        });
    }

   // Form 3: Forgot Password Route
    if (forgotForm) {
        forgotForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const newPwd = document.getElementById("forgotNewPassword").value;
            const confirmNewPwd = document.getElementById("forgotConfirmPassword").value;

            if (!passwordRegex.test(newPwd)) {
                displayMessage(forgotForm, "Password must be at least 6 characters with 1 uppercase, 1 lowercase, 1 number, and 1 special character.", false);
                return;
            }

            if (newPwd !== confirmNewPwd) {
                displayMessage(forgotForm, "Passwords do not match.", false);
                return;
            }

            const payload = {
                email: document.getElementById("forgotEmail").value,
                verificationCode: document.getElementById("forgotCode").value,
                newPassword: newPwd,
                confirmPassword: confirmNewPwd
            };

            try {
                const response = await fetch(`${API_URL}/reset-password`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });
                const textMessage = await response.text();
                
                displayMessage(forgotForm, textMessage, response.ok);

                // ---> CLEAR ALL FORM FIELDS ON SUCCESSFUL RESET <---
                if (response.ok) {
                    forgotForm.reset();
                }
            } catch (err) {
                displayMessage(forgotForm, "Error: Unable to reach backend systems.", false);
            }
        });
    }
    // --- 5. COMPACT INTERACTIVE PASSWORD EYE VISIBILITY ENGINE ---
    const togglePasswordIcons = document.querySelectorAll('.password-toggle-icon');

    togglePasswordIcons.forEach(toggle => {
        toggle.addEventListener('click', function () {
            const passwordInput = this.parentElement.querySelector('input');
            
            if (passwordInput.type === 'password') {
                passwordInput.type = 'text';
                this.innerHTML = `
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 19c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                        <line x1="1" y1="1" x2="23" y2="23"></line>
                    </svg>
                `;
            } else {
                passwordInput.type = 'password';
                this.innerHTML = `
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                `;
            }
        });
    });
});